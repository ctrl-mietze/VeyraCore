package ctrl.veyracore

import java.io.File

/** Verified root-manager transport; not a custom kernel driver. */
enum class ModuleTransport(val title: String) {
    KERNELSU("KernelSU"),
    MAGISK("Magisk"),
    UNAVAILABLE("Keine passende Modul-Runtime")
}

data class InstallerChoice(
    val transport: ModuleTransport,
    val binary: String? = null,
    val message: String
)

object InstallerOutput {
    /** No shell parameters are accepted from an untrusted GitHub catalog. */
    fun parse(input: String): InstallerChoice {
        val ksu = input.lineSequence().firstOrNull { it.startsWith("KSUD=") }
            ?.substringAfter('=')
        if (ksu != null && ksu.matches(Regex("^/[A-Za-z0-9/_+.-]+/ksud$"))) {
            return InstallerChoice(ModuleTransport.KERNELSU, ksu, "KernelSU CLI gefunden")
        }
        val magisk = input.lineSequence().firstOrNull { it.startsWith("MAGISK=") }
            ?.substringAfter('=')
        if (magisk != null && magisk.matches(Regex("^/[A-Za-z0-9/_+.-]+/magisk$"))) {
            return InstallerChoice(ModuleTransport.MAGISK, magisk, "Magisk CLI gefunden")
        }
        return InstallerChoice(ModuleTransport.UNAVAILABLE, null, "Kein vertrauenswürdiger Root-Installer erreichbar")
    }
}

class VeyraModuleInstaller(private val shell: SafeShell) {
    suspend fun availability(): InstallerChoice {
        val root = shell.probe()
        if (root.state != Verification.VERIFIED)
            return InstallerChoice(ModuleTransport.UNAVAILABLE, null, "UID 0 nicht bestätigt")
        val response = shell.su(
            "if [ -x /data/adb/ksu/bin/ksud ]; then echo KSUD=/data/adb/ksu/bin/ksud; " +
                "elif command -v ksud >/dev/null 2>&1; then echo KSUD=$(command -v ksud); fi; " +
                "if command -v magisk >/dev/null 2>&1; then echo MAGISK=$(command -v magisk); " +
                "elif [ -x /sbin/magisk ]; then echo MAGISK=/sbin/magisk; fi",
            6
        )
        if (response.exitCode != 0 || response.timedOut) return InstallerChoice(
            ModuleTransport.UNAVAILABLE, null, response.error ?: "Modul-Installer antwortet nicht"
        )
        return InstallerOutput.parse(response.output)
    }

    suspend fun install(staged: StagedModule): Result<String> = runCatching {
        require(staged.file.isFile && staged.file.parentFile?.name == "module-import") {
            "Keine überprüfte Modul-ZIP im Staging"
        }
        val path = staged.file.canonicalPath
        require(path.matches(Regex("^/[A-Za-z0-9/_.-]+\\.zip$"))) { "Unsicherer Modulpfad" }
        require(path == staged.file.absolutePath) { "Unerwartete Pfadumleitung" }
        require(staged.file.length() in 1..ZipSafety.MAX_ZIP_BYTES) { "Unerwartete ZIP-Größe" }
        require(shell.probe().state == Verification.VERIFIED) { "Root muss vor Installation gültig sein" }
        val selection = availability()
        require(selection.transport != ModuleTransport.UNAVAILABLE && selection.binary != null) {
            selection.message
        }
        val command = when (selection.transport) {
            ModuleTransport.KERNELSU -> "${selection.binary} module install '$path'"
            ModuleTransport.MAGISK -> "${selection.binary} --install-module '$path'"
            ModuleTransport.UNAVAILABLE -> error("Kein Installer")
        }
        val result = shell.su(command, 90)
        require(!result.timedOut && result.exitCode == 0) {
            result.error ?: result.output.ifBlank { "Installationsruntime meldet einen Fehler" }
        }
        "Von ${selection.transport.title} verarbeitet: ${result.output.takeLast(1800).ifBlank { "Exit 0" }}"
    }
}