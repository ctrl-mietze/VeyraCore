# Third-party notice and source policy

VeyraCore v0.2.0-dev contains no copied native source, binaries, payloads, kernel modules, or driver code from the projects below. Public documentation is referenced for interoperability and development research only. The built-in source catalog is an original, non-executable index. Module installation delegates only to a compatible, pre-existing runtime after consent.

| Project | Upstream | Published license observed | v0.2.0-dev use |
|---|---|---|---|
| DirtyFrag | https://github.com/mitschud/DirtyFrag | GPL-2.0 | No code or artifacts imported. Any future reuse requires GPL compatibility review and notices. |
| KernelSU | https://github.com/tiann/KernelSU | GPL-3.0 | No code or artifacts imported. VeyraCore does not use its kernel hooks. |
| Root My Galaxy Next | https://github.com/rushiranpise/Root-My-Galaxy-Next | Upstream license page must be rechecked before import | No code, data, or payload imported. |
| GhostSam | https://github.com/snothin/GhostSam | Apache-2.0 | No code or artifacts imported. Any future reuse requires NOTICE/license preservation. |

This repository's new source is independently authored. External claims about a device, kernel, CVE, or compatibility are never converted into a VeyraCore support claim without reproducible, device-specific review.

## Eigenständige VeyraCore-Entwicklung 0.3.0-dev

Die 57 Links in docs/PORT_REGISTRY.md sind öffentliche Referenzen; die neue Anwendung enthält keine von dort kopierten nativen Exploit-Binaries, umbenannten proprietären Treiber oder KernelSU-Kernel-Patches. Sie kann vorhandene fremde Root-Laufzeiten für Module verwenden.

Upstream-Referenzen: BuSung-dev / Root My Galaxy und Payloads, rushiranpise / Root My Galaxy Next und Payloads, mitschud / DirtyFrag, tiann / KernelSU. Bei einer späteren tatsächlichen Übernahme gelten die Originallizenzen und Urhebervermerke der jeweiligen Projekte.

## 0.4.0-dev

Veyra Load (Kotlin-Kontrollschicht) und der Helper-APK-Updatepfad sind eigenständige Android-App-Komponenten. KernelSU und Magisk bleiben externe Root-Runtimes und Modulinstaller; ihre Namen dienen der transparenten Kompatibilitätsanzeige. Aus KernelSU, Root My Galaxy Next oder DirtyFrag wurden keine nativen Kernel-Treiber, Exploit-Dateien oder Systemsignatur-Injektionskomponenten in diese Entwicklungsstufe übernommen. Originalquellen und Lizenzreferenzen stehen im Port-Katalog.

## Veyra Root metadata import — 0.5.0-dev

Data copied from Raphael's Veyra Root repository (https://github.com/ctrl-mietze/Veyra) under its published Apache 2.0 license: its local DF and Pixel targets-v3.json manifests, containing model, kernel and metadata records. The new VeyraProfileEngine.kt reimplements parts of Veyra Root's SupportManifest matching concept without copying native exploit libraries, .ko modules or daemon binaries. The underlying Veyra Root and upstream RMG/KSU creators retain their own credits, license notices and authorship. No third-party kernel drivers are included in this build.