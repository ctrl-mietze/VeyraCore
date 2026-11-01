# VeyraCore

**VeyraCore** is an independent Android root-management and temporary-root research project by ctrl-mietze.

- Android application ID: `ctrl.veyracore`
- Two visual themes: **Veyra OLED** (true black + violet) and **Veyra Berry** (rose/violet + charcoal).
- Device-first compatibility discovery; exact firmware and kernel profiles must be verified, not guessed.
- The Home / Settings / Logs navigation is always present; Modules and Superuser appear only when verified root access exists.
- Architecture separates Android UI, exploit providers, privileged runtime, and module-management interfaces.
- Root state must be verified on-device. A successful certificate or setup step is **not proof of UID 0**.

## Status

**0.1.0 development foundation.** The Compose app builds as `ctrl.veyracore` (min SDK 33) and can live alongside `ctrl.mietze.veyracore` and `ctrl.mietze.veyraroot`. It provides two persistent dark palettes, real device metadata, a bounded `su -c id` proof, local structured logs/Downloads export, and cautious management of pre-existing `/data/adb/modules` modules after a UID-0 proof.

This is not a root exploit, a boot-image patcher, or a replacement kernel manager. GhostLock, DirtyFrag, Root My Galaxy discovery, Veyra Load, and a policy backend are deliberately shown as unverified/future unless a separately reviewed runtime is integrated.

## Relevant upstream research

- [Root My Galaxy Next](https://github.com/rushiranpise/Root-My-Galaxy-Next) — rooted-device profiles and helper research.
- [DirtyFrag](https://github.com/mitschud/DirtyFrag) — CVE-2026-43284 research and Samsung adaptations.
- [KernelSU](https://github.com/tiann/KernelSU) — SU policy and module compatibility reference.
- [GhostSam](https://github.com/snothin/GhostSam) — Samsung Ghostlock research.

Third-party source and payload binaries are not included in the initial tree; adding any requires verification of provenance, redistributability, device compatibility, and original notices.

## Build

With JDK 17 and Android SDK API 37 installed:

```powershell
$env:ANDROID_HOME = 'C:\Users\Mietze\AppData\Local\Android\Sdk'
.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon
```

The resulting development APK is `app/build/outputs/apk/debug/app-debug.apk`. It is normally debug-signed and must not be presented as a production/rooting release.

See [architecture](docs/ARCHITECTURE.md), [provider status](docs/ROOT_PROVIDER_STATUS.md), [port registry](docs/PORT_REGISTRY.md), and [third-party policy](THIRD_PARTY.md).
