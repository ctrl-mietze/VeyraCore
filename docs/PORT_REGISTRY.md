# Offline port research registry

Catalog review date: 2026-11-01. These are upstream research links and summaries only. VeyraCore does not fetch them at runtime, does not bundle their payloads, and does not claim that any listed project works on the current device. Exact upstream tables may change; the links are the source of truth for their own projects.

| Project | Official/source link | Documented scope in the upstream material | VeyraCore status / provenance |
|---|---|---|---|
| Root My Galaxy Next | [rushiranpise/Root-My-Galaxy-Next](https://github.com/rushiranpise/Root-My-Galaxy-Next) | Samsung Galaxy installer with explicitly supported model and kernel combinations. | Research only; no feed, payload, helper, or source imported. |
| DirtyFrag | [mitschud/DirtyFrag](https://github.com/mitschud/DirtyFrag) | Temporary-root research and KMI-specific artifacts around DirtyFrag/CVE-2026-43284. | Blocked in Veyra; no CVE implementation, binaries, or module imported. |
| KernelSU | [tiann/KernelSU](https://github.com/tiann/KernelSU) | Upstream kernel/user-space root interoperability and module concepts. | Attribution/interoperability reference only; no KernelSU source or driver imported. |
| GhostSam | [snothin/GhostSam](https://github.com/snothin/GhostSam) | Samsung GhostLock/DirtyFrag research, device series, and payload architecture. | Research only; no payloads, prebuilt `ksud`, or source imported. |
| GhostLock-Galaxy | [wxxsfxyzm/GhostLock-Galaxy](https://github.com/wxxsfxyzm/GhostLock-Galaxy) | Galaxy Z Fold6 exact kernel profiles and shell/Shizuku execution route. | Research only; no exact Veyra profile or runtime integration. |
| ghostlock-a17 | [mobilehackinglab/ghostlock-a17](https://github.com/mobilehackinglab/ghostlock-a17) | Galaxy A17 SM-A175F Android 16/GKI 6.12 exact-target research. | Research only; no target offsets or exploit code imported. |
| Root My Pixel | [alex193a/Root-My-Pixel](https://github.com/alex193a/Root-My-Pixel) | Pixel temporary-root application and exact build/payload research. | Research only; no Pixel compatibility claim. |
| GhostLock-oneplus | [JoinChang/ghostlock-oneplus](https://github.com/JoinChang/ghostlock-oneplus) | OnePlus/OPPO/Xiaomi GhostLock targets with documented kernel/build tables. | Research only; no payload, offsets, or device claim. |
| Root My Device | [tqmane/Root-My-Device](https://github.com/tqmane/Root-My-Device) | Separate exact-build Nothing Phone (3a) and OnePlus Pad 3 application sets. | Research only; no universal port implied and no code imported. |
| Ghostlock-5.10 | [CrackerCat/GhostLock-5.10](https://github.com/CrackerCat/GhostLock-5.10) | Amazon Fire 5.10 device-specific GhostLock research. | Research only; no Fire/5.10 support claim. |
| RootMyVivo | [zenyxx-xd/RootMyVivo](https://github.com/zenyxx-xd/RootMyVivo) | vivo/iQOO temporary-root application and payload-catalog research. | Research only; no vivo/iQOO support claim and no runtime downloads. |

## What an actual Veyra port would require

An integration would need a provenance record, license/notice review, exact firmware/build identity, complete kernel/KMI match, ABI and page-size match, artifact hashes, a tested execution context, a safe failure path, and reproducible verification on the target device. A model family or kernel major version alone is never enough. Until then, catalog entries remain research-only.

## Veyra endpoints

| Surface | Current endpoint | State |
|---|---|---|
| Android application | `ctrl.veyracore` | Implemented; v0.2.0-dev |
| Root evidence | `su -c id` | Bounded live proof; no spoofing |
| Veyra Load socket | `/dev/socket/veyra_loadd` | Probe-only; no daemon shipped |
| Existing module directory | `/data/adb/modules` | Read/marker adapter after UID-0 proof |
| ZIP install | `ksud module install <cache ZIP>` | Disabled unless command help is verified; post-install list verification required |
