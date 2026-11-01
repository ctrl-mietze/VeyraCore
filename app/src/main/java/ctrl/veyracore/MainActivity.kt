package ctrl.veyracore

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private val vm: VeyraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { VeyraApp(vm) }
    }
}

class VeyraViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalStore(app)
    private val shell = SafeShell()

    val registry: RootProviderRegistry = RootProviderRegistry.default()
    val device: DeviceInfo = DeviceInfo.current()

    var berry by mutableStateOf(store.berry)
        private set
    var probe by mutableStateOf(RootProbe())
        private set
    var checking by mutableStateOf(false)
        private set
    var providerEvaluations by mutableStateOf(registry.evaluateAll(device, probe))
        private set
    var selectedMethodId by mutableStateOf(store.selectedMethodId)
        private set
    var preparationMessage by mutableStateOf<String?>(null)
        private set

    var modules by mutableStateOf<List<RootModule>>(emptyList())
        private set
    var moduleStatus by mutableStateOf("UID 0 noch nicht verifiziert")
        private set
    var moduleInstaller by mutableStateOf(ModuleInstallerAvailability(false, "Noch nicht geprüft"))
        private set
    var moduleZipPreview by mutableStateOf<ModuleZipPreview?>(null)
        private set
    var moduleZipMessage by mutableStateOf<String?>(null)
        private set
    var moduleZipBusy by mutableStateOf(false)
        private set
    private var moduleZipUri: Uri? = null

    var logs by mutableStateOf(store.logs())
        private set
    var exportStatus by mutableStateOf<String?>(null)
        private set

    init {
        log(Severity.INFO, "VeyraCore gestartet", "v0.2.0-dev; Rootstatus wird ausschließlich durch einen aktuellen UID-0-Nachweis bestimmt.")
    }

    fun updateBerry(value: Boolean) {
        if (berry == value) return
        berry = value
        store.berry = value
        log(Severity.INFO, "Design geändert", if (value) "Veyra Berry" else "Veyra OLED")
    }

    fun log(severity: Severity, event: String, detail: String) {
        store.append(AppLog(System.currentTimeMillis(), severity, event, detail))
        logs = store.logs()
    }

    fun clearLogs() {
        store.clearLogs()
        logs = emptyList()
        exportStatus = "Lokales Protokoll geleert"
    }

    fun checkRoot() = viewModelScope.launch {
        if (checking) return@launch
        checking = true
        log(Severity.INFO, "Root-Prüfung", "Begrenzter su -c id Test gestartet")
        val outcome = shell.probe()
        probe = outcome
        providerEvaluations = registry.evaluateAll(device, outcome)
        checking = false
        log(
            if (outcome.isUid0) Severity.SUCCESS else Severity.WARNING,
            "Root-Prüfung beendet",
            "${outcome.uidClass.label}: ${outcome.detail}"
        )
        if (outcome.isUid0) {
            refreshModules()
            checkModuleInstaller()
        } else {
            modules = emptyList()
            moduleStatus = "UID 0 nicht verifiziert; Module bleiben verborgen"
            moduleInstaller = ModuleInstallerAvailability(false, "UID 0 fehlt")
        }
    }

    fun refreshProviderEvaluations() {
        providerEvaluations = registry.evaluateAll(device, probe)
    }

    fun evaluation(methodId: String): ProviderEvaluation? = providerEvaluations.firstOrNull { it.methodId == methodId }

    fun selectMethod(methodId: String) {
        if (registry.provider(methodId) == null) return
        selectedMethodId = methodId
        store.selectedMethodId = methodId
        preparationMessage = "${registry.provider(methodId)?.displayName} ausgewählt. Es wurde kein Lauf gestartet."
        log(Severity.INFO, "Root-Methode ausgewählt", methodId)
    }

    fun prepareMethod(methodId: String) = viewModelScope.launch {
        val provider = registry.provider(methodId) ?: return@launch
        val result = provider.prepare(device)
        preparationMessage = result.detail
        log(Severity.INFO, "Verifikationsplan", "${provider.displayName}: ${result.detail}")
    }

    fun refreshModules() = viewModelScope.launch {
        if (!probe.isUid0) {
            moduleStatus = "UID 0 nicht verifiziert; keine Modulabfrage ausgeführt"
            modules = emptyList()
            return@launch
        }
        moduleStatus = "Module werden über autorisiertes su gelesen …"
        shell.modules().onSuccess { found ->
            modules = found
            moduleStatus = if (found.isEmpty()) "Keine Module in /data/adb/modules gefunden" else "${found.size} Modul(e) geladen"
            log(Severity.INFO, "Module aktualisiert", moduleStatus)
        }.onFailure { error ->
            moduleStatus = error.message ?: "Modulzugriff fehlgeschlagen"
            log(Severity.ERROR, "Module nicht geladen", moduleStatus)
        }
    }

    private fun checkModuleInstaller() = viewModelScope.launch {
        moduleInstaller = shell.moduleInstallerAvailability()
        log(Severity.INFO, "ZIP-Installer geprüft", moduleInstaller.detail)
    }

    fun toggleModule(module: RootModule) = viewModelScope.launch {
        if (!probe.isUid0) return@launch
        shell.setModuleDisabled(module.id, !module.disabled).onSuccess {
            log(Severity.SUCCESS, "Modulstatus geändert", module.id)
            refreshModules()
        }.onFailure { error ->
            log(Severity.ERROR, "Modulstatus unverändert", error.message ?: module.id)
        }
    }

    fun removeModule(module: RootModule) = viewModelScope.launch {
        if (!probe.isUid0) return@launch
        shell.markModuleRemoval(module.id).onSuccess {
            log(Severity.WARNING, "Entfernung für Neustart markiert", module.id)
            refreshModules()
        }.onFailure { error ->
            log(Severity.ERROR, "Entfernung nicht markiert", error.message ?: module.id)
        }
    }

    fun previewModule(uri: Uri) = viewModelScope.launch {
        moduleZipBusy = true
        moduleZipMessage = null
        moduleZipPreview = null
        moduleZipUri = null
        val displayName = uri.lastPathSegment?.substringAfterLast('/').orEmpty().ifBlank { "module.zip" }
        val result: Result<ModuleZipPreview> = withContext(Dispatchers.IO) {
            runCatching {
                val input = getApplication<Application>().contentResolver.openInputStream(uri)
                    ?: error("Ausgewähltes ZIP kann nicht geöffnet werden")
                input.use { ModuleZipVerifier.inspect(it, displayName).getOrThrow() }
            }
        }
        result.onSuccess { preview ->
            moduleZipUri = uri
            moduleZipPreview = preview
            moduleZipMessage = "Sichere Vorschau erstellt; es wurde noch nichts installiert."
            log(Severity.INFO, "Modul-ZIP geprüft", "${preview.moduleId} · ${preview.entryCount} Einträge")
        }.onFailure { error ->
            moduleZipMessage = "ZIP abgelehnt: ${error.message ?: "ungültige Struktur"}"
            log(Severity.WARNING, "Modul-ZIP abgelehnt", moduleZipMessage.orEmpty())
        }
        moduleZipBusy = false
    }

    fun clearModulePreview() {
        moduleZipUri = null
        moduleZipPreview = null
        moduleZipMessage = null
    }

    fun installPreviewModule() = viewModelScope.launch {
        val preview = moduleZipPreview ?: return@launch
        val uri = moduleZipUri ?: return@launch
        if (!probe.isUid0) {
            moduleZipMessage = "Installation blockiert: aktueller UID-0-Nachweis fehlt."
            return@launch
        }
        if (!moduleInstaller.available) {
            moduleZipMessage = "Installation deaktiviert: ${moduleInstaller.detail}"
            return@launch
        }

        moduleZipBusy = true
        moduleZipMessage = "ZIP wird erneut geprüft und an ksud übergeben …"
        val cacheFile = withContext(Dispatchers.IO) {
            File.createTempFile("veyra-module-", ".zip", getApplication<Application>().cacheDir)
        }
        try {
            withContext(Dispatchers.IO) { copyUriBounded(uri, cacheFile) }
            val verifiedAgain = withContext(Dispatchers.IO) {
                FileInputStream(cacheFile).use { ModuleZipVerifier.inspect(it, preview.displayName).getOrThrow() }
            }
            require(verifiedAgain.moduleId == preview.moduleId) { "ZIP-Inhalt hat sich seit der Vorschau geändert" }
            val command = shell.installModuleZip(cacheFile)
            if (command.exitCode != 0) {
                moduleZipMessage = "ksud meldete keinen Erfolg: ${command.error ?: command.output.ifBlank { "Exit ${command.exitCode}" }}"
                log(Severity.ERROR, "Modulinstallation fehlgeschlagen", moduleZipMessage.orEmpty())
            } else {
                val verification = shell.modules()
                val installed = verification.getOrNull()?.any { it.id == preview.moduleId } == true
                if (installed) {
                    moduleZipMessage = "${preview.moduleName} installiert und in /data/adb/modules verifiziert. Änderungen werden je nach Modul beim nächsten Neustart aktiv."
                    log(Severity.SUCCESS, "Modulinstallation verifiziert", preview.moduleId)
                    refreshModules()
                } else {
                    moduleZipMessage = "ksud wurde beendet, aber ${preview.moduleId} ist nicht in der Modulauflistung. Erfolg wird nicht behauptet."
                    log(Severity.ERROR, "Modulinstallation nicht verifiziert", preview.moduleId)
                }
            }
        } catch (error: Exception) {
            moduleZipMessage = "Installation nicht ausgeführt: ${error.message ?: error.javaClass.simpleName}"
            log(Severity.ERROR, "Modulinstallation abgebrochen", moduleZipMessage.orEmpty())
        } finally {
            withContext(Dispatchers.IO) { cacheFile.delete() }
            moduleZipBusy = false
        }
    }

    private suspend fun copyUriBounded(uri: Uri, target: File) {
        val resolver = getApplication<Application>().contentResolver
        val input = resolver.openInputStream(uri) ?: error("ZIP kann nicht erneut geöffnet werden")
        input.use { source ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(8192)
                var total = 0L
                while (true) {
                    val count = source.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= ModuleZipVerifier.MAX_ARCHIVE_BYTES) { "ZIP überschreitet 64 MiB" }
                    output.write(buffer, 0, count)
                }
            }
        }
    }

    fun diagnosticsText(): String = buildString {
        appendLine("VeyraCore 0.2.0-dev")
        appendLine("Application ID: ctrl.veyracore")
        appendLine("Device: ${device.manufacturer} ${device.model}")
        appendLine("Android: ${device.android}")
        appendLine("Firmware: ${device.firmwareId}")
        appendLine("Fingerprint: ${device.fingerprint}")
        appendLine("Kernel: ${device.kernel}")
        appendLine("KMI: ${device.kmi}")
        appendLine("ABI: ${device.abi}")
        appendLine("Page size: ${device.pageSize ?: "unknown"}")
        appendLine("UID proof: ${probe.uidClass.label}; uid=${probe.uid ?: "unknown"}")
        appendLine("Identity: ${probe.identity ?: "not checked"}")
        appendLine("SELinux: ${probe.selinux ?: "not checked"}")
        appendLine("Veyra Load runtime: ${probe.runtime} / ${probe.runtimeDetail}")
    }

    fun exportLogs() = viewModelScope.launch {
        val snapshot = logs.sortedBy { it.epochMs }
        val uri = withContext(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver
            var target: Uri? = null
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "VeyraCore-diagnostics-${System.currentTimeMillis()}.txt")
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                target = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: error("Downloads-Ziel konnte nicht erstellt werden")
                resolver.openOutputStream(target!!)?.bufferedWriter()?.use { writer ->
                    snapshot.forEach { entry ->
                        writer.appendLine("${Date(entry.epochMs)} [${entry.severity}] ${entry.event}: ${entry.detail}")
                    }
                } ?: error("Downloads-Datei konnte nicht geschrieben werden")
                val ready = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                resolver.update(target!!, ready, null, null)
                target
            } catch (error: Exception) {
                target?.let { resolver.delete(it, null, null) }
                null
            }
        }
        exportStatus = uri?.let { "Exportiert nach MediaStore Downloads: $it" } ?: "Export fehlgeschlagen"
        log(if (uri != null) Severity.SUCCESS else Severity.ERROR, "Diagnose-Export", exportStatus.orEmpty())
    }
}

private val Oled = darkColorScheme(
    primary = Color(0xFFB777FF),
    onPrimary = Color(0xFF210039),
    primaryContainer = Color(0xFF3B145C),
    onPrimaryContainer = Color(0xFFF0DFFF),
    secondary = Color(0xFFD5B5FF),
    onSecondary = Color(0xFF2C1247),
    secondaryContainer = Color(0xFF3B2A4D),
    background = Color.Black,
    surface = Color(0xFF0C0A10),
    surfaceVariant = Color(0xFF18131F),
    onBackground = Color(0xFFF8F5FB),
    onSurface = Color(0xFFF8F5FB),
    onSurfaceVariant = Color(0xFFC9BDCE),
    outline = Color(0xFF4A3C54),
    error = Color(0xFFFFB3B9)
)

private val Berry = darkColorScheme(
    primary = Color(0xFFFF6F9A),
    onPrimary = Color(0xFF3D0018),
    primaryContainer = Color(0xFF6D173C),
    onPrimaryContainer = Color(0xFFFFD9E3),
    secondary = Color(0xFFC9A4FF),
    onSecondary = Color(0xFF2D1552),
    secondaryContainer = Color(0xFF432A60),
    background = Color(0xFF100B13),
    surface = Color(0xFF1A131E),
    surfaceVariant = Color(0xFF2A1C2D),
    onBackground = Color(0xFFFFF7FB),
    onSurface = Color(0xFFFFF7FB),
    onSurfaceVariant = Color(0xFFD9C3D3),
    outline = Color(0xFF594152),
    error = Color(0xFFFFB3B9)
)

private val VeyraTypography = Typography(
    displaySmall = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp),
    headlineMedium = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    titleLarge = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 21.sp),
    titleMedium = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
)

private enum class Tab(val id: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("home", "Home", Icons.Rounded.Home),
    ROOT("root", "Root", Icons.Rounded.Security),
    MODULES("modules", "Modules", Icons.Rounded.Extension),
    SUPERUSER("superuser", "Superuser", Icons.Rounded.AdminPanelSettings),
    SETTINGS("settings", "Settings", Icons.Rounded.Settings),
    LOGS("logs", "Logs", Icons.Rounded.Article)
}

@Composable
private fun VeyraApp(vm: VeyraViewModel) {
    val colors = if (vm.berry) Berry else Oled
    val activity = LocalContext.current as? ComponentActivity
    SideEffect {
        activity?.window?.let { window ->
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(colorScheme = colors, typography = VeyraTypography) {
        var tabId by rememberSaveable { mutableStateOf(Tab.HOME.id) }
        val tabs = buildList {
            add(Tab.HOME)
            add(Tab.ROOT)
            if (vm.probe.isUid0) {
                add(Tab.MODULES)
                add(Tab.SUPERUSER)
            }
            add(Tab.SETTINGS)
            add(Tab.LOGS)
        }
        if (tabs.none { it.id == tabId }) tabId = Tab.HOME.id
        val selected = tabs.first { it.id == tabId }
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().background(MaterialTheme.colorScheme.background)) {
            val wide = maxWidth >= 600.dp
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    AdaptiveNavigation(tabs, selected, onSelect = { tabId = it.id }, wide = true)
                    Box(Modifier.weight(1f).fillMaxHeight()) { SelectedPage(selected, vm, onNavigate = { tabId = it }) }
                }
            } else {
                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = { AdaptiveNavigation(tabs, selected, onSelect = { tabId = it.id }, wide = false) }
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) { SelectedPage(selected, vm, onNavigate = { tabId = it }) }
                }
            }
        }
    }
}

@Composable
private fun AdaptiveNavigation(tabs: List<Tab>, selected: Tab, onSelect: (Tab) -> Unit, wide: Boolean) {
    if (wide) {
        NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
            Spacer(Modifier.height(12.dp))
            tabs.forEach { tab ->
                NavigationRailItem(
                    selected = selected == tab,
                    onClick = { onSelect(tab) },
                    icon = { Icon(tab.icon, tab.label) },
                    label = { Text(tab.label, maxLines = 1, softWrap = false) },
                    alwaysShowLabel = true
                )
            }
        }
    } else {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            tabs.forEach { tab ->
                NavigationBarItem(
                    modifier = Modifier.weight(1f),
                    selected = selected == tab,
                    onClick = { onSelect(tab) },
                    icon = { Icon(tab.icon, tab.label, Modifier.size(20.dp)) },
                    label = {
                        Text(
                            tab.label,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 10.sp
                        )
                    },
                    alwaysShowLabel = true
                )
            }
        }
    }
}

@Composable
private fun SelectedPage(tab: Tab, vm: VeyraViewModel, onNavigate: (String) -> Unit) {
    AnimatedContent(
        targetState = tab,
        transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(100)) },
        label = "page-transition"
    ) { current ->
        when (current) {
            Tab.HOME -> Home(vm, onNavigate)
            Tab.ROOT -> Root(vm)
            Tab.MODULES -> Modules(vm)
            Tab.SUPERUSER -> Superuser(vm)
            Tab.SETTINGS -> Settings(vm)
            Tab.LOGS -> Logs(vm)
        }
    }
}

@Composable
private fun Page(title: String, subtitle: String? = null, content: LazyListScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.align(Alignment.TopCenter).widthIn(max = 860.dp).fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(title, style = MaterialTheme.typography.headlineMedium)
                subtitle?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
            }
            content()
        }
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(11.dp), content = content)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Meta(label: String, value: String, mono: Boolean = false) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
        Text(value.ifBlank { "Unbekannt" }, fontFamily = if (mono) FontFamily.Monospace else FontFamily.SansSerif)
    }
}

@Composable
private fun StatusBadge(label: String, color: Color) {
    Surface(color = color.copy(alpha = .16f), contentColor = color, shape = RoundedCornerShape(50)) {
        Text(label, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun RootStatusBadge(probe: RootProbe) {
    val color = when {
        probe.isUid0 -> Color(0xFF85E0B0)
        probe.state == Verification.ERROR -> MaterialTheme.colorScheme.error
        probe.state == Verification.NOT_VERIFIED -> Color(0xFFF4C76D)
        else -> MaterialTheme.colorScheme.secondary
    }
    val label = when {
        probe.isUid0 -> "UID 0 VERIFIZIERT"
        probe.state == Verification.NOT_VERIFIED -> "NICHT VERIFIZIERT"
        probe.state == Verification.ERROR -> "PRÜFUNG FEHLERHAFT"
        else -> "NOCH NICHT GEPRÜFT"
    }
    StatusBadge(label, color)
}

@Composable
private fun Home(vm: VeyraViewModel, onNavigate: (String) -> Unit) = Page(
    title = "VeyraCore",
    subtitle = "v0.2.0-dev · unabhängige, ehrliche Runtime-Grundlage"
) {
    item {
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Shield, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(27.dp))
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Aktueller Root-Nachweis", style = MaterialTheme.typography.titleMedium)
                    RootStatusBadge(vm.probe)
                }
            }
            Text(vm.probe.uidClass.label, fontWeight = FontWeight.SemiBold)
            Text(
                vm.probe.detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = vm::checkRoot, enabled = !vm.checking, modifier = Modifier.fillMaxWidth()) {
                if (vm.checking) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Rounded.Fingerprint, null)
                Spacer(Modifier.width(8.dp))
                Text(if (vm.checking) "Prüfung läuft …" else "UID-0-Nachweis prüfen")
            }
            Text(
                "Nur ein aktuelles `su -c id` mit exit code 0 und führendem uid=0 zählt. Pfade, Manager-Apps, Zertifikate oder ADB-UIDs zählen nicht.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    item {
        SectionTitle("Schnellzugriff")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            QuickAction("Root", Icons.Rounded.Security, { onNavigate(Tab.ROOT.id) }, Modifier.weight(1f))
            QuickAction("Logs", Icons.Rounded.Article, { onNavigate(Tab.LOGS.id) }, Modifier.weight(1f))
            QuickAction("Settings", Icons.Rounded.Settings, { onNavigate(Tab.SETTINGS.id) }, Modifier.weight(1f))
        }
    }
    item {
        SectionTitle("Lokales Geräteprofil")
        DeviceProfileCard(vm.device)
    }
    item {
        SectionTitle("Beobachtete Runtime")
        Panel {
            EvidenceRow("UID", vm.probe.uid?.toString() ?: "nicht geprüft", vm.probe.isUid0)
            EvidenceRow("Identität", vm.probe.identity ?: "nicht geprüft", vm.probe.isUid0)
            EvidenceRow("SELinux", vm.probe.selinux ?: "nicht geprüft", false)
            EvidenceRow("Veyra Load", vm.probe.runtimeDetail, vm.probe.runtime == RuntimeStatus.CONNECTED)
        }
    }
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier) {
    FilledTonalButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 11.dp)) {
        Icon(icon, null, Modifier.size(18.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DeviceProfileCard(device: DeviceInfo) {
    Panel {
        Meta("Hersteller / Modell", "${device.manufacturer} · ${device.model}")
        Meta("Android / Security Patch", "${device.android} · ${device.securityPatch}")
        Meta("Firmware ID", device.firmwareId, mono = true)
        Meta("Build-Fingerprint", device.fingerprint, mono = true)
        Meta("Kernel / KMI", "${device.kernel} · ${device.kmi}", mono = true)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) { Meta("ABI", device.abi, mono = true) }
            Column(Modifier.weight(1f)) { Meta("Seitengröße", device.pageSize?.let { "$it Bytes" } ?: "unknown", mono = true) }
        }
    }
}

@Composable
private fun EvidenceRow(label: String, value: String, verified: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(
            if (verified) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            null,
            tint = if (verified) Color(0xFF85E0B0) else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Root(vm: VeyraViewModel) = Page(
    title = "Root",
    subtitle = "Methoden, Nachweise und exakte Port-Grenzen auf diesem Gerät"
) {
    item {
        Panel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Live-Nachweis", style = MaterialTheme.typography.titleMedium)
                    Text(vm.probe.uidClass.label, fontWeight = FontWeight.SemiBold)
                }
                RootStatusBadge(vm.probe)
            }
            EvidenceRow("su -c id", vm.probe.identity ?: "Noch nicht ausgeführt", vm.probe.isUid0)
            EvidenceRow("SELinux", vm.probe.selinux ?: "Noch nicht geprüft", false)
            EvidenceRow(
                "Runtime socket",
                if (vm.probe.runtime == RuntimeStatus.CONNECTED) "connected · /dev/socket/veyra_loadd" else vm.probe.runtimeDetail,
                vm.probe.runtime == RuntimeStatus.CONNECTED
            )
            Text(
                "UID 1000 (system) und UID 2000 (adb shell) sind keine UID-0-Berechtigung. Diese App blendet privilegierte Bereiche erst nach dem aktuellen UID-0-Nachweis ein.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    item { SectionTitle("Lokales Geräteprofil") }
    item { DeviceProfileCard(vm.device) }
    item { SectionTitle("Root-Methoden") }
    item { vm.evaluation("ghostlock")?.let { ProviderCard(it, vm) } }
    item { vm.evaluation("dirtyfrag")?.let { ProviderCard(it, vm) } }
    item { vm.evaluation("vx-cert-reroot")?.let { ProviderCard(it, vm) } }
    item {
        Panel {
            Text("Registry-Regel", style = MaterialTheme.typography.titleMedium)
            Text("Ein Provider darf erst unterstützt heißen, wenn ein vollständiges Profil aus Firmware, Build-Fingerprint, Kernel/KMI, ABI und Seitengröße exakt übereinstimmt und der Lauf auf dem Zielgerät reproduzierbar verifiziert ist.")
            Text("Die Veyra-Registry enthält in 0.2.0-dev keine Exploitprofile. Auswahl und Vorbereitung speichern nur eine sichere Prüfabsicht; es werden keine unbekannten Dateien geladen.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
    item { SectionTitle("Offline-Portkatalog") }
    item {
        Text("Quellen werden als Forschung referenziert, nicht als Veyra-Kompatibilitätsversprechen. Der Katalog ist statisch und benötigt keine Netzverbindung.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
    items(PortResearchCatalog.entries, key = { it.name }) { entry -> PortResearchCard(entry) }
}

@Composable
private fun ProviderCard(evaluation: ProviderEvaluation, vm: VeyraViewModel) {
    val context = LocalContext.current
    val selected = vm.selectedMethodId == evaluation.methodId
    val statusColor = when (evaluation.status) {
        ProviderStatus.SUPPORTED -> Color(0xFF85E0B0)
        ProviderStatus.BLOCKED -> Color(0xFFF4C76D)
        ProviderStatus.REQUIRES_EXTERNAL_RUNTIME -> MaterialTheme.colorScheme.secondary
        ProviderStatus.UNAVAILABLE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(evaluation.displayName, style = MaterialTheme.typography.titleMedium)
                Text("ID: ${evaluation.methodId}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            }
            StatusBadge(evaluation.status.label, statusColor)
        }
        Text(evaluation.detail)
        Text("Provenienz: ${evaluation.provenance.sourceName}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(evaluation.provenance.basis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (selected) {
                FilledTonalButton(onClick = {}, enabled = false, modifier = Modifier.weight(1f)) { Text("Ausgewählt") }
            } else {
                OutlinedButton(onClick = { vm.selectMethod(evaluation.methodId) }, modifier = Modifier.weight(1f)) { Text("Für Prüfung auswählen") }
            }
            TextButton(onClick = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(evaluation.provenance.sourceUrl))) }
            }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.OpenInNew, null, Modifier.size(17.dp))
                Spacer(Modifier.width(4.dp))
                Text("Quelle", maxLines = 1)
            }
        }
        if (selected) {
            OutlinedButton(onClick = { vm.prepareMethod(evaluation.methodId) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Rule, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Nur Verifikationsplan vorbereiten")
            }
        }
        vm.preparationMessage?.takeIf { selected }?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun PortResearchCard(entry: PortResearchEntry) {
    val context = LocalContext.current
    Panel {
        Text(entry.name, style = MaterialTheme.typography.titleMedium)
        Text(entry.documentedScope)
        Text(entry.veyraStatus, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.repositoryUrl))) }
        }, contentPadding = PaddingValues(0.dp)) {
            Icon(Icons.Rounded.Link, null, Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(entry.repositoryUrl, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Modules(vm: VeyraViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::previewModule) }
    var confirmInstall by rememberSaveable { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf<RootModule?>(null) }
    Page("Modules", "Nur nach aktuellem UID-0-Nachweis; Marker-Änderungen bleiben gezielt und reversibel") {
        item {
            Panel {
                Text(vm.moduleStatus, style = MaterialTheme.typography.titleMedium)
                Text("Liest /data/adb/modules über den autorisierten su-Backend. IDs werden vor jeder Pfadoperation validiert; module.prop wird nur als Text gelesen.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = vm::refreshModules, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Refresh, null)
                    Spacer(Modifier.width(7.dp))
                    Text("Module aktualisieren")
                }
            }
        }
        item {
            Panel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.UploadFile, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(9.dp))
                    Text("ZIP-Modul importieren", style = MaterialTheme.typography.titleMedium)
                }
                Text("SAF-Vorschau prüft module.prop, sichere relative Pfade, Größenlimits, Duplikate und Unix-Symlink-Metadaten. Es wird nichts extrahiert oder ausgeführt.")
                Text(vm.moduleInstaller.detail, color = if (vm.moduleInstaller.available) Color(0xFF85E0B0) else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Button(onClick = { picker.launch(arrayOf("application/zip", "application/octet-stream")) }, enabled = !vm.moduleZipBusy, modifier = Modifier.fillMaxWidth()) {
                    if (vm.moduleZipBusy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Rounded.Archive, null)
                    Spacer(Modifier.width(7.dp))
                    Text(if (vm.moduleZipBusy) "ZIP wird geprüft …" else "ZIP auswählen und vorschauen")
                }
                vm.moduleZipPreview?.let { preview ->
                    HorizontalDivider()
                    Text(preview.moduleName, style = MaterialTheme.typography.titleMedium)
                    Text("${preview.moduleId} · ${preview.moduleVersion} · ${preview.archiveBytes / 1024} KiB · ${preview.entryCount} Einträge", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                    if (preview.moduleAuthor.isNotBlank()) Text("von ${preview.moduleAuthor}", style = MaterialTheme.typography.bodySmall)
                    if (preview.moduleDescription.isNotBlank()) Text(preview.moduleDescription, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text("Installieren bleibt ausdrücklich bestätigt und benötigt eine verifizierte ksud module install-Schnittstelle. Nach Installation wird die Modulauflistung erneut geprüft; Aktivierung kann einen Neustart benötigen.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = vm::clearModulePreview, modifier = Modifier.weight(1f), enabled = !vm.moduleZipBusy) { Text("Verwerfen") }
                        Button(onClick = { confirmInstall = true }, modifier = Modifier.weight(1f), enabled = vm.moduleInstaller.available && !vm.moduleZipBusy) { Text("Installieren") }
                    }
                }
                vm.moduleZipMessage?.let { Text(it, color = if (it.startsWith("ZIP abgelehnt") || it.contains("fehlgeschlagen") || it.contains("abgebrochen")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            }
        }
        if (vm.modules.isEmpty()) {
            item { EmptyCard("Keine geladenen Module", "Nach einer Aktualisierung werden vorhandene /data/adb/modules angezeigt.") }
        }
        items(vm.modules, key = { it.id }) { module -> ModuleCard(module, vm, onRemove = { confirmRemove = it }) }
    }
    if (confirmInstall && vm.moduleZipPreview != null) {
        AlertDialog(
            onDismissRequest = { confirmInstall = false },
            title = { Text("ZIP-Modul installieren?") },
            text = { Text("${vm.moduleZipPreview?.moduleName} wird über die zuvor verifizierte ksud module install-Schnittstelle installiert. VeyraCore löscht keine bestehenden Module. Je nach Modul ist ein Neustart nötig.") },
            confirmButton = { TextButton(onClick = { confirmInstall = false; vm.installPreviewModule() }) { Text("Bestätigen") } },
            dismissButton = { TextButton(onClick = { confirmInstall = false }) { Text("Abbrechen") } }
        )
    }
    confirmRemove?.let { module ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text("Entfernung markieren?") },
            text = { Text("${module.name} wird nicht sofort gelöscht. Es wird nur die sichere remove-Markierung für den nächsten Boot gesetzt.") },
            confirmButton = { TextButton(onClick = { confirmRemove = null; vm.removeModule(module) }) { Text("Für Neustart markieren") } },
            dismissButton = { TextButton(onClick = { confirmRemove = null }) { Text("Abbrechen") } }
        )
    }
}

@Composable
private fun ModuleCard(module: RootModule, vm: VeyraViewModel, onRemove: (RootModule) -> Unit) {
    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(module.name, style = MaterialTheme.typography.titleMedium)
                Text("${module.id} · ${module.version}", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
            }
            if (module.disabled) StatusBadge("deaktiviert", Color(0xFFF4C76D))
            else StatusBadge("aktiv", Color(0xFF85E0B0))
        }
        if (module.author.isNotBlank()) Text("von ${module.author}", style = MaterialTheme.typography.bodySmall)
        if (module.description.isNotBlank()) Text(module.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(onClick = { vm.toggleModule(module) }, modifier = Modifier.weight(1f)) { Text(if (module.disabled) "Aktivieren" else "Deaktivieren") }
            OutlinedButton(onClick = { onRemove(module) }, modifier = Modifier.weight(1f), enabled = !module.markedForRemoval) { Text(if (module.markedForRemoval) "Entfernung markiert" else "Entfernen") }
        }
        if (module.markedForRemoval) Text("remove-Markierung ist für den nächsten Neustart gesetzt.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Superuser(vm: VeyraViewModel) = Page("Superuser", "Read-only Nachweise · keine Policy- oder Grant-Manipulation") {
    item {
        Panel {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("App-Root-Proof", style = MaterialTheme.typography.titleMedium)
                    Text(vm.probe.uidClass.label, fontWeight = FontWeight.SemiBold)
                }
                RootStatusBadge(vm.probe)
            }
            Meta("Aktuelle id-Antwort", vm.probe.identity ?: "nicht geprüft", mono = true)
            Text("VeyraCore zeigt diese Seite nur nach einem echten UID-0-Nachweis. Sie vergibt, widerruft oder fälscht keine Superuser-Rechte.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
    item {
        Panel {
            Text("UID-Unterscheidung", style = MaterialTheme.typography.titleMedium)
            UidExplanation("UID 0", "root", "Privilegierte Identität. Für VeyraCore nur gültig, wenn su -c id exakt uid=0 und exit code 0 liefert.", vm.probe.uid == 0)
            UidExplanation("UID 1000", "system", "Android-Systemdienst; kein App-Root und kein Beweis für einen autorisierten su-Zugang.", vm.probe.uid == 1000)
            UidExplanation("UID 2000", "adb shell", "ADB-Shell-Identität; nützlich für Diagnose, aber nicht UID 0.", vm.probe.uid == 2000)
        }
    }
    item {
        Panel {
            Text("Veyra Load", style = MaterialTheme.typography.titleMedium)
            EvidenceRow("Socket-Probe", vm.probe.runtimeDetail, vm.probe.runtime == RuntimeStatus.CONNECTED)
            Text("Es gibt in 0.2.0-dev keinen first-party Veyra-Load-Policy-Backend. Allow/Deny-Listen und Root-Grants bleiben deshalb unveränderbar und werden nicht aus Settings-Spoofing abgeleitet.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun UidExplanation(uid: String, name: String, detail: String, current: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Icon(if (current) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked, null, tint = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text("$uid · $name", fontWeight = FontWeight.SemiBold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Settings(vm: VeyraViewModel) {
    val context = LocalContext.current
    Page("Settings", "Persistente Darstellung und transparente Grenzen") {
        item {
            Panel {
                Text("Veyra OLED", style = MaterialTheme.typography.titleMedium)
                Text("True black, lilac/violet accents, klare Oberflächen ohne Glas- oder Gradient-Look.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                ThemeChoice("Veyra OLED", "#000000 · Lilac #B777FF", !vm.berry) { vm.updateBerry(false) }
                ThemeChoice("Veyra Berry", "Rose #FF6F9A · Violet #C9A4FF · Charcoal", vm.berry) { vm.updateBerry(true) }
            }
        }
        item {
            Panel {
                Text("Verifikation", style = MaterialTheme.typography.titleMedium)
                Text("Root wird nur manuell durch den sichtbaren su -c id Test geprüft. Es gibt keine stille Anfrage beim App-Start und keine Manager-App-Erkennung.")
                Text("Providerstatus: ${vm.providerEvaluations.count { it.status == ProviderStatus.SUPPORTED }} unterstützt, ${vm.providerEvaluations.count { it.status == ProviderStatus.BLOCKED }} gesperrt, ${vm.providerEvaluations.count { it.status == ProviderStatus.REQUIRES_EXTERNAL_RUNTIME }} externe Runtime nötig.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            Panel {
                Text("Runtime & Credits", style = MaterialTheme.typography.titleMedium)
                Text("VeyraCore 0.2.0-dev · eigener UI-/Runtime-Entwurf. Keine KernelSU-, DirtyFrag-, GhostLock- oder fremden Helper-Quellen/Binaries wurden importiert.")
                OutlinedButton(onClick = {
                    val manager = context.getSystemService(ClipboardManager::class.java)
                    manager.setPrimaryClip(ClipData.newPlainText("VeyraCore diagnostics", vm.diagnosticsText()))
                    vm.log(Severity.INFO, "Diagnose kopiert", "Zwischenablage")
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.ContentCopy, null)
                    Spacer(Modifier.width(7.dp))
                    Text("Diagnose kopieren")
                }
            }
        }
    }
}

@Composable
private fun ThemeChoice(title: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = .45f))
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun Logs(vm: VeyraViewModel) {
    var confirmClear by remember { mutableStateOf(false) }
    Page("Logs", "Lokales Protokoll · Export ohne breite Speicherberechtigung") {
        item {
            Panel {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = vm::exportLogs, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.FileDownload, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Downloads")
                    }
                    OutlinedButton(onClick = { confirmClear = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.DeleteOutline, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Leeren")
                    }
                }
                vm.exportStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text("Maximal 200 Einträge werden app-intern gehalten. Der Export schreibt nach MediaStore.Downloads und löscht keine anderen Dateien.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (vm.logs.isEmpty()) item { EmptyCard("Noch keine Logeinträge", "Prüfungen und sichere Aktionen erscheinen hier.") }
        items(vm.logs, key = { "${it.epochMs}-${it.event}" }) { entry ->
            Panel {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Text(entry.event, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(entry.severity.name, color = if (entry.severity == Severity.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
                }
                Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(entry.epochMs)), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(entry.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Lokale Logs leeren?") },
            text = { Text("Nur das app-interne VeyraCore-Protokoll wird entfernt. Downloads-Exporte bleiben bestehen.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; vm.clearLogs() }) { Text("Leeren") } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Abbrechen") } }
        )
    }
}

@Composable
private fun EmptyCard(title: String, detail: String) {
    Panel {
        Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(26.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
