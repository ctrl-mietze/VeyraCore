package ctrl.veyracore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * First-party user-authorized root control plane, NOT a kernel exploit or UID0 daemon.
 * Every privileged command is issued only after authorization by the installed su provider.
 * No arbitrary commands are accepted from screens or GitHub source entries.
 */
enum class LoadBackend(val title: String) {
    NONE("Keine Root-Runtime"),
    SU_ONLY("SU-Session"),
    KERNELSU("KernelSU-kompatible Runtime"),
    MAGISK("Magisk-kompatible Runtime")
}

data class VeyraLoadSnapshot(
    val root: Verification = Verification.UNKNOWN,
    val backend: LoadBackend = LoadBackend.NONE,
    val verifiedUid: Int? = null,
    val selinux: String = "Unbekannt",
    val moduleCount: Int? = null,
    val moduleInstaller: Boolean = false,
    val mechanism: String = "Keine eigene Kernel-Root-Engine eingebaut",
    val status: String = "Noch nicht abgefragt"
) {
    val authorized: Boolean get() = root == Verification.VERIFIED && verifiedUid == 0
}

object RuntimeReply {
    fun isRoot(id: ShellResult): Boolean = id.exitCode == 0 && !id.timedOut &&
        Regex("(^|\\s)uid=0(?:\\b|\\()").containsMatchIn(id.output)

    fun parseField(output: String, key: String): String? =
        output.lineSequence().firstOrNull { it.startsWith("$key=") }
            ?.substringAfter('=')
            ?.take(240)
}

/** Veyra Load is a real management layer over a verified existing SU backend. */
class VeyraLoadRuntime(private val shell: SafeShell) : VeyraRuntimeBridge {
    override suspend fun connection(): ProviderAvailability {
        val snapshot = inspect()
        return ProviderAvailability(
            if (snapshot.authorized) Verification.VERIFIED else snapshot.root,
            snapshot.status
        )
    }

    suspend fun inspect(): VeyraLoadSnapshot = withContext(Dispatchers.IO) {
        val identity = shell.su("id", 7)
        if (!RuntimeReply.isRoot(identity)) {
            return@withContext VeyraLoadSnapshot(
                root = if (identity.error != null || identity.timedOut) Verification.ERROR
                    else Verification.NOT_VERIFIED,
                status = identity.error ?: if (identity.timedOut) "su hat nicht rechtzeitig geantwortet"
                    else "Keine autorisierte UID0-Session"
            )
        }
        val inventory = shell.su(
            "if [ -x /data/adb/ksu/bin/ksud ]; then echo 'KSU=1'; else echo 'KSU=0'; fi; " +
                "if command -v magisk >/dev/null 2>&1; then echo 'MAGISK=1'; else echo 'MAGISK=0'; fi; " +
                "if [ -d /data/adb/modules ]; then echo 'MODULES=1'; else echo 'MODULES=0'; fi; " +
                "(getenforce 2>/dev/null || echo unknown) | sed 's/^/SELINUX=/'",
            7
        )
        val ksu = RuntimeReply.parseField(inventory.output, "KSU") == "1"
        val magisk = RuntimeReply.parseField(inventory.output, "MAGISK") == "1"
        val backend = when {
            ksu -> LoadBackend.KERNELSU
            magisk -> LoadBackend.MAGISK
            else -> LoadBackend.SU_ONLY
        }
        val moduleRead = if (RuntimeReply.parseField(inventory.output, "MODULES") == "1")
            shell.modules() else null
        val moduleCount = moduleRead?.getOrNull()?.size
        val selinux = RuntimeReply.parseField(inventory.output, "SELINUX") ?: "Unbekannt"
        VeyraLoadSnapshot(
            root = Verification.VERIFIED,
            backend = backend,
            verifiedUid = 0,
            selinux = selinux,
            moduleCount = moduleCount,
            moduleInstaller = ksu || magisk,
            mechanism = when (backend) {
                LoadBackend.KERNELSU -> "UID0 durch vorhandenes KernelSU; Veyra Load = Control Plane"
                LoadBackend.MAGISK -> "UID0 durch vorhandenes Magisk; Veyra Load = Control Plane"
                LoadBackend.SU_ONLY -> "UID0 durch vorhandenes su; kein eigener Kernel-Exploit"
                LoadBackend.NONE -> "Nicht verfügbar"
            },
            status = "UID 0 bestätigt · ${backend.title}" +
                if (moduleCount == null) " · Modulbestand nicht lesbar"
                else " · $moduleCount Modul(e)"
        )
    }
}