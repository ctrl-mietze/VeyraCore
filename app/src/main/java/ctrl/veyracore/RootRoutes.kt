package ctrl.veyracore

/** Research sources are metadata, never executable payloads. */
enum class RouteState { VERIFIED_EXISTING, REQUIRES_PROVIDER, REQUIRES_EXACT_PROFILE }

data class RootRoute(
    val id: String,
    val name: String,
    val subtitle: String,
    val description: String,
    val upstream: String,
    val state: RouteState,
    val canStart: Boolean = false
)

object RootRouteCatalog {
    fun routes(device: DeviceInfo, probe: RootProbe): List<RootRoute> = listOf(
        RootRoute(
            "existing-su", "Existing Root", "Vorhandene privilegierte Sitzung",
            if (probe.state == Verification.VERIFIED)
                "UID 0 wurde auf diesem Gerät nachgewiesen. Module lassen sich jetzt über eine kompatible Runtime verwalten."
            else "Kein aktueller UID-0-Nachweis. Mit „Root prüfen“ wird vorhandener Zugriff sicher erkannt.",
            "https://github.com/tiann/KernelSU",
            if (probe.state == Verification.VERIFIED) RouteState.VERIFIED_EXISTING else RouteState.REQUIRES_PROVIDER
        ),
        RootRoute(
            "ghostlock", "GhostLock", "Gerätespezifische Kernelroute",
            "Nur exakt geprüfte Modell-, Firmware- und Kernelprofile sind zulässig. Kein portierter Exploit oder Treiber in dieser App.",
            "https://github.com/fynks/awesome-temporary-root",
            RouteState.REQUIRES_EXACT_PROFILE
        ),
        RootRoute(
            "dirtyfrag", "DirtyFrag", "CVE-2026-43284 · temporäre Rootroute",
            "KMI allein beweist keine Verwundbarkeit. Herstellerhärtungen und konkrete Firmware müssen getrennt validiert werden.",
            "https://github.com/mitschud/DirtyFrag",
            RouteState.REQUIRES_EXACT_PROFILE
        ),
        RootRoute(
            "reroot", "Veyra Reroot", "Zertifikat / Helper / Soft Restart",
            "System-UID 1000 und Zertifikatsannahme sind kein UID-0-Nachweis. Die eigenständige Veyra-Load-Bridge ist noch nicht integriert.",
            "https://github.com/rushiranpise/Root-My-Galaxy-Next",
            RouteState.REQUIRES_PROVIDER
        )
    )
}


/** Public research links. Listing one never means the currently selected device is supported. */
data class ResearchPort(val label: String, val target: String, val url: String)
object RootResearchIndex {
    val ports: List<ResearchPort> = listOf(
        ResearchPort("Root My Galaxy Next", "Samsung Galaxy – genaue Builds", "https://github.com/rushiranpise/Root-My-Galaxy-Next"),
        ResearchPort("GhostSam", "Samsung Galaxy S22 / S26 / Z Fold8", "https://github.com/snothin/GhostSam"),
        ResearchPort("GhostLock-Galaxy", "Samsung Galaxy Z Fold6", "https://github.com/wxxsfxyzm/GhostLock-Galaxy"),
        ResearchPort("ghostlock-a17", "Samsung Galaxy A17", "https://github.com/mobilehackinglab/ghostlock-a17"),
        ResearchPort("Samsung SM-G990B", "Samsung Galaxy S21 FE, profilgebunden", "https://github.com/k-o-n-t-o-r/ghostlock-sm-g990b"),
        ResearchPort("ghostlock-oneplus", "OnePlus, OPPO und Xiaomi", "https://github.com/JoinChang/ghostlock-oneplus"),
        ResearchPort("GhostLock-for-OnePlus", "OnePlus Ace 6T / OnePlus 15", "https://github.com/p2p3p/GhostLock-for-OnePlus"),
        ResearchPort("ghostlock-emerald", "POCO M6 Pro 4G", "https://github.com/datfooldive/ghostlock-emerald"),
        ResearchPort("zenfone9-ghostlock", "ASUS Zenfone 9", "https://github.com/CKwasd/zenfone9-ghostlock"),
        ResearchPort("meizu21-ghostlock-root", "Meizu 21", "https://github.com/ymh001/meizu21-ghostlock-root"),
        ResearchPort("GhostLock-5.10", "Amazon Fire Max 11 / Fire TV", "https://github.com/CrackerCat/GhostLock-5.10"),
        ResearchPort("DirtyFrag", "Kernel- und KMI-spezifische Forschung", "https://github.com/mitschud/DirtyFrag"),
        ResearchPort("KernelSU module reference", "Modulformat und SU-Architektur", "https://github.com/tiann/KernelSU"),
        ResearchPort("Temporary Root index", "Weitere öffentliche Ports und Profile", "https://github.com/fynks/awesome-temporary-root")
    )
}
