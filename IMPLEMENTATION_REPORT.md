# VeyraCore 0.2.0-dev implementation report

## Delivered

- Version advanced from `0.1.0` to `0.2.0-dev` while retaining application ID `ctrl.veyracore`, min SDK 33, target SDK 35, and compile SDK 36.
- Reworked the Compose UI into a cohesive Material 3 surface with Veyra OLED and Veyra Berry persisted themes, branded adaptive icon resources, readable cards, safe drawing insets, and adaptive bottom/rail navigation.
- Added always-present Home, Root, Settings, and Logs destinations. Modules and Superuser are gated by a current, strict UID-0 result.
- Added detailed device evidence: Android/API, security patch, firmware/build identity, fingerprint, kernel release, KMI, ABI, page size, UID class, SELinux result, and Veyra Load socket probe.
- Added a real provider interface and registry with distinct method IDs, provenance, exact profile matching, and honest availability states. GhostLock, DirtyFrag, and VX Cert / Re-Root are selection/preparation-only in this build.
- Kept the existing safe module marker actions and added bounded SAF ZIP preview plus confirmed `ksud module install` support only when the command help is verified. Installation is re-verified against `/data/adb/modules` before success is reported.
- Added strict UID parsing tests, exact profile tests including kernel/firmware/ABI/page/KMI, provider registry tests, and safe ZIP tests including symlink metadata.
- Updated the offline port catalog, architecture boundary, provider status, README, and third-party provenance policy.

## Verification

The final build/test command, APK identity, size, and SHA-256 are recorded below after the final verified build:

- Command: `gradlew.bat testDebugUnitTest assembleDebug --no-daemon`
- Toolchain: JDK 17, Android SDK API 36, Gradle wrapper distribution 9.5.1.
- Unit tests: see the final Gradle report and the handoff summary.
- Package/application ID: `ctrl.veyracore`.
- Version: `0.2.0-dev`.
- APK: `C:\Users\Mietze\Downloads\VeyraCore-v0.2.0-dev-debug.apk`.
- SHA-256: to be filled from the final copied APK in this working tree.

## Explicit non-claims

- No GhostLock, DirtyFrag, Root My Galaxy, KernelSU driver, kernel module, exploit payload, helper APK, certificate mutation, or Veyra Load daemon was imported.
- No exact device compatibility is claimed for the S25 Ultra, Pixel, or any other model. The registry has no supported exploit profile until all identity dimensions and a reproducible hardware run are verified.
- Superuser is read-only. There is no policy backend and no spoofed grant path.
- A successful `ksud` command is not accepted as module installation success until the module is visible in a fresh, authorized module listing.
- No device was installed or modified automatically by this build task.

## Provenance

The application code in this repository is independently authored. The previous working VeyraCore source and Raspberry Manager source were inspected read-only for architecture and visual direction. No source, binary, private token, keystore, helper identity, or unrelated GPL implementation was copied. See [THIRD_PARTY.md](THIRD_PARTY.md) for attribution and reuse boundaries.
