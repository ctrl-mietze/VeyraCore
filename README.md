# VeyraCore 0.2.0-dev

VeyraCore is an independent Android application for transparent root-state verification, safe diagnostics, and device-specific root research. It is authored under the `ctrl.veyracore` package and is not a repackaged copy of the earlier VeyraCore application.

The 0.2 line is deliberately conservative: a visible UID-0 proof is required before privileged surfaces appear, research projects are catalogued without claiming compatibility, and no exploit payload, kernel driver, certificate mutation, boot-image patcher, or unknown binary download is shipped.

## What is in the app

- Material 3 Compose UI with two persisted palettes:
  - **Veyra OLED** — true black with violet/lilac accents.
  - **Veyra Berry** — rose/magenta/violet accents over a restrained charcoal surface.
- Adaptive navigation for phones, landscape, and wide screens. Home, Root, Settings, and Logs are always available. Modules and Superuser appear only after a live UID-0 proof.
- Home and Root show actual Android, firmware, fingerprint, kernel, KMI, ABI, page-size, SELinux, UID, and runtime evidence where available.
- Root provider registry with distinct method IDs, provenance, exact profile matching, and the states `unavailable`, `supported`, `blocked`, and `requires external runtime`.
- Read-only Superuser evidence. UID 0, UID 1000/system, and UID 2000/ADB shell are displayed as different identities; settings cannot manufacture a grant.
- Existing safe module marker actions: enable/disable through `disable`, and confirmed next-boot removal through `remove`.
- Optional SAF ZIP preview. The verifier bounds archive size, checks relative paths and duplicate entries, reads `module.prop` as data, rejects symlink metadata, and never extracts or executes the archive. Installation is enabled only when `ksud module install --help` is verified through the authorized root backend; the result is checked against the module list before success is shown.
- Structured local logs with export to `MediaStore.Downloads` without broad storage permission.

## Root-provider boundary

VeyraCore does not claim that a device is compatible because it is a Galaxy, Pixel, S25 Ultra, or because a project mentions the same kernel family. A future supported profile must match the exact manufacturer, model, firmware/build identity, fingerprint, complete kernel release/KMI, ABI, and page size. The registry is intentionally empty of exploit profiles in this development build.

GhostLock, DirtyFrag, and VX Cert / Re-Root are visible as separate research methods. Their cards support safe selection and a non-running verification plan only. No method card starts an exploit, downloads a payload, edits certificate state, flashes a partition, or changes a policy database.

The old working VeyraCore source was reviewed read-only for architecture and UX context: `VeyraCoreActivity.kt` coordinated the screen state, `InstallViewModel.kt` represented a staged install state machine, and `DfrInstall.kt` described an external `app_process` helper boundary. Those ideas are referenced in [ARCHITECTURE.md](docs/ARCHITECTURE.md); source, helper identities, payloads, binaries, and certificate logic were not copied into this repository.

## Research catalog

[PORT_REGISTRY.md](docs/PORT_REGISTRY.md) is an offline catalog of the requested upstream research projects. It records source links, the documented scope, and VeyraCore's non-integration status. It is not a compatibility list and it is not fetched at runtime.

## Build

Use JDK 17 and the local Android SDK API 36 platform. On Windows, set the SDK path for the shell if it is not already configured:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
.\gradlew.bat testDebugUnitTest assembleDebug --no-daemon
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. The assignment handoff copies it to `C:\Users\Mietze\Downloads\VeyraCore-v0.2.0-dev-debug.apk` and verifies its package identity and SHA-256. Do not install it onto a device automatically.

## Project status

This is a development preview, not a production root release. The APK can manage diagnostics and already-authorized module markers only when the device itself supplies the required proof. No physical device/provider run is implied by a successful workstation build.

See [IMPLEMENTATION_REPORT.md](IMPLEMENTATION_REPORT.md), [ROOT_PROVIDER_STATUS.md](docs/ROOT_PROVIDER_STATUS.md), [ARCHITECTURE.md](docs/ARCHITECTURE.md), and [THIRD_PARTY.md](THIRD_PARTY.md).

See also [Veyra visual language](docs/DESIGN_SYSTEM.md) for the exact OLED/Berry semantic palette, mobile/landscape layout, and evidence-to-color rules.
