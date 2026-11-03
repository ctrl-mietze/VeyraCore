package ctrl.veyracore

/** Reference catalog, NOT an exploit download list. A repository listing cannot prove a device is exploitable. */
enum class PortGroup(val title: String) {
    SAMSUNG("Samsung / GhostLock"),
    PAYLOADS("Weitere Payload-Feeds"),
    DIRTYFRAG("DirtyFrag / Reroot"),
    GHOSTLOCK("Weitere Geräteports"),
    RESEARCH("Forschung / Referenzen")
}
data class PortSource(
    val title: String,
    val url: String,
    val group: PortGroup,
    /** Source itself has been independently identified; NOT an attestation that it works on this phone. */
    val sourceConfirmed: Boolean
) {
    val id: String get() = url.removePrefix("https://github.com/")
}

/** Public source references, user curated November 2026. Some community repositories may be gone or renamed. */
object RootPortRegistry {
    val all: List<PortSource> = listOf(
        PortSource("Root-My-Galaxy-Payloads", "https://github.com/BuSung-dev/Root-My-Galaxy-Payloads", PortGroup.SAMSUNG, true),
        PortSource("Root-My-Galaxy-Payloads", "https://github.com/rushiranpise/Root-My-Galaxy-Payloads", PortGroup.SAMSUNG, true),
        PortSource("Root-My-Galaxy-Next", "https://github.com/rushiranpise/Root-My-Galaxy-Next", PortGroup.SAMSUNG, true),
        PortSource("Root-My-Galaxy", "https://github.com/BuSung-dev/Root-My-Galaxy", PortGroup.SAMSUNG, true),
        PortSource("GhostSam", "https://github.com/MocLG/GhostSam", PortGroup.SAMSUNG, false),
        PortSource("GhostLock-Galaxy", "https://github.com/wxxsfxyzm/GhostLock-Galaxy", PortGroup.SAMSUNG, false),
        PortSource("root-my-s24", "https://github.com/NanoTurtle1145/root-my-s24", PortGroup.SAMSUNG, false),
        PortSource("ghostlock-s26", "https://github.com/snothin/ghostlock-s26", PortGroup.SAMSUNG, false),
        PortSource("ghostlock-a17", "https://github.com/mobilehackinglab/ghostlock-a17", PortGroup.SAMSUNG, false),
        PortSource("SM-A166W-DZG1-Temporary-Root", "https://github.com/jojobear691/SM-A166W-DZG1-Temporary-Root", PortGroup.SAMSUNG, false),
        PortSource("CVE-2026-43499-S25U", "https://github.com/BuSung-dev/CVE-2026-43499-S25U", PortGroup.SAMSUNG, false),
        PortSource("Root-My-Galaxy-Payloads", "https://github.com/newazbenalam/Root-My-Galaxy-Payloads", PortGroup.SAMSUNG, false),
        PortSource("Root-My-Pixel-Payloads", "https://github.com/alex193a/Root-My-Pixel-Payloads", PortGroup.PAYLOADS, false),
        PortSource("RootMyVivo-Payloads", "https://github.com/zenyxx-xd/RootMyVivo-Payloads", PortGroup.PAYLOADS, false),
        PortSource("Root-My-Device-Payloads", "https://github.com/Witaqua-tools/Root-My-Device-Payloads", PortGroup.PAYLOADS, false),
        PortSource("RootMyVivo-Exploit", "https://github.com/zenyxx-xd/RootMyVivo-Exploit", PortGroup.PAYLOADS, false),
        PortSource("Root-My-Galaxy-Payloads", "https://github.com/gamer765/Root-My-Galaxy-Payloads", PortGroup.PAYLOADS, false),
        PortSource("dirtyfrag", "https://github.com/V4bel/dirtyfrag", PortGroup.DIRTYFRAG, false),
        PortSource("DFRoot", "https://github.com/diabl0w/DFRoot", PortGroup.DIRTYFRAG, true),
        PortSource("DirtyFrag", "https://github.com/mitschud/DirtyFrag", PortGroup.DIRTYFRAG, true),
        PortSource("DFReroot", "https://github.com/polygraphene/DFReroot", PortGroup.DIRTYFRAG, false),
        PortSource("DirtyInit", "https://github.com/combeng6th/DirtyInit", PortGroup.DIRTYFRAG, false),
        PortSource("dirtyfrag-arm64", "https://github.com/linnemanlabs/dirtyfrag-arm64", PortGroup.DIRTYFRAG, false),
        PortSource("DFRoot", "https://github.com/a2333c/DFRoot", PortGroup.DIRTYFRAG, false),
        PortSource("KernelSU", "https://github.com/diabl0w/KernelSU", PortGroup.DIRTYFRAG, false),
        PortSource("Root-My-Device-KSU", "https://github.com/Witaqua-tools/Root-My-Device-KSU", PortGroup.DIRTYFRAG, true),
        PortSource("ghostlock-app", "https://github.com/YuKongA/ghostlock-app", PortGroup.GHOSTLOCK, false),
        PortSource("ghostlock-oneplus", "https://github.com/JoinChang/ghostlock-oneplus", PortGroup.GHOSTLOCK, false),
        PortSource("GhostLock-for-OnePlus", "https://github.com/p2p3p/GhostLock-for-OnePlus", PortGroup.GHOSTLOCK, false),
        PortSource("Root-My-Pixel", "https://github.com/alex193a/Root-My-Pixel", PortGroup.GHOSTLOCK, false),
        PortSource("RootMyVivo", "https://github.com/zenyxx-xd/RootMyVivo", PortGroup.GHOSTLOCK, false),
        PortSource("Root-My-Device", "https://github.com/WitAqua-tools/Root-My-Device", PortGroup.GHOSTLOCK, false),
        PortSource("ghostlock-app", "https://github.com/45072yy980/ghostlock-app", PortGroup.GHOSTLOCK, false),
        PortSource("ghostlock-rmx5085", "https://github.com/Nixbones/ghostlock-rmx5085", PortGroup.GHOSTLOCK, false),
        PortSource("zenfone9-ghostlock", "https://github.com/CKwasd/zenfone9-ghostlock", PortGroup.GHOSTLOCK, false),
        PortSource("ghostlock-x200-root", "https://github.com/xiaohj233/ghostlock-x200-root", PortGroup.GHOSTLOCK, false),
        PortSource("ghostlock-x200-app", "https://github.com/xiaohj233/ghostlock-x200-app", PortGroup.GHOSTLOCK, false),
        PortSource("MT6989-ghostlock", "https://github.com/MaheshSharan/MT6989-ghostlock", PortGroup.GHOSTLOCK, false),
        PortSource("iQOO-Z9_5G-vivo-T3_5G-Root-GhostLock", "https://github.com/ankitrawatgit/iQOO-Z9_5G-vivo-T3_5G-Root-GhostLock", PortGroup.GHOSTLOCK, false),
        PortSource("ghostlock-honor", "https://github.com/MnemosyneLab/ghostlock-honor", PortGroup.GHOSTLOCK, false),
        PortSource("honor-anb-an00-root", "https://github.com/Chuniex-0631/honor-anb-an00-root", PortGroup.GHOSTLOCK, false),
        PortSource("root-my-nothing", "https://github.com/ang3lo-azevedo/root-my-nothing", PortGroup.GHOSTLOCK, false),
        PortSource("ghostlock-emerald", "https://github.com/datfooldive/ghostlock-emerald", PortGroup.GHOSTLOCK, false),
        PortSource("meizu21-ghostlock-root", "https://github.com/ymh001/meizu21-ghostlock-root", PortGroup.GHOSTLOCK, false),
        PortSource("GhostLock", "https://github.com/R0rt1z2/GhostLock", PortGroup.GHOSTLOCK, false),
        PortSource("GhostLock-NVIDIA-Shield-9.2.4", "https://github.com/cyberbalsa/GhostLock-NVIDIA-Shield-9.2.4", PortGroup.GHOSTLOCK, false),
        PortSource("IonStackQuest3", "https://github.com/F-19-F/IonStackQuest3", PortGroup.GHOSTLOCK, false),
        PortSource("ghostlock-xringo1", "https://github.com/ayyy7128/ghostlock-xringo1", PortGroup.GHOSTLOCK, false),
        PortSource("ghostlock-sm-g990b", "https://github.com/k-o-n-t-o-r/ghostlock-sm-g990b", PortGroup.RESEARCH, false),
        PortSource("ghostlock-sabrina", "https://github.com/k-o-n-t-o-r/ghostlock-sabrina", PortGroup.RESEARCH, false),
        PortSource("smt878u-ionstack-poc", "https://github.com/Wtrwx/smt878u-ionstack-poc", PortGroup.RESEARCH, false),
        PortSource("ghostlock-s26", "https://github.com/1ndevelopment/ghostlock-s26", PortGroup.RESEARCH, false),
        PortSource("ghostlock-somalia", "https://github.com/tusharpandey13/ghostlock-somalia", PortGroup.RESEARCH, false),
        PortSource("CVE-2026-43499-popsicle", "https://github.com/x-spy/CVE-2026-43499-popsicle", PortGroup.RESEARCH, false),
        PortSource("CyberMeowfia", "https://github.com/NebuSec/CyberMeowfia", PortGroup.RESEARCH, true),
        PortSource("awesome-temporary-root", "https://github.com/fynks/awesome-temporary-root", PortGroup.RESEARCH, true),
        PortSource("KernelSU", "https://github.com/tiann/KernelSU", PortGroup.RESEARCH, true),
    )
    fun search(query: String, group: PortGroup? = null): List<PortSource> = all.filter {
        (group == null || it.group == group) &&
            (query.isBlank() || it.title.contains(query, true) || it.id.contains(query, true))
    }
}