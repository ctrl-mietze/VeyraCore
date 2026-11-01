package ctrl.veyracore

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
}

class VeyraViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalStore(app)
    private val shell = SafeShell()
    var berry by mutableStateOf(store.berry); private set
    var probe by mutableStateOf(RootProbe()); private set
    var checking by mutableStateOf(false); private set
    var modules by mutableStateOf<List<RootModule>>(emptyList()); private set
    var moduleStatus by mutableStateOf("Noch nicht geladen"); private set
    var logs by mutableStateOf(store.logs()); private set
    var exportStatus by mutableStateOf<String?>(null); private set
    val device = DeviceInfo.current()

    init { log(Severity.INFO, "VeyraCore gestartet", "v0.1.0; Rootstatus wird nie aus einer App-Erkennung abgeleitet.") }
    fun updatePalette(value: Boolean) { berry = value; store.berry = value; log(Severity.INFO, "Design geändert", if (value) "Veyra Berry" else "Veyra OLED") }
    @JvmName("applyBerryPalette") fun setBerry(value: Boolean) = updatePalette(value)
    fun log(severity: Severity, event: String, detail: String) { store.append(AppLog(System.currentTimeMillis(), severity, event, detail)); logs = store.logs() }
    fun clearLogs() { store.clearLogs(); logs = emptyList(); exportStatus = "Lokales Protokoll geleert" }
    fun checkRoot() = viewModelScope.launch {
        checking = true; log(Severity.INFO, "Root-Prüfung", "Begrenzter su -c id Test gestartet")
        val outcome = shell.probe(); probe = outcome; checking = false
        log(if (outcome.state == Verification.VERIFIED) Severity.SUCCESS else Severity.WARNING, "Root-Prüfung beendet", outcome.detail)
        if (outcome.state == Verification.VERIFIED) refreshModules()
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

private val Oled = darkColorScheme(primary = Color(0xFFA855F7), secondary = Color(0xFFD2A6FF), background = Color.Black, surface = Color(0xFF0B0A0F), surfaceVariant = Color(0xFF17141E), onBackground = Color(0xFFF8F5FB), onSurface = Color(0xFFF8F5FB), onSurfaceVariant = Color(0xFFCABFD1), outline = Color(0xFF40364A), error = Color(0xFFFF8E9D))
private val Berry = darkColorScheme(primary = Color(0xFFE83A67), secondary = Color(0xFFB078EB), background = Color(0xFF100C14), surface = Color(0xFF1B1520), surfaceVariant = Color(0xFF281D2D), onBackground = Color(0xFFF8F5FB), onSurface = Color(0xFFF8F5FB), onSurfaceVariant = Color(0xFFD4C4D5), outline = Color(0xFF524254), error = Color(0xFFFF8E9D))
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
private enum class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) { Home("Home", Icons.Rounded.Home), Modules("Module", Icons.Rounded.Extension), Superuser("Superuser", Icons.Rounded.AdminPanelSettings), Settings("Einstellungen", Icons.Rounded.Settings), Logs("Logs", Icons.Rounded.Article) }

@Composable private fun Page(title: String, subtitle: String? = null, content: LazyListScope.() -> Unit) = LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text(title, style = MaterialTheme.typography.headlineMedium); if (subtitle != null) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp)) }; content() }
@Composable private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .72f)), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) }
@Composable private fun Meta(label: String, value: String) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(.38f)); Text(value, modifier = Modifier.weight(.62f), maxLines = 2, overflow = TextOverflow.Ellipsis) }

@Composable private fun Home(vm: VeyraViewModel) = Page("VeyraCore", "v0.1.0 · unabhängige Runtime-Grundlage") {
    item { Panel { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Shield, null, tint = MaterialTheme.colorScheme.onPrimary) }; Spacer(Modifier.width(12.dp)); Column { Text("Root-Status", style = MaterialTheme.typography.titleMedium); StatusText(vm.probe.state, vm.probe.detail) } }; Button(onClick = vm::checkRoot, enabled = !vm.checking, modifier = Modifier.fillMaxWidth()) { if (vm.checking) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); else Icon(Icons.Rounded.Security, null); Spacer(Modifier.width(8.dp)); Text(if (vm.checking) "Prüfung läuft" else "Root prüfen") }; Text("Nachweis: begrenztes „su -c id“. Kein Status wird aus einem Dateipfad oder einer Manager-App abgeleitet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    item { Text("Gerät", style = MaterialTheme.typography.titleLarge) }
    item { Panel { Meta("Hersteller", vm.device.manufacturer); Meta("Modell", vm.device.model); Meta("System", vm.device.android); Meta("Kernel", vm.device.kernel); Meta("Build", vm.device.fingerprint) } }
    item { Text("Einrichtungsstatus", style = MaterialTheme.typography.titleLarge) }
    item { Panel { SetupSteps(vm.probe) } }
    item { Text("Root-Methoden", style = MaterialTheme.typography.titleLarge) }
    item { MethodPanel("GhostLock", "Kein Chain-Import", "Keine signierte, exakte Profilquelle integriert. Daher unverifiziert; Start ist absichtlich nicht verfügbar.") }
    item { MethodPanel("DirtyFrag · CVE-2026-43284", "Unverifiziert", "Keine Payloads, KMI-Tabellen oder Exploit-Logik in dieser APK. Nur künftige, separat geprüfte Integration möglich.") }
    item { MethodPanel("Root My Galaxy Profile", "Quelle nicht integriert", "Profilmatcher erwartet Hersteller, Modell, Build-Fingerprint und Kernel exakt. Der v0.1.0-Registry ist bewusst leer.") }
}
@Composable private fun StatusText(state: Verification, detail: String) { val color = when(state) { Verification.VERIFIED -> Green; Verification.NOT_VERIFIED, Verification.ERROR -> MaterialTheme.colorScheme.error; Verification.UNKNOWN -> Amber }; Text(when(state) { Verification.VERIFIED -> "VERIFIZIERT"; Verification.NOT_VERIFIED -> "NICHT VERIFIZIERT"; Verification.ERROR -> "FEHLER"; Verification.UNKNOWN -> "UNBEKANNT" }, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
@Composable private fun SetupSteps(probe: RootProbe) { val steps = listOf("Route erkannt" to Verification.UNKNOWN, "Privilegierte Ausführung" to probe.state, "Zertifikat erkannt" to Verification.UNKNOWN, "Helper-UID geprüft" to Verification.UNKNOWN, "Reboot / Reroot geprüft" to Verification.UNKNOWN, "Runtime verbunden" to if (probe.runtime == "connected") Verification.VERIFIED else Verification.UNKNOWN); steps.forEachIndexed { i, (name, status) -> Row(verticalAlignment = Alignment.CenterVertically) { val c = if (status == Verification.VERIFIED) Green else MaterialTheme.colorScheme.outline; Icon(if (status == Verification.VERIFIED) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null, tint = c, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(12.dp)); Column { Text("${i + 1}. $name"); Text(if (status == Verification.VERIFIED) "Verifiziert" else "Unbekannt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; if (i < steps.lastIndex) HorizontalDivider(Modifier.padding(start = 32.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = .4f)) } }
@Composable private fun MethodPanel(name: String, status: String, detail: String) = Panel { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(name, style = MaterialTheme.typography.titleMedium); Text(status, color = Amber, style = MaterialTheme.typography.labelMedium) }; Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant); OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("Root starten nicht verfügbar") } }

@Composable private fun Modules(vm: VeyraViewModel) = Page("Module", "Echte, vorsichtige Verwaltung bereits vorhandener Root-Module") { item { Panel { Text(vm.moduleStatus, color = MaterialTheme.colorScheme.onSurfaceVariant); OutlinedButton(onClick = vm::refreshModules, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Aktualisieren") }; Text("Liest /data/adb/modules via autorisiertem su. IDs werden vor jeder Pfadoperation validiert.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; items(vm.modules, key = { it.id }) { module -> ModuleCard(module, vm) } }
@Composable private fun ModuleCard(module: RootModule, vm: VeyraViewModel) { var confirm by remember { mutableStateOf(false) }; Panel { Text(module.name, style = MaterialTheme.typography.titleMedium); Text("${module.id} · ${module.version}", color = MaterialTheme.colorScheme.secondary); if (module.author.isNotBlank()) Text("von ${module.author}", style = MaterialTheme.typography.bodySmall); if (module.description.isNotBlank()) Text(module.description, color = MaterialTheme.colorScheme.onSurfaceVariant); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilledTonalButton(onClick = { vm.toggleModule(module) }, modifier = Modifier.weight(1f)) { Text(if (module.disabled) "Aktivieren" else "Deaktivieren") }; OutlinedButton(onClick = { confirm = true }, modifier = Modifier.weight(1f), enabled = !module.markedForRemoval) { Text(if (module.markedForRemoval) "Entfernung markiert" else "Entfernen") } } }; if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Entfernung markieren?") }, text = { Text("${module.name} wird nicht jetzt gelöscht. Die sichere remove-Markierung wird für den nächsten Boot gesetzt.") }, confirmButton = { TextButton(onClick = { confirm = false; vm.removeModule(module) }) { Text("Für Neustart markieren") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Abbrechen") } }) }
@Composable private fun Superuser(vm: VeyraViewModel) = Page("Superuser", "Diagnose ohne erfundene Berechtigungsregeln") { item { Panel { Text("Aktuelle App-Autorisierung", style = MaterialTheme.typography.titleMedium); StatusText(vm.probe.state, if (vm.probe.state == Verification.VERIFIED) "Diese App erhielt für den begrenzten UID-0-Test eine su-Antwort." else "Kein aktueller UID-0-Nachweis."); HorizontalDivider(); Text("Veyra Load Runtime", style = MaterialTheme.typography.titleMedium); StatusText(if (vm.probe.runtime == "connected") Verification.VERIFIED else Verification.UNKNOWN, if (vm.probe.runtime == "connected") "Socket /dev/socket/veyra_loadd erreichbar" else "Kein Veyra-Load-Daemon nachgewiesen"); Text("Es gibt in v0.1.0 keine Kernel-/Policy-Bridge. Allow/Deny-Listen werden daher nicht vorgetäuscht.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable private fun Settings(vm: VeyraViewModel) { val context = LocalContext.current; Page("Einstellungen", "Lokale, transparente Optionen") { item { Panel { Text("Erscheinungsbild", style = MaterialTheme.typography.titleMedium); Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(if (vm.berry) "Veyra Berry" else "Veyra OLED"); Text(if (vm.berry) "#100C14 · Rose #E83A67 · Violett #B078EB" else "#000000 · Violett #A855F7", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(checked = vm.berry, onCheckedChange = vm::setBerry) } } }; item { Panel { Text("Verifikation", style = MaterialTheme.typography.titleMedium); Text("Root wird ausschließlich manuell durch den Button auf der Startseite geprüft. Beim App-Start erfolgt keine stille su-Anfrage.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }; item { Panel { Text("Runtime & Credits", style = MaterialTheme.typography.titleMedium); Text("VeyraCore 0.1.0 · eigener UI-/Runtime-Entwurf. Keine KernelSU-, DirtyFrag- oder GhostSam-Quellen, Binaries oder Payloads enthalten.", color = MaterialTheme.colorScheme.onSurfaceVariant); OutlinedButton(onClick = { val diagnostics = "VeyraCore 0.1.0\n${vm.device}\nRoot: ${vm.probe}"; (context.getSystemService(ClipboardManager::class.java)).setPrimaryClip(ClipData.newPlainText("VeyraCore diagnostics", diagnostics)); vm.log(Severity.INFO, "Diagnose kopiert", "Zwischenablage") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("Diagnose kopieren") } } } }
}
@Composable private fun Logs(vm: VeyraViewModel) = Page("Logs", "Lokal gespeichert · exportierbar ohne Speicherberechtigung") { item { Panel { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = vm::exportLogs, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.FileDownload, null); Spacer(Modifier.width(6.dp)); Text("Export") }; OutlinedButton(onClick = vm::clearLogs, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.DeleteOutline, null); Spacer(Modifier.width(6.dp)); Text("Leeren") } }; vm.exportStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }; items(vm.logs, key = { it.epochMs.toString() + it.event }) { entry -> Panel { Text(entry.event, style = MaterialTheme.typography.titleMedium); Text("${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(entry.epochMs))} · ${entry.severity}", color = if (entry.severity == Severity.ERROR) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall); Text(entry.detail, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
