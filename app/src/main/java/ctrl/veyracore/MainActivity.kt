package ctrl.veyracore

import android.app.Application
import android.content.Context
import android.net.Uri
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalUriHandler
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
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private val vm: VeyraViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { VeyraApp(vm) }
    }

    override fun onResume() {
        super.onResume()
        vm.onAppResumed()
        vm.maybeStartNotification(this)
    }
}

class VeyraViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalStore(app)
    private val shell = SafeShell()
    private val veyraLoad = VeyraLoadRuntime(shell)
    var load by mutableStateOf(VeyraLoadSnapshot()); private set
    var loadBusy by mutableStateOf(false); private set
    private val moduleImporter = ModuleImporter(app, shell)
    private val helperInstaller = HelperPackageInstaller(app)
    var stagedHelper by mutableStateOf<StagedHelperApk?>(null); private set
    var helperInstallStatus by mutableStateOf("Helper-Update noch nicht gewählt"); private set
    var stagedModule by mutableStateOf<StagedModule?>(null); private set
    var moduleImportStatus by mutableStateOf("Module nur aus vertrauenswürdigen Quellen installieren"); private set
    var berry by mutableStateOf(store.berry); private set
    var probe by mutableStateOf(RootProbe()); private set
    var checking by mutableStateOf(false); private set
    var modules by mutableStateOf<List<RootModule>>(emptyList()); private set
    var moduleStatus by mutableStateOf("Noch nicht geladen"); private set
    var providerStatus by mutableStateOf<String?>(null); private set
    var activityRevision by mutableIntStateOf(0); private set
    private var pendingExternalRootCheck = false
    var helper by mutableStateOf(HelperSnapshot()); private set
    var notificationEnabled by mutableStateOf(HelperNotificationControl.enabled(app)); private set
    var notificationActive by mutableStateOf(false); private set
    var automaticSelection by mutableStateOf(store.automaticSelection); private set
    var deviceVerification by mutableStateOf(store.deviceVerification); private set
    var selectedPort by mutableStateOf(store.selectedPort); private set
    var discoveredProfiles by mutableStateOf<List<PayloadProfile>>(emptyList()); private set
    var matchedProfiles by mutableStateOf<List<PayloadProfile>>(emptyList()); private set
    var feedStatus by mutableStateOf("Payload-Feeds noch nicht abgefragt"); private set
    var feedLoading by mutableStateOf(false); private set
    var logs by mutableStateOf(store.logs()); private set
    var exportStatus by mutableStateOf<String?>(null); private set
    val device = DeviceInfo.current()

    init {
        log(Severity.INFO, "VeyraCore gestartet", "v0.4.0-dev; Root UID 0 und Helper UID 1000 getrennt")
        refreshHelper()
        refreshPayloadFeeds()
    }
    fun updatePalette(value: Boolean) { berry = value; store.berry = value; log(Severity.INFO, "Design geändert", if (value) "Veyra Berry" else "Veyra OLED") }
    @JvmName("applyAutoSelection") fun setAutomaticSelection(value: Boolean) {
        automaticSelection = value; store.automaticSelection = value
        log(Severity.INFO, "Root-Auswahl", if (value) "Automatisch" else "Manuell")
    }
    @JvmName("applyDeviceVerification") fun setDeviceVerification(value: Boolean) {
        deviceVerification = value; store.deviceVerification = value
        log(Severity.WARNING, "Geräteprüfung", if (value) "Aktiv" else "Manuelle Auswahl ohne Vorauswahl-Prüfung; keine Payload wird ungeprüft ausgeführt")
    }
    fun selectPort(id: String) {
        if (RootPortRegistry.all.none { it.id == id }) return
        selectedPort = id; store.selectedPort = id
        automaticSelection = false; store.automaticSelection = false
        providerStatus = "Manuelle Quelle: $id · Quellenwahl ist keine aktive Exploit-Engine."
        log(Severity.INFO, "Quelle ausgewählt", id)
    }
    fun refreshPayloadFeeds() = viewModelScope.launch {
        feedLoading = true
        val result = PayloadFeeds(getApplication()).refresh()
        discoveredProfiles = result.profiles
        matchedProfiles = PayloadFeeds.matching(result.profiles, device)
        feedStatus = result.message + " · ${matchedProfiles.size} Modell-/Kernel-Treffer"
        feedLoading = false
        log(Severity.INFO, "Support-Feeds aktualisiert", feedStatus)
    }
    fun refreshHelper() {
        val context = getApplication<Application>()
        val result = HelperInspector.check(context)
        // Snapshot timestamps change on every read; only log real package identity changes.
        if (result.packageName != helper.packageName ||
            result.uid != helper.uid ||
            result.signerSha256 != helper.signerSha256) {
            log(if (result.acceptedSystemUid) Severity.SUCCESS else Severity.INFO,
                "Helper-Status", result.detail)
        }
        helper = result
        notificationActive = HelperNotificationControl.isShown(context)
    }
    /** Called only after the Activity is visibly resumed, not from ViewModel.init. */
    fun maybeStartNotification(context: Context) {
        if (helper.acceptedSystemUid && notificationEnabled && HelperNotificationControl.hasPermission(context)) {
            HelperNotificationControl.sync(context)
        }
        notificationActive = HelperNotificationControl.isShown(context)
    }
    fun refreshVeyraLoad() = viewModelScope.launch {
        loadBusy = true
        val outcome = veyraLoad.inspect()
        load = outcome
        loadBusy = false
        log(if (outcome.authorized) Severity.SUCCESS else Severity.INFO, "Veyra Load Runtime", outcome.status)
    }
    fun setNotifications(value: Boolean) {
        notificationEnabled = value
        val context = getApplication<Application>()
        HelperNotificationControl.updatePreference(context, value)
        if (value) HelperNotificationControl.sync(context)
        else context.stopService(android.content.Intent(context, VeyraHelperNotificationService::class.java))
        notificationActive = HelperNotificationControl.isShown(context)
        log(Severity.INFO, "Helper-Benachrichtigung", if (value) "Aktivierung angefragt" else "Deaktiviert")
    }
    fun afterNotificationPermission(allowed: Boolean) {
        if (allowed) setNotifications(true)
        else { providerStatus = "Benachrichtigungsberechtigung nicht erteilt."; log(Severity.WARNING, "Benachrichtigung", providerStatus!!) }
    }
    fun stageHelperApk(uri: Uri) = viewModelScope.launch {
        stagedHelper = null
        helperInstallStatus = "APK wird kontrolliert ..."
        helperInstaller.stage(uri)
            .onSuccess {
                stagedHelper = it
                helperInstallStatus = "APK geprüft: ${it.packageName} · ${it.versionName} · signiert"
                log(Severity.SUCCESS, "Helper-Update vorbereitet", helperInstallStatus)
            }
            .onFailure {
                helperInstallStatus = it.message ?: "Helper-APK abgelehnt"
                log(Severity.ERROR, "Helper-APK nicht akzeptiert", helperInstallStatus)
            }
    }
    fun discardHelperApk() {
        stagedHelper?.file?.delete()
        stagedHelper = null
        helperInstallStatus = "Helper-Update abgebrochen"
    }
    fun requestHelperUpdate(context: Context) {
        val staged = stagedHelper ?: return
        helperInstaller.requestInstall(context, staged)
            .onSuccess {
                helperInstallStatus = "Android-Paketinstaller geöffnet – Erfolg erst nach PackageManager-Prüfung."
                pendingExternalRootCheck = true
                log(Severity.INFO, "Helper-Update angefordert", helperInstallStatus)
            }
            .onFailure {
                helperInstallStatus = "Paketinstaller: ${it.message}"
                log(Severity.ERROR, "Helper-Update nicht gestartet", helperInstallStatus)
            }
        stagedHelper = null // Keep the staged file for the external installer until cache cleanup.
    }
    fun setupViaExistingProvider(context: Context) {
        if (helper.acceptedSystemUid) {
            providerStatus = "System-UID 1000 wurde vom PackageManager bestätigt."
            return
        }
        val provider = InstalledRootTools.known.firstOrNull {
            (it.id == "legacy-core" || it.id == "rmgn") && InstalledRootTools.isInstalled(context, it)
        }
        if (provider == null) {
            providerStatus = "Kein installierter Cert-/Helper-Setup-Provider verfügbar. Die unabhängige App schreibt packages.xml nicht selbst."
            log(Severity.WARNING, "Helper-Setup nicht verfügbar", providerStatus!!)
        } else openInstalledRootTool(context, provider)
    }
    fun startRecommendedRoot(context: Context) {
        if (probe.state == Verification.VERIFIED) {
            providerStatus = "UID 0 ist bereits verifiziert. Schritt 1 abgeschlossen."
            return
        }
        if (!automaticSelection) {
            val selected = selectedPort.lowercase()
            val mappedId = when {
                selected.contains("root-my-galaxy-next") -> "rmgn"
                selected.startsWith("busung-dev/root-my-galaxy") -> "rmg"
                selected.contains("ghostlock-galaxy") -> "zf6-root"
                selected.contains("root-my-s24") -> "veyra-root"
                else -> null
            }
            val provider = mappedId?.let { id ->
                InstalledRootTools.known.firstOrNull {
                    it.id == id && InstalledRootTools.isInstalled(context, it)
                }
            }
            if (provider == null) {
                providerStatus = "Ausgewählte Quelle ${selectedPort.ifBlank { "nicht angegeben" }} ist nicht als installierbarer App-Provider verfügbar. Wähle unten einen vorhandenen Anbieter; kein fremder Exploit wird ungeprüft gestartet."
                log(Severity.WARNING, "Manueller Provider", providerStatus!!)
            } else openInstalledRootTool(context, provider)
            return
        }
        // Disabling Veyra's preliminary device check allows choosing and opening an
        // installed provider, but that provider still owns its own compatibility checks.
        val eligibleProfiles = if (deviceVerification) matchedProfiles.size else maxOf(1, matchedProfiles.size)
        val provider = InstalledRootTools.recommended(context, device, eligibleProfiles)
        if (provider == null) {
            providerStatus = if (deviceVerification)
                "Kein geprüftes Geräteprofil für einen installierten Root-Provider verfügbar. Feed-Treffer allein garantieren keinen Exploit."
            else "Kein installierter kompatibler Root-Provider vorhanden. Geräteprüfung ist deaktiviert, aber es wird kein ungeprüfter Kernel-Exploit ausgeführt."
            log(Severity.WARNING, "Keine Root-Engine verfügbar", providerStatus!!)
        } else openInstalledRootTool(context, provider)
    }
    @JvmName("applyBerryPalette") fun setBerry(value: Boolean) = updatePalette(value)
    fun log(severity: Severity, event: String, detail: String) { store.append(AppLog(System.currentTimeMillis(), severity, event, detail)); logs = store.logs() }
    fun clearLogs() { store.clearLogs(); logs = emptyList(); exportStatus = "Lokales Protokoll geleert" }
    fun openInstalledRootTool(context: Context, tool: InstalledRootTool) {
        InstalledRootTools.open(context, tool)
            .onSuccess {
                pendingExternalRootCheck = true
                providerStatus = tool.title + " geöffnet. Root wird erst nach Rückkehr erneut überprüft."
                log(Severity.INFO, "Externes Root-Tool geöffnet", tool.packageName)
            }
            .onFailure {
                providerStatus = "Start fehlgeschlagen: " + (it.message ?: "Unbekannter Fehler")
                log(Severity.ERROR, "Root-Tool nicht geöffnet", providerStatus ?: "")
            }
    }

    fun onAppResumed() {
        activityRevision += 1
        refreshHelper()
        if (pendingExternalRootCheck) {
            pendingExternalRootCheck = false
            providerStatus = "Zurück in VeyraCore: prüfe vorhandenes UID 0 erneut."
            checkRoot()
        }
    }

    fun checkRoot() = viewModelScope.launch {
        checking = true; log(Severity.INFO, "Root-Prüfung", "Begrenzter su -c id Test gestartet")
        val outcome = shell.probe(); probe = outcome; checking = false
        refreshHelper()
        log(if (outcome.state == Verification.VERIFIED) Severity.SUCCESS else Severity.WARNING, "Root-Prüfung beendet", outcome.detail)
        if (outcome.state == Verification.VERIFIED) {
            refreshVeyraLoad()
            refreshModules()
        } else load = VeyraLoadSnapshot(
            root = outcome.state, status = "Ohne UID0 kein Veyra Load Zugriff"
        )
    }
    fun refreshModules() = viewModelScope.launch {
        moduleStatus = "Module werden über autorisiertes su gelesen …"
        shell.modules().onSuccess { modules = it; moduleStatus = if (it.isEmpty()) "Keine Module in /data/adb/modules gefunden" else "${it.size} Modul(e) geladen"; log(Severity.INFO, "Module aktualisiert", moduleStatus) }
            .onFailure { moduleStatus = it.message ?: "Modulzugriff fehlgeschlagen"; log(Severity.ERROR, "Module nicht geladen", moduleStatus) }
    }
    fun toggleModule(module: RootModule) = viewModelScope.launch {
        shell.setModuleDisabled(module.id, !module.disabled).onSuccess { log(Severity.SUCCESS, "Modulstatus geändert", module.id); refreshModules() }
            .onFailure { log(Severity.ERROR, "Modulstatus unverändert", it.message ?: module.id) }
    }
    fun removeModule(module: RootModule) = viewModelScope.launch {
        shell.markModuleRemoval(module.id).onSuccess { log(Severity.WARNING, "Entfernung für Neustart markiert", module.id); refreshModules() }
            .onFailure { log(Severity.ERROR, "Entfernung nicht markiert", it.message ?: module.id) }
    }
    fun stageModule(uri: Uri) = viewModelScope.launch {
        stagedModule?.file?.delete()
        stagedModule = null
        moduleImportStatus = "ZIP wird geprüft …"
        moduleImporter.stage(uri)
            .onSuccess {
                stagedModule = it
                moduleImportStatus = "ZIP geprüft: ${it.summary.count} Dateien · ${it.summary.unpackedBytes / 1024} KiB"
                log(Severity.INFO, "Modul-ZIP geprüft", moduleImportStatus)
            }
            .onFailure {
                moduleImportStatus = "ZIP abgelehnt: ${it.message ?: "Unbekannter Fehler"}"
                log(Severity.ERROR, "Modul-ZIP abgelehnt", moduleImportStatus)
            }
    }
    fun discardStagedModule() {
        stagedModule?.file?.delete()
        stagedModule = null
        moduleImportStatus = "Import abgebrochen"
    }
    fun installStagedModule() = viewModelScope.launch {
        val pending = stagedModule ?: return@launch
        stagedModule = null
        moduleImportStatus = "Kompatible Root-Runtime wird geprüft …"
        moduleImporter.install(pending)
            .onSuccess {
                moduleImportStatus = "Installationsauftrag abgeschlossen"
                log(Severity.SUCCESS, "Modul installiert", it)
                refreshModules()
            }
            .onFailure {
                moduleImportStatus = it.message ?: "Keine kompatible Modul-Runtime"
                log(Severity.ERROR, "Modulinstallation", moduleImportStatus)
            }
        pending.file.delete()
    }

    fun exportLogs() = viewModelScope.launch {
        val uri = withContext(Dispatchers.IO) {
            val values = ContentValues().apply { put(MediaStore.Downloads.DISPLAY_NAME, "VeyraCore-diagnostics-${System.currentTimeMillis()}.txt"); put(MediaStore.Downloads.MIME_TYPE, "text/plain"); put(MediaStore.Downloads.IS_PENDING, 1) }
            val resolver = getApplication<Application>().contentResolver
            val target = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
            runCatching { resolver.openOutputStream(target)?.bufferedWriter()?.use { writer -> logs.sortedBy { it.epochMs }.forEach { writer.appendLine("${Date(it.epochMs)} [${it.severity}] ${it.event}: ${it.detail}") } }; values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0); resolver.update(target, values, null, null); target }.getOrNull()
        }
        exportStatus = uri?.let { "Exportiert: $it" } ?: "Export fehlgeschlagen"
        log(if (uri != null) Severity.SUCCESS else Severity.ERROR, "Diagnose-Export", exportStatus!!)
    }
}

private val Oled = darkColorScheme(primary = Color(0xFFA855F7), secondary = Color(0xFFD2A6FF), background = Color.Black, surface = Color(0xFF080808), surfaceVariant = Color(0xFF121014), onBackground = Color(0xFFF8F5FB), onSurface = Color(0xFFF8F5FB), onSurfaceVariant = Color(0xFFCABFD1), outline = Color(0xFF40364A), error = Color(0xFFFF8E9D))
private val Berry = darkColorScheme(primary = Color(0xFFE84A77), secondary = Color(0xFFB17DF6), background = Color(0xFF100C14), surface = Color(0xFF1B1520), surfaceVariant = Color(0xFF281D2D), onBackground = Color(0xFFF8F5FB), onSurface = Color(0xFFF8F5FB), onSurfaceVariant = Color(0xFFD4C4D5), outline = Color(0xFF524254), error = Color(0xFFFF8E9D))
private val Green = Color(0xFF83DDAC)
private val Amber = Color(0xFFF6C86F)

@Composable private fun VeyraApp(vm: VeyraViewModel) {
    val colors = if (vm.berry) Berry else Oled
    val activity = LocalContext.current as? ComponentActivity
    SideEffect { activity?.window?.let { WindowCompat.setDecorFitsSystemWindows(it, false); it.statusBarColor = colors.background.toArgb(); it.navigationBarColor = colors.background.toArgb() } }
    MaterialTheme(colorScheme = colors, typography = Typography(titleLarge = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.SemiBold), headlineMedium = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold))) {
        var tab by rememberSaveable { mutableStateOf(Tab.Home) }
        val tabs = buildList { add(Tab.Home); if (vm.probe.state == Verification.VERIFIED) { add(Tab.Modules); add(Tab.Superuser) }; add(Tab.Settings); add(Tab.Logs) }
        if (tab !in tabs) tab = Tab.Home
        Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = { NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) { tabs.forEach { item -> NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Icon(item.icon, item.label) }, label = { Text(item.label, maxLines = 1) }) } } }) { padding ->
            AnimatedContent(tab, modifier = Modifier.padding(padding).fillMaxSize(), transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) }) { selected ->
                when (selected) { Tab.Home -> Home(vm); Tab.Modules -> Modules(vm); Tab.Superuser -> Superuser(vm); Tab.Settings -> Settings(vm); Tab.Logs -> Logs(vm) }
            }
        }
    }
}
private enum class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) { Home("Home", Icons.Rounded.Home), Modules("Module", Icons.Rounded.Extension), Superuser("Superuser", Icons.Rounded.AdminPanelSettings), Settings("Optionen", Icons.Rounded.Settings), Logs("Logs", Icons.Rounded.Article) }

@Composable private fun Page(title: String, subtitle: String? = null, content: LazyListScope.() -> Unit) = LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text(title, style = MaterialTheme.typography.headlineMedium); if (subtitle != null) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp)) }; content() }
@Composable private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f)), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) }
@Composable private fun Meta(label: String, value: String) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(.38f)); Text(value, modifier = Modifier.weight(.62f), maxLines = 2, overflow = TextOverflow.Ellipsis) }

@Composable private fun DevicePanel(info: DeviceInfo) {
    val context = LocalContext.current
    Panel {
        Text("Geräteprofil", style = MaterialTheme.typography.titleMedium)
        Meta("Hersteller", info.manufacturer)
        Meta("Modell", info.model)
        Meta("Android", info.android)
        Meta("Architektur", info.abi)
        Meta("Speicherseite", info.pageSize)
        Meta("Security Patch", info.securityPatch)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .35f))
        Text("Kernel", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(info.kernel, style = MaterialTheme.typography.bodyMedium)
        Text("Build-Fingerprint", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(info.fingerprint, style = MaterialTheme.typography.bodySmall)
        OutlinedButton(
            onClick = {
                val details = "VeyraCore Device Profile\nManufacturer: ${info.manufacturer}\nModel: ${info.model}\nOS: ${info.android}\nABI: ${info.abi}\nPage Size: ${info.pageSize}\nSecurity Patch: ${info.securityPatch}\nKernel: ${info.kernel}\nFingerprint: ${info.fingerprint}"
                context.getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText("VeyraCore device profile", details))
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Geräteprofil kopieren")
        }
        Text("Exakte Kernel- und Firmwaredaten für die Kompatibilitätsprüfung. Ein ähnlicher Kernel ist keine bestätigte Root-Unterstützung.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun Home(vm: VeyraViewModel) = Page("VeyraCore", "v0.4.0-dev · Veyra Load Runtime") {
    item { Panel { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Shield, null, tint = MaterialTheme.colorScheme.onPrimary) }; Spacer(Modifier.width(12.dp)); Column { Text("Root-Status", style = MaterialTheme.typography.titleMedium); StatusText(vm.probe.state, vm.probe.detail) } }; Button(onClick = vm::checkRoot, enabled = !vm.checking, modifier = Modifier.fillMaxWidth()) { if (vm.checking) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); else Icon(Icons.Rounded.Security, null); Spacer(Modifier.width(8.dp)); Text(if (vm.checking) "Prüfung läuft" else "Root prüfen") }; Text("Nachweis: begrenztes „su -c id“. Kein Status wird aus einem Dateipfad oder einer Manager-App abgeleitet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    item { RootStartPanel(vm) }
    item { VeyraLoadPanel(vm) }
    item { Text("Root-Provider", style = MaterialTheme.typography.titleLarge) }
    item { Text("Vorhandene Root-App wirklich öffnen und danach UID 0 erneut verifizieren. Der eingebaute Katalog enthält Forschungsquellen, keine ausführbaren Universal-Exploits.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    InstalledRootTools.known.forEach { tool -> item { InstalledToolPanel(vm, tool) } }
    vm.providerStatus?.let { message -> item { Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) } }
    item { Text("Gerät", style = MaterialTheme.typography.titleLarge) }
    item { DevicePanel(vm.device) }
    item { Text("VX Core Einrichtung", style = MaterialTheme.typography.titleLarge) }
    item { Panel { SetupSteps(vm) } }
    item { HelperSetupPanel(vm) }
    item { Text("Root-Methoden", style = MaterialTheme.typography.titleLarge) }
    RootRouteCatalog.routes(vm.device, vm.probe).forEach { route ->
        item {
            MethodPanel(
                route.name,
                when (route.state) {
                    RouteState.VERIFIED_EXISTING -> "UID 0 erkannt"
                    RouteState.REQUIRES_EXACT_PROFILE -> "Profil nicht bestätigt"
                    RouteState.REQUIRES_PROVIDER -> "Provider nicht verfügbar"
                },
                route.description,
                route.upstream
            )
        }
    }
    item { ResearchCatalog(vm) }
}
@Composable private fun VeyraLoadPanel(vm: VeyraViewModel) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Memory, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Veyra Load · Eigene Runtime-Verwaltung",
                style = MaterialTheme.typography.titleMedium)
        }
        Text("Veyra Load stellt eine eigene Kontrollschicht über einen freigegebenen SU-Provider bereit. Kein eingebauter Kernel-Exploit, keine Umbenennung von KernelSU-Treibern.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        StatusText(vm.load.root, vm.load.status)
        if (vm.load.authorized) {
            Meta("Laufzeit", vm.load.backend.title)
            Meta("SU-Nachweis", "UID ${vm.load.verifiedUid}")
            Meta("SELinux", vm.load.selinux)
            Meta("Module", vm.load.moduleCount?.toString() ?: "Nicht lesbar")
            Meta("Installation", if (vm.load.moduleInstaller) "Über erkannte Runtime" else "Nicht verfügbar")
        } else {
            Text("Nach Root-Freigabe prüft Veyra Load die tatsächliche SU-Runtime und den Modulzugriff.",
                style = MaterialTheme.typography.bodySmall,
                color = Amber)
        }
        OutlinedButton(onClick = vm::refreshVeyraLoad,
            enabled = !vm.loadBusy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (vm.loadBusy) "Runtime wird geprüft ..." else "Veyra Load aktualisieren")
        }
    }
}

@Composable private fun RootStartPanel(vm: VeyraViewModel) {
    val context = LocalContext.current
    val selected = RootPortRegistry.all.find { it.id == vm.selectedPort }
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Bolt, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("01 · ROOT ERHALTEN", style = MaterialTheme.typography.titleMedium)
        }
        Text("Vorhandene Root-Rechte (KernelSU, Magisk oder kompatibles su) akzeptieren oder passenden Anbieter starten. Erfolg zählt ausschließlich mit UID 0.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Automatische Methodenauswahl")
                Text(if (vm.automaticSelection) "Modell-/Kernel-Kandidaten bevorzugen" else
                    "Manuelle Quelle: ${selected?.title ?: "Noch keine gewählt"}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = vm.automaticSelection, onCheckedChange = vm::setAutomaticSelection)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text("Geräte- und Kernelprüfung")
                Text(if (vm.deviceVerification) "Vorauswahl nach Modell und Kernel" else
                    "Deaktiviert: nur manuelle Untersuchung, keine automatische Exploit-Ausführung",
                    style = MaterialTheme.typography.bodySmall, color = Amber)
            }
            Switch(checked = vm.deviceVerification, onCheckedChange = vm::setDeviceVerification)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .35f))
        Text(vm.feedStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        vm.matchedProfiles.take(4).forEach { profile ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CheckCircleOutline, null, tint = Amber, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(7.dp))
                Column {
                    Text(profile.label, style = MaterialTheme.typography.bodyMedium)
                    Text("${profile.source} · ${profile.id} · vorläufiger Profil-Treffer",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (vm.matchedProfiles.isEmpty()) {
            Text("Kein bestätigter Modell-/Kernel-Treffer in den verfügbaren Feeds. Keine automatische Exploit-Freigabe.",
                style = MaterialTheme.typography.bodySmall, color = Amber)
        }
        Button(onClick = { vm.startRecommendedRoot(context) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Rounded.PlayArrow, null)
            Spacer(Modifier.width(8.dp))
            Text(if (vm.probe.state == Verification.VERIFIED) "Root bereits vorhanden" else "Root-Provider starten")
        }
        OutlinedButton(onClick = vm::checkRoot, enabled = !vm.checking, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Rounded.VerifiedUser, null)
            Spacer(Modifier.width(8.dp))
            Text("Vorhandenes Root prüfen / KSU-Freigabe")
        }
        TextButton(onClick = vm::refreshPayloadFeeds, enabled = !vm.feedLoading) {
            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (vm.feedLoading) "Feeds laden …" else "Payload-Profile neu einlesen")
        }
    }
}

@Composable private fun HelperSetupPanel(vm: VeyraViewModel) {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.afterNotificationPermission(it)
    }
    val helperPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.stageHelperApk(uri)
    }
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AdminPanelSettings, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("VX Core Certificate & Helper", style = MaterialTheme.typography.titleMedium)
        }
        Text(vm.helper.detail, color = if (vm.helper.acceptedSystemUid) Green else Amber,
            style = MaterialTheme.typography.bodyMedium)
        vm.helper.packageName?.let { Meta("Paket", it) }
        vm.helper.versionName?.let { Meta("Version", it) }
        vm.helper.uid?.let { Meta("Installierte UID", it.toString()) }
        vm.helper.signerSha256?.let {
            Text("APK-Signer SHA-256", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
        Text("Zertifikatsannahme wird aus der tatsächlich installierten Android-System-UID 1000 des Helpers abgeleitet. UID 1000 ersetzt keine Root-UID 0. Ein Neustart kann für PackageManager-Änderungen notwendig sein.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!vm.helper.acceptedSystemUid) {
            OutlinedButton(onClick = { vm.setupViaExistingProvider(context) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Rounded.Build, null)
                Spacer(Modifier.width(8.dp))
                Text("Zertifikat / Helper einrichten")
            }
            Text("Die bisherige VeyraCore oder RMG Next führt das echte Zertifikats-Setup aus. Diese Version schreibt keine ungetesteten Paketmanager-Dateien.",
                style = MaterialTheme.typography.bodySmall, color = Amber)
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Permanente Helper-Benachrichtigung")
                    Text(if (vm.notificationActive) "Benachrichtigung erkannt" else
                        "Statusanzeige noch nicht bestätigt",
                        color = if (vm.notificationActive) Green else Amber,
                        style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = vm.notificationEnabled, onCheckedChange = {
                    if (it && !HelperNotificationControl.hasPermission(context)) {
                        permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    } else vm.setNotifications(it)
                })
            }
            if (vm.notificationEnabled && !HelperNotificationControl.hasPermission(context)) {
                Button(onClick = { permission.launch(android.Manifest.permission.POST_NOTIFICATIONS) },
                    modifier = Modifier.fillMaxWidth()) { Text("Benachrichtigungen erlauben") }
            } else if (vm.notificationEnabled && !vm.notificationActive) {
                OutlinedButton(onClick = { vm.setNotifications(true) },
                    modifier = Modifier.fillMaxWidth()) { Text("Helper-Benachrichtigung aktivieren") }
            }
        }
        if (vm.helper.acceptedSystemUid) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .3f))
            Text("Helper-APK aktualisieren", style = MaterialTheme.typography.titleSmall)
            Text("Eigene Installer-Strecke mit Signaturvergleich. Nur eine bereits vertrauenswürdig installierte Helper-Paketkennung wird zum Update angeboten; keine Veränderung der Android-Systemsignatur.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
                onClick = { helperPicker.launch(arrayOf(
                    "application/vnd.android.package-archive", "application/octet-stream")) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Icon(Icons.Rounded.SystemUpdate, null)
                Spacer(Modifier.width(8.dp))
                Text("Signierte Helper-APK auswählen")
            }
            Text(vm.helperInstallStatus, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = vm::refreshHelper, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Zertifikat & Helper erneut prüfen")
        }
    }
    HelperUpdateConfirmation(vm)
}

@Composable private fun HelperUpdateConfirmation(vm: VeyraViewModel) {
    val context = LocalContext.current
    vm.stagedHelper?.let { staged ->
        AlertDialog(
            onDismissRequest = vm::discardHelperApk,
            title = { Text("Veyra Helper aktualisieren?") },
            text = { Text("${staged.packageName} · Version ${staged.versionName}. SHA-256 der Signatur wurde mit dem installierten System-Helper abgeglichen. Android entscheidet abschließend über das Paket-Update.") },
            confirmButton = { TextButton(onClick = { vm.requestHelperUpdate(context) }) { Text("Android-Installer öffnen") } },
            dismissButton = { TextButton(onClick = vm::discardHelperApk) { Text("Abbrechen") } }
        )
    }
}

@Composable private fun StatusText(state: Verification, detail: String) { val color = when(state) { Verification.VERIFIED -> Green; Verification.NOT_VERIFIED, Verification.ERROR -> MaterialTheme.colorScheme.error; Verification.UNKNOWN -> Amber }; Text(when(state) { Verification.VERIFIED -> "VERIFIZIERT"; Verification.NOT_VERIFIED -> "NICHT VERIFIZIERT"; Verification.ERROR -> "FEHLER"; Verification.UNKNOWN -> "UNBEKANNT" }, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
@Composable private fun SetupSteps(vm: VeyraViewModel) {
    val steps = listOf(
        Triple("01 · Root erhalten (UID 0)", vm.probe.state == Verification.VERIFIED,
            if (vm.probe.state == Verification.VERIFIED) "su -c id bestätigt" else "Externen Provider starten oder vorhandenes su freigeben"),
        Triple("02 · VX Core Zertifikat", vm.helper.acceptedSystemUid,
            if (vm.helper.acceptedSystemUid) "System-PackageManager hat den Helper unter UID 1000 akzeptiert" else "Signatur- und PackageManager-Schritt noch nicht bestätigt"),
        Triple("03 · Veyra Helper", vm.helper.acceptedSystemUid,
            if (vm.helper.acceptedSystemUid) "${vm.helper.packageName} · UID ${vm.helper.uid}" else vm.helper.detail),
        Triple("04 · Reroot / Soft Reboot", false,
            "Ein Framework-Neustart muss separat nachgewiesen werden; kein automatischer Erfolg"),
        Triple("05 · Permanente Benachrichtigung", vm.notificationActive,
            if (vm.notificationActive) "Benachrichtigung aktiv" else "Noch nicht nachgewiesen")
    )
    steps.forEachIndexed { index, (title, verified, detail) ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (verified) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                null, tint = if (verified) Green else Amber, modifier = Modifier.size(23.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        if (index != steps.lastIndex) {
            HorizontalDivider(Modifier.padding(start = 35.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = .38f))
        }
    }
}
@Composable private fun MethodPanel(name: String, status: String, detail: String, source: String? = null) {
    val uri = LocalUriHandler.current
    Panel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(status, color = if (status.contains("UID 0")) Green else Amber, style = MaterialTheme.typography.labelSmall)
        }
        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        if (source != null) {
            OutlinedButton(onClick = { uri.openUri(source) }, modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)) {
                Icon(Icons.Rounded.OpenInNew, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Quelle & Kompatibilität")
            }
        }
    }
}

@Composable private fun InstalledToolPanel(vm: VeyraViewModel, tool: InstalledRootTool) {
    val context = LocalContext.current
    val installed = remember(vm.activityRevision, tool.packageName) { InstalledRootTools.isInstalled(context, tool) }
    Panel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tool.title, style = MaterialTheme.typography.titleMedium)
                Text(if (installed) "Installiert · externer Start" else "Nicht installiert", color = if (installed) Green else Amber, style = MaterialTheme.typography.labelSmall)
            }
            Icon(Icons.Rounded.Smartphone, null, tint = MaterialTheme.colorScheme.primary)
        }
        Text(tool.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(
            onClick = { vm.openInstalledRootTool(context, tool) },
            enabled = installed,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Icon(Icons.Rounded.OpenInNew, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (installed) "App öffnen" else "App nicht verfügbar")
        }
    }
}

@Composable private fun ResearchCatalog(vm: VeyraViewModel) {
    val uri = LocalUriHandler.current
    var expanded by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var groupIndex by rememberSaveable { mutableIntStateOf(-1) }
    val selectedGroup = PortGroup.entries.getOrNull(groupIndex)
    val filtered = RootPortRegistry.search(query, selectedGroup)
    Panel {
        Text("Port-Bibliothek", style = MaterialTheme.typography.titleMedium)
        Text("${RootPortRegistry.all.size} Open-Source-Referenzen: Samsung, Pixel, DirtyFrag und weitere Geräte. Keine automatisch ausführbaren Exploit-Binaries.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Icon(if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
            Spacer(Modifier.width(8.dp))
            Text(if (expanded) "Port-Bibliothek ausblenden" else "Alle 57 Quellen durchsuchen")
        }
        if (expanded) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, label = { Text("Quelle oder Modell suchen") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(selectedGroup?.title ?: "Alle Kategorien", fontWeight = FontWeight.Medium)
                    Text("${filtered.size} Quellen", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { groupIndex = if (groupIndex >= PortGroup.entries.lastIndex) -1 else groupIndex + 1 }) {
                    Icon(Icons.Rounded.SwapHoriz, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Kategorie")
                }
            }
            filtered.forEach { port ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .26f))
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(port.title, style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text(port.id, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${port.group.title} · " +
                        if (port.sourceConfirmed) "Quelle erfasst, Gerätesupport ungeprüft"
                        else "Community-Link, Existenz/Kompatibilität ungeprüft",
                        style = MaterialTheme.typography.labelSmall, color = Amber)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedButton(onClick = { vm.selectPort(port.id) },
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp)) {
                            Text(if (vm.selectedPort == port.id) "Ausgewählt" else "Manuell wählen",
                                maxLines = 1, style = MaterialTheme.typography.labelMedium)
                        }
                        TextButton(onClick = { uri.openUri(port.url) },
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp)) {
                            Icon(Icons.Rounded.OpenInNew, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Quellcode", maxLines = 1)
                        }
                    }
                }
            }
            if (filtered.isEmpty()) Text("Keine Quellen gefunden.", color = Amber)
            Text("Die Auswahl merkt sich eine Forschungsquelle. Die eigentliche Root-Ausführung benötigt einen dafür geeigneten verifizierten Provider.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
@Composable private fun Modules(vm: VeyraViewModel) = Page("Module", "Echte, vorsichtige Verwaltung bereits vorhandener Root-Module") { item { Panel { Text(vm.moduleStatus, color = MaterialTheme.colorScheme.onSurfaceVariant); OutlinedButton(onClick = vm::refreshModules, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Aktualisieren") }; Text("Liest /data/adb/modules via autorisiertem su. IDs werden vor jeder Pfadoperation validiert.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; item { ModuleImportCard(vm) }; items(vm.modules, key = { it.id }) { module -> ModuleCard(module, vm) } }
@Composable private fun ModuleImportCard(vm: VeyraViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.stageModule(uri)
    }
    Panel {
        Text("Modul hinzufügen", style = MaterialTheme.typography.titleMedium)
        Text("VeyraCore prüft die ZIP und übergibt sie nach Bestätigung an einen verifizierten Modul-Installer. Keine eigenständige Ausführung von ZIP-Skripten.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = { picker.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        ) {
            Icon(Icons.Rounded.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Modul-ZIP auswählen")
        }
        Text(vm.moduleImportStatus, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
    vm.stagedModule?.let { staged ->
        AlertDialog(
            onDismissRequest = vm::discardStagedModule,
            title = { Text("Modul installieren?") },
            text = {
                Text("Die ZIP enthält ${staged.summary.count} Einträge (${staged.summary.unpackedBytes / 1024} KiB entpackt). Installationsskripte können Root-Rechte ausführen. Nur vertrauenswürdige Module bestätigen.")
            },
            confirmButton = { TextButton(onClick = vm::installStagedModule) { Text("Installieren") } },
            dismissButton = { TextButton(onClick = vm::discardStagedModule) { Text("Abbrechen") } }
        )
    }
}
@Composable private fun ModuleCard(module: RootModule, vm: VeyraViewModel) { var confirm by remember { mutableStateOf(false) }; Panel { Text(module.name, style = MaterialTheme.typography.titleMedium); Text("${module.id} · ${module.version}", color = MaterialTheme.colorScheme.secondary); if (module.author.isNotBlank()) Text("von ${module.author}", style = MaterialTheme.typography.bodySmall); if (module.description.isNotBlank()) Text(module.description, color = MaterialTheme.colorScheme.onSurfaceVariant); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilledTonalButton(onClick = { vm.toggleModule(module) }, modifier = Modifier.weight(1f)) { Text(if (module.disabled) "Aktivieren" else "Deaktivieren") }; OutlinedButton(onClick = { confirm = true }, modifier = Modifier.weight(1f), enabled = !module.markedForRemoval) { Text(if (module.markedForRemoval) "Entfernung markiert" else "Entfernen") } } }; if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Entfernung markieren?") }, text = { Text("${module.name} wird nicht jetzt gelöscht. Die sichere remove-Markierung wird für den nächsten Boot gesetzt.") }, confirmButton = { TextButton(onClick = { confirm = false; vm.removeModule(module) }) { Text("Für Neustart markieren") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Abbrechen") } }) }
@Composable private fun Superuser(vm: VeyraViewModel) =
    Page("Superuser", "Belege und freigegebene Root-Runtime") {
        item {
            Panel {
                Text("UID 0 Freigabe", style = MaterialTheme.typography.titleMedium)
                StatusText(vm.probe.state, vm.probe.detail)
                Text("Eine installierte Manager-App oder UID 1000 wird nicht als Root gewertet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            VeyraLoadPanel(vm)
        }
        item {
            Panel {
                Text("Rechteverwaltung", style = MaterialTheme.typography.titleMedium)
                Text("Root-Anfragen werden vom vorhandenen SU-Provider autorisiert. VeyraCore kann ohne dessen eigene Berechtigungs-API keine fremden App-Regeln ändern und zeigt deshalb keine erfundene Allow/Deny-Liste.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Aktiver Backend-Typ: ${vm.load.backend.title}",
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

@Composable private fun Settings(vm: VeyraViewModel) {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.afterNotificationPermission(it)
    }
    Page("Einstellungen", "Lokale, transparente Optionen") {
        item {
            Panel {
                Text("Erscheinungsbild", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(if (vm.berry) "Veyra Berry" else "Veyra OLED")
                        Text(if (vm.berry) "#100C14 · Berry #E84A77 · Violett #B17DF6" else
                            "#000000 · Violett #A855F7",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = vm.berry, onCheckedChange = vm::setBerry)
                }
            }
        }
        item {
            Panel {
                Text("Root-Auswahl", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Automatische Auswahl")
                        Text("Exakte Geräteprofile und vorhandene Anbieter berücksichtigen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = vm.automaticSelection, onCheckedChange = vm::setAutomaticSelection)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = .32f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Gerät verifizieren")
                        Text("Abschalten erlaubt manuelle Quellenwahl, jedoch keine stille Ausführung fremder Kernel-Payloads.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = vm.deviceVerification, onCheckedChange = vm::setDeviceVerification)
                }
                if (!vm.automaticSelection) Text(
                    "Manuelle Quelle: ${vm.selectedPort.ifBlank { "Keine ausgewählt" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary)
            }
        }
        item {
            Panel {
                Text("Veyra Helper", style = MaterialTheme.typography.titleMedium)
                Text(vm.helper.detail, style = MaterialTheme.typography.bodySmall,
                    color = if (vm.helper.acceptedSystemUid) Green else Amber)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Permanente Statusmeldung")
                        Text("Nur bei nachgewiesener Helper-System-UID 1000",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = vm.notificationEnabled, onCheckedChange = {
                        if (it && !HelperNotificationControl.hasPermission(context))
                            permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        else vm.setNotifications(it)
                    })
                }
                Text(if (vm.notificationActive) "Statusmeldung aktiv" else
                    "Statusmeldung noch nicht nachgewiesen",
                    color = if (vm.notificationActive) Green else Amber,
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = vm::refreshHelper, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Helper erneut prüfen")
                }
            }
        }
        item {
            Panel {
                Text("Root / Runtime / Quellen", style = MaterialTheme.typography.titleMedium)
                Text("Root-Beweis: su -c id, UID 0. Zertifikatsbeweis: System-UID 1000 des installierten Helpers. Eintrag im GitHub-Katalog ist kein Exploit-Nachweis.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
                Text("VeyraCore 0.4.0-dev · eigenständige App. KernelSU-Module werden nur über tatsächlich verfügbare Root-Runtimes verwaltet. Keine in diese Version kopierten Treiber oder neuen Kernel-Patches.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = {
                    val diagnostics = "VeyraCore 0.4.0-dev\n${vm.device}\nRoot: ${vm.probe}\nHelper: ${vm.helper}\nProfiles: ${vm.feedStatus}\nAutomatic: ${vm.automaticSelection}\nDeviceCheck: ${vm.deviceVerification}"
                    context.getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText("VeyraCore diagnostics", diagnostics))
                    vm.log(Severity.INFO, "Diagnose kopiert", "Zwischenablage")
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.ContentCopy, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Diagnose kopieren")
                }
            }
        }
    }
}
@Composable private fun Logs(vm: VeyraViewModel) = Page("Logs", "Lokal gespeichert · exportierbar ohne Speicherberechtigung") { item { Panel { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = vm::exportLogs, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.FileDownload, null); Spacer(Modifier.width(6.dp)); Text("Export") }; OutlinedButton(onClick = vm::clearLogs, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.DeleteOutline, null); Spacer(Modifier.width(6.dp)); Text("Leeren") } }; vm.exportStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }; items(vm.logs, key = { it.epochMs.toString() + it.event }) { entry -> Panel { Text(entry.event, style = MaterialTheme.typography.titleMedium); Text("${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(entry.epochMs))} · ${entry.severity}", color = if (entry.severity == Severity.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall); Text(entry.detail, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }