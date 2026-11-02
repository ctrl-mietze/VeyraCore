# VeyraCore — Quellen- und Payload-Katalog

Stand: 2. November 2026 · VeyraCore 0.3.0-dev

**57 kuratierte GitHub-Links sind nicht 57 bestätigte Root-Exploits.** Ein Fork, Forschungs-PoC, Payload-Manifest oder eine andere Android-App ist nicht gleichbedeutend mit einer auf dem lokalen Gerät ausführbaren Root-Engine.

Nur die bekannten JSON-Support-Feeds von BuSung und rushiranpise werden als Metadaten gelesen. Beliebige Repository-Binärdateien werden nicht heruntergeladen oder ausgeführt. Eine manuelle Quellenwahl führt niemals unmittelbar einen Kernel-Payload aus.

### Geräteauswahl

- Exakte Modellbezeichnung Build.MODEL und dreiteilige Kernel-Version aus den bekannten targets-v3.json-Feeds; optional Firmware-Fingerprint.
- Bereits erlaubte UID0-Sitzung hat Vorrang; andernfalls können installierte externe Root-Provider geöffnet werden.
- Automatische Empfehlung und manuelle Quellenwahl sind separat; Geräteprüfung lässt sich für die Recherche ausschalten.
- Ein Profil-Treffer beweist nicht, dass eine Payload mit der individuellen Firmware wirklich funktioniert.

## Referenzen

### Samsung / GhostLock

| Nr. | Repository | Link | Status |
|---:|---|---|---|
| 1 | Root-My-Galaxy-Payloads | [BuSung-dev/Root-My-Galaxy-Payloads](https://github.com/BuSung-dev/Root-My-Galaxy-Payloads) | Quelle erfasst; Gerät nicht bestätigt |
| 2 | Root-My-Galaxy-Payloads | [rushiranpise/Root-My-Galaxy-Payloads](https://github.com/rushiranpise/Root-My-Galaxy-Payloads) | Quelle erfasst; Gerät nicht bestätigt |
| 3 | Root-My-Galaxy-Next | [rushiranpise/Root-My-Galaxy-Next](https://github.com/rushiranpise/Root-My-Galaxy-Next) | Quelle erfasst; Gerät nicht bestätigt |
| 4 | Root-My-Galaxy | [BuSung-dev/Root-My-Galaxy](https://github.com/BuSung-dev/Root-My-Galaxy) | Quelle erfasst; Gerät nicht bestätigt |
| 5 | GhostSam | [MocLG/GhostSam](https://github.com/MocLG/GhostSam) | Community-Link; Geräteprüfung ausstehend |
| 6 | GhostLock-Galaxy | [wxxsfxyzm/GhostLock-Galaxy](https://github.com/wxxsfxyzm/GhostLock-Galaxy) | Community-Link; Geräteprüfung ausstehend |
| 7 | root-my-s24 | [NanoTurtle1145/root-my-s24](https://github.com/NanoTurtle1145/root-my-s24) | Community-Link; Geräteprüfung ausstehend |
| 8 | ghostlock-s26 | [snothin/ghostlock-s26](https://github.com/snothin/ghostlock-s26) | Community-Link; Geräteprüfung ausstehend |
| 9 | ghostlock-a17 | [mobilehackinglab/ghostlock-a17](https://github.com/mobilehackinglab/ghostlock-a17) | Community-Link; Geräteprüfung ausstehend |
| 10 | SM-A166W-DZG1-Temporary-Root | [jojobear691/SM-A166W-DZG1-Temporary-Root](https://github.com/jojobear691/SM-A166W-DZG1-Temporary-Root) | Community-Link; Geräteprüfung ausstehend |
| 11 | CVE-2026-43499-S25U | [BuSung-dev/CVE-2026-43499-S25U](https://github.com/BuSung-dev/CVE-2026-43499-S25U) | Community-Link; Geräteprüfung ausstehend |
| 12 | Root-My-Galaxy-Payloads | [newazbenalam/Root-My-Galaxy-Payloads](https://github.com/newazbenalam/Root-My-Galaxy-Payloads) | Community-Link; Geräteprüfung ausstehend |

### Weitere Payload-Kataloge

| Nr. | Repository | Link | Status |
|---:|---|---|---|
| 13 | Root-My-Pixel-Payloads | [alex193a/Root-My-Pixel-Payloads](https://github.com/alex193a/Root-My-Pixel-Payloads) | Community-Link; Geräteprüfung ausstehend |
| 14 | RootMyVivo-Payloads | [zenyxx-xd/RootMyVivo-Payloads](https://github.com/zenyxx-xd/RootMyVivo-Payloads) | Community-Link; Geräteprüfung ausstehend |
| 15 | Root-My-Device-Payloads | [Witaqua-tools/Root-My-Device-Payloads](https://github.com/Witaqua-tools/Root-My-Device-Payloads) | Community-Link; Geräteprüfung ausstehend |
| 16 | RootMyVivo-Exploit | [zenyxx-xd/RootMyVivo-Exploit](https://github.com/zenyxx-xd/RootMyVivo-Exploit) | Community-Link; Geräteprüfung ausstehend |
| 17 | Root-My-Galaxy-Payloads | [gamer765/Root-My-Galaxy-Payloads](https://github.com/gamer765/Root-My-Galaxy-Payloads) | Community-Link; Geräteprüfung ausstehend |

### DirtyFrag und Reroot

| Nr. | Repository | Link | Status |
|---:|---|---|---|
| 18 | dirtyfrag | [V4bel/dirtyfrag](https://github.com/V4bel/dirtyfrag) | Community-Link; Geräteprüfung ausstehend |
| 19 | DFRoot | [diabl0w/DFRoot](https://github.com/diabl0w/DFRoot) | Quelle erfasst; Gerät nicht bestätigt |
| 20 | DirtyFrag | [mitschud/DirtyFrag](https://github.com/mitschud/DirtyFrag) | Quelle erfasst; Gerät nicht bestätigt |
| 21 | DFReroot | [polygraphene/DFReroot](https://github.com/polygraphene/DFReroot) | Community-Link; Geräteprüfung ausstehend |
| 22 | DirtyInit | [combeng6th/DirtyInit](https://github.com/combeng6th/DirtyInit) | Community-Link; Geräteprüfung ausstehend |
| 23 | dirtyfrag-arm64 | [linnemanlabs/dirtyfrag-arm64](https://github.com/linnemanlabs/dirtyfrag-arm64) | Community-Link; Geräteprüfung ausstehend |
| 24 | DFRoot | [a2333c/DFRoot](https://github.com/a2333c/DFRoot) | Community-Link; Geräteprüfung ausstehend |
| 25 | KernelSU | [diabl0w/KernelSU](https://github.com/diabl0w/KernelSU) | Community-Link; Geräteprüfung ausstehend |
| 26 | Root-My-Device-KSU | [Witaqua-tools/Root-My-Device-KSU](https://github.com/Witaqua-tools/Root-My-Device-KSU) | Quelle erfasst; Gerät nicht bestätigt |

### Weitere GhostLock-Ports

| Nr. | Repository | Link | Status |
|---:|---|---|---|
| 27 | ghostlock-app | [YuKongA/ghostlock-app](https://github.com/YuKongA/ghostlock-app) | Community-Link; Geräteprüfung ausstehend |
| 28 | ghostlock-oneplus | [JoinChang/ghostlock-oneplus](https://github.com/JoinChang/ghostlock-oneplus) | Community-Link; Geräteprüfung ausstehend |
| 29 | GhostLock-for-OnePlus | [p2p3p/GhostLock-for-OnePlus](https://github.com/p2p3p/GhostLock-for-OnePlus) | Community-Link; Geräteprüfung ausstehend |
| 30 | Root-My-Pixel | [alex193a/Root-My-Pixel](https://github.com/alex193a/Root-My-Pixel) | Community-Link; Geräteprüfung ausstehend |
| 31 | RootMyVivo | [zenyxx-xd/RootMyVivo](https://github.com/zenyxx-xd/RootMyVivo) | Community-Link; Geräteprüfung ausstehend |
| 32 | Root-My-Device | [WitAqua-tools/Root-My-Device](https://github.com/WitAqua-tools/Root-My-Device) | Community-Link; Geräteprüfung ausstehend |
| 33 | ghostlock-app | [45072yy980/ghostlock-app](https://github.com/45072yy980/ghostlock-app) | Community-Link; Geräteprüfung ausstehend |
| 34 | ghostlock-rmx5085 | [Nixbones/ghostlock-rmx5085](https://github.com/Nixbones/ghostlock-rmx5085) | Community-Link; Geräteprüfung ausstehend |
| 35 | zenfone9-ghostlock | [CKwasd/zenfone9-ghostlock](https://github.com/CKwasd/zenfone9-ghostlock) | Community-Link; Geräteprüfung ausstehend |
| 36 | ghostlock-x200-root | [xiaohj233/ghostlock-x200-root](https://github.com/xiaohj233/ghostlock-x200-root) | Community-Link; Geräteprüfung ausstehend |
| 37 | ghostlock-x200-app | [xiaohj233/ghostlock-x200-app](https://github.com/xiaohj233/ghostlock-x200-app) | Community-Link; Geräteprüfung ausstehend |
| 38 | MT6989-ghostlock | [MaheshSharan/MT6989-ghostlock](https://github.com/MaheshSharan/MT6989-ghostlock) | Community-Link; Geräteprüfung ausstehend |
| 39 | iQOO-Z9_5G-vivo-T3_5G-Root-GhostLock | [ankitrawatgit/iQOO-Z9_5G-vivo-T3_5G-Root-GhostLock](https://github.com/ankitrawatgit/iQOO-Z9_5G-vivo-T3_5G-Root-GhostLock) | Community-Link; Geräteprüfung ausstehend |
| 40 | ghostlock-honor | [MnemosyneLab/ghostlock-honor](https://github.com/MnemosyneLab/ghostlock-honor) | Community-Link; Geräteprüfung ausstehend |
| 41 | honor-anb-an00-root | [Chuniex-0631/honor-anb-an00-root](https://github.com/Chuniex-0631/honor-anb-an00-root) | Community-Link; Geräteprüfung ausstehend |
| 42 | root-my-nothing | [ang3lo-azevedo/root-my-nothing](https://github.com/ang3lo-azevedo/root-my-nothing) | Community-Link; Geräteprüfung ausstehend |
| 43 | ghostlock-emerald | [datfooldive/ghostlock-emerald](https://github.com/datfooldive/ghostlock-emerald) | Community-Link; Geräteprüfung ausstehend |
| 44 | meizu21-ghostlock-root | [ymh001/meizu21-ghostlock-root](https://github.com/ymh001/meizu21-ghostlock-root) | Community-Link; Geräteprüfung ausstehend |
| 45 | GhostLock | [R0rt1z2/GhostLock](https://github.com/R0rt1z2/GhostLock) | Community-Link; Geräteprüfung ausstehend |
| 46 | GhostLock-NVIDIA-Shield-9.2.4 | [cyberbalsa/GhostLock-NVIDIA-Shield-9.2.4](https://github.com/cyberbalsa/GhostLock-NVIDIA-Shield-9.2.4) | Community-Link; Geräteprüfung ausstehend |
| 47 | IonStackQuest3 | [F-19-F/IonStackQuest3](https://github.com/F-19-F/IonStackQuest3) | Community-Link; Geräteprüfung ausstehend |
| 48 | ghostlock-xringo1 | [ayyy7128/ghostlock-xringo1](https://github.com/ayyy7128/ghostlock-xringo1) | Community-Link; Geräteprüfung ausstehend |

### Forschung / Referenzen

| Nr. | Repository | Link | Status |
|---:|---|---|---|
| 49 | ghostlock-sm-g990b | [k-o-n-t-o-r/ghostlock-sm-g990b](https://github.com/k-o-n-t-o-r/ghostlock-sm-g990b) | Community-Link; Geräteprüfung ausstehend |
| 50 | ghostlock-sabrina | [k-o-n-t-o-r/ghostlock-sabrina](https://github.com/k-o-n-t-o-r/ghostlock-sabrina) | Community-Link; Geräteprüfung ausstehend |
| 51 | smt878u-ionstack-poc | [Wtrwx/smt878u-ionstack-poc](https://github.com/Wtrwx/smt878u-ionstack-poc) | Community-Link; Geräteprüfung ausstehend |
| 52 | ghostlock-s26 | [1ndevelopment/ghostlock-s26](https://github.com/1ndevelopment/ghostlock-s26) | Community-Link; Geräteprüfung ausstehend |
| 53 | ghostlock-somalia | [tusharpandey13/ghostlock-somalia](https://github.com/tusharpandey13/ghostlock-somalia) | Community-Link; Geräteprüfung ausstehend |
| 54 | CVE-2026-43499-popsicle | [x-spy/CVE-2026-43499-popsicle](https://github.com/x-spy/CVE-2026-43499-popsicle) | Community-Link; Geräteprüfung ausstehend |
| 55 | CyberMeowfia | [NebuSec/CyberMeowfia](https://github.com/NebuSec/CyberMeowfia) | Quelle erfasst; Gerät nicht bestätigt |
| 56 | awesome-temporary-root | [fynks/awesome-temporary-root](https://github.com/fynks/awesome-temporary-root) | Quelle erfasst; Gerät nicht bestätigt |
| 57 | KernelSU | [tiann/KernelSU](https://github.com/tiann/KernelSU) | Quelle erfasst; Gerät nicht bestätigt |

## Support-Metadaten-Feeds

- [Original BuSung](https://raw.githubusercontent.com/BuSung-dev/Root-My-Galaxy-Payloads/main/support/targets-v3.json)
- [Root My Galaxy Next](https://raw.githubusercontent.com/rushiranpise/Root-My-Galaxy-Payloads/main/support/targets-v3.json)

Nur diese beiden festen HTTPS-Adressen werden angefragt. Der Parser limitiert auf 1MB je Feed, 2048 Einträge, begrenzte Anzahl Geräte und Kernel pro Eintrag; speichert den letzten erfolgreichen Stand lokal. Er lädt keine ausführbaren Exploit- oder Kernel-Dateien.