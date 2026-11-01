package ctrl.veyracore

import android.content.Context
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Base64
import java.util.concurrent.TimeUnit

enum class Verification { UNKNOWN, VERIFIED, NOT_VERIFIED, ERROR }
enum class Severity { INFO, SUCCESS, WARNING, ERROR }

data class DeviceInfo(val manufacturer: String, val model: String, val android: String, val kernel: String, val fingerprint: String) {
    companion object {
        fun current() = DeviceInfo(Build.MANUFACTURER.ifBlank { "Unbekannt" }, Build.MODEL.ifBlank { "Unbekannt" }, "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", System.getProperty("os.version") ?: "Unbekannt", Build.FINGERPRINT ?: "Unbekannt")
    }
}
data class RootProbe(val state: Verification = Verification.UNKNOWN, val detail: String = "Noch nicht geprüft", val selinux: String? = null, val runtime: String? = null)
data class AppLog(val epochMs: Long, val severity: Severity, val event: String, val detail: String)
data class ShellResult(val exitCode: Int?, val output: String, val timedOut: Boolean = false, val error: String? = null)
data class RootModule(val id: String, val name: String, val version: String, val author: String, val description: String, val disabled: Boolean, val markedForRemoval: Boolean)

/** Boundary for future, separately reviewed root integrations. This app ships no exploit chain. */
interface RootProvider {
    val id: String
    val displayName: String
    suspend fun availability(device: DeviceInfo): ProviderAvailability
    suspend fun begin(): ProviderResult
}
data class ProviderAvailability(val state: Verification, val detail: String)
data class ProviderResult(val accepted: Boolean, val detail: String)

/** Future first-party daemon contract; no implementation is bundled in v0.1.0. */
interface VeyraRuntimeBridge {
    suspend fun connection(): ProviderAvailability
}

object ProfileMatcher {
    /** Exact matching only. A blank registry intentionally never reports compatibility. */
    fun matches(profile: SupportedProfile, info: DeviceInfo): Boolean =
        profile.manufacturer.equals(info.manufacturer, ignoreCase = true) &&
            profile.model.equals(info.model, ignoreCase = true) &&
            profile.fingerprint == info.fingerprint &&
            profile.kernel == info.kernel
}
data class SupportedProfile(val manufacturer: String, val model: String, val fingerprint: String, val kernel: String)

object ModuleId {
    private val safe = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$")
    fun isSafe(value: String): Boolean = safe.matches(value) && value !in setOf(".", "..")
}

class SafeShell {
    suspend fun su(command: String, timeoutSeconds: Long = 6): ShellResult = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
            val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
                return@withContext ShellResult(null, "", timedOut = true, error = "Zeitlimit nach ${timeoutSeconds}s")
            }
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText().take(24_000) }
            ShellResult(process.exitValue(), output.trim())
        } catch (e: Exception) {
            ShellResult(null, "", error = e.message ?: e.javaClass.simpleName)
        }
    }

    suspend fun probe(): RootProbe {
        val identity = su("id", 6)
        val root = identity.exitCode == 0 && Regex("(^|\\s)uid=0(\\s|\\(|$)").containsMatchIn(identity.output)
        if (!root) return RootProbe(Verification.NOT_VERIFIED, identity.error ?: if (identity.timedOut) "su-Prüfung zeitlich begrenzt abgebrochen" else identity.output.ifBlank { "Kein UID-0-Nachweis" })
        val security = su("getenforce 2>/dev/null || echo unknown", 3).output.ifBlank { "unknown" }
        val daemon = su("test -S /dev/socket/veyra_loadd && echo connected || echo absent", 3).output.ifBlank { "absent" }
        return RootProbe(Verification.VERIFIED, identity.output, security, daemon)
    }

    suspend fun modules(): Result<List<RootModule>> = runCatching {
        // module.prop is parsed as data, never sourced: a module controls this file.
        val result = su("for d in /data/adb/modules/*; do [ -d \"\$d\" ] || continue; id=\${d##*/}; case \"\$id\" in *[!A-Za-z0-9._-]*|'') continue;; esac; p=\"\$d/module.prop\"; readprop() { [ -r \"\$p\" ] && sed -n \"s/^\$1=//p\" \"\$p\" | head -n 1 | tr '\\036\\037\\n' '   '; }; name=\$(readprop name); version=\$(readprop version); author=\$(readprop author); description=\$(readprop description); [ -f \"\$d/disable\" ] && disabled=1 || disabled=0; [ -f \"\$d/remove\" ] && removed=1 || removed=0; printf '%s\\037%s\\037%s\\037%s\\037%s\\037%s\\037%s\\036' \"\$id\" \"\${name:-\$id}\" \"\${version:-unknown}\" \"\${author:-unknown}\" \"\${description:-}\" \"\$disabled\" \"\$removed\"; done", 8)
        require(result.exitCode == 0) { result.error ?: result.output.ifBlank { "Modulpfad nicht zugreifbar" } }
        result.output.split('\u001e').filter { it.isNotBlank() }.mapNotNull { row ->
            val f = row.split('\u001f')
            if (f.size == 7 && ModuleId.isSafe(f[0])) RootModule(f[0], f[1], f[2], f[3], f[4], f[5] == "1", f[6] == "1") else null
        }
    }

    suspend fun setModuleDisabled(id: String, disabled: Boolean): Result<Unit> = guardedModuleCommand(id, "disable", disabled)
    suspend fun markModuleRemoval(id: String): Result<Unit> = guardedModuleCommand(id, "remove", true)
    private suspend fun guardedModuleCommand(id: String, marker: String, create: Boolean): Result<Unit> = runCatching {
        require(ModuleId.isSafe(id)) { "Ungültige Modul-ID" }
        val command = if (create) "touch /data/adb/modules/$id/$marker" else "rm -f /data/adb/modules/$id/$marker"
        val result = su(command, 5)
        require(result.exitCode == 0) { result.error ?: result.output.ifBlank { "Shell-Befehl fehlgeschlagen" } }
    }
}

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("veyracore", Context.MODE_PRIVATE)
    var berry: Boolean
        get() = prefs.getBoolean("berry", false)
        set(value) { prefs.edit().putBoolean("berry", value).apply() }
    fun append(log: AppLog) {
        val encoded = Base64.getEncoder().encodeToString("${log.epochMs}|${log.severity}|${log.event}|${log.detail}".toByteArray())
        val all = (prefs.getString("logs", "") ?: "").split(',').filter { it.isNotBlank() }.takeLast(199) + encoded
        prefs.edit().putString("logs", all.joinToString(",")).apply()
    }
    fun logs(): List<AppLog> = (prefs.getString("logs", "") ?: "").split(',').mapNotNull { entry ->
        runCatching { String(Base64.getDecoder().decode(entry)).split('|', limit = 4) }.getOrNull()?.let { p ->
            if (p.size == 4) runCatching { AppLog(p[0].toLong(), Severity.valueOf(p[1]), p[2], p[3]) }.getOrNull() else null
        }
    }.sortedByDescending { it.epochMs }
    fun clearLogs() = prefs.edit().remove("logs").apply()
}
