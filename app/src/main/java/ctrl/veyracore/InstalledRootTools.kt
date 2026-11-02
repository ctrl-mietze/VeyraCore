package ctrl.veyracore

import android.content.Context
import android.content.Intent

/** User-controlled handoff to a real installed root provider, not a fake exploit runner. */
data class InstalledRootTool(
    val id: String,
    val title: String,
    val packageName: String,
    val description: String
)

object InstalledRootTools {
    val known = listOf(
        InstalledRootTool("veyra-root", "Veyra Root", "ctrl.mietze.veyraroot",
            "Vorhandene Veyra-Root-App mit eigenem Firmware-/Payload-Ablauf."),
        InstalledRootTool("legacy-core", "VeyraCore (bisherige Version)", "ctrl.mietze.veyracore",
            "Bestehende Legacy-App mit VX-Core-Cert- und Helper-Setup."),
        InstalledRootTool("zf6-root", "ZF6Root", "de.veyracore.zf6root",
            "Spezifischer Samsung Galaxy Z Fold6 Root-Port."),
        InstalledRootTool("rmgn", "Root My Galaxy Next", "dev.rushiranpise.rmgnext",
            "RMG Next: modell- und firmwareabhängige Root-/Zertifikat-Integration."),
        InstalledRootTool("rmg", "Root My Galaxy", "io.github.rootmygalaxy.s23ultra",
            "Samsung Root-Engine mit gerätespezifischem Payload-Katalog."),
        InstalledRootTool("s25-legacy", "Galaxy Root Engine", "dev.busung.s25uroot",
            "Vorhandene Geräte-App. Exploit und unterstützte Firmware werden dort geprüft.")
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
            ?: error("Diese App ist nicht installiert oder besitzt keinen Launcher")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** A recommendation needs both an exact published profile and an installed provider. */
    fun recommended(context: Context, device: DeviceInfo, matchingProfiles: Int): InstalledRootTool? {
        if (matchingProfiles <= 0) return null
        val ordered = when {
            device.model.startsWith("SM-F956", ignoreCase = true) ->
                listOf("zf6-root", "veyra-root", "legacy-core", "rmgn", "rmg")
            device.manufacturer.equals("samsung", true) ->
                listOf("veyra-root", "legacy-core", "rmgn", "rmg", "s25-legacy")
            else -> listOf("veyra-root", "rmgn")
        }
        return ordered.asSequence().mapNotNull { wanted -> known.find { it.id == wanted } }
            .firstOrNull { isInstalled(context, it) }
    }
}