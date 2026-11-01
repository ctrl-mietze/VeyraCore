package ctrl.veyracore

import android.content.Context
import android.content.Intent

/**
 * Explicit hand-off to already installed, user-selected root applications.
 * Launching another app does not grant root and is never represented as success.
 * No exploit binaries, embedded drivers or privileged commands live here.
 */
data class InstalledRootTool(
    val id: String,
    val title: String,
    val packageName: String,
    val description: String
)

object InstalledRootTools {
    val known: List<InstalledRootTool> = listOf(
        InstalledRootTool(
            "veyra-root", "Veyra Root", "ctrl.mietze.veyraroot",
            "Bestehende Veyra-Root-App. Gerät und Firmware vor jedem Start dort prüfen."
        ),
        InstalledRootTool(
            "zf6-root", "ZF6Root", "de.veyracore.zf6root",
            "Samsung-Port mit eigener Geräteprüfung. Ein installiertes Tool belegt keine Kompatibilität."
        ),
        InstalledRootTool(
            "rmgn", "Root My Galaxy Next", "dev.rushiranpise.rmgnext",
            "Eigenständige Referenz-App für gerätespezifische Root- und Helper-Abläufe."
        )
    )

    fun isAllowlisted(tool: InstalledRootTool): Boolean =
        known.any { it.id == tool.id && it.packageName == tool.packageName }

    fun isInstalled(context: Context, tool: InstalledRootTool): Boolean =
        isAllowlisted(tool) &&
            runCatching {
                context.packageManager.getLaunchIntentForPackage(tool.packageName) != null
            }.getOrDefault(false)

    fun open(context: Context, tool: InstalledRootTool): Result<Unit> = runCatching {
        require(isAllowlisted(tool)) { "Nicht freigegebener Provider" }
        val intent = context.packageManager.getLaunchIntentForPackage(tool.packageName)
            ?: error("Die App ist nicht installiert oder besitzt keinen Launcher")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
