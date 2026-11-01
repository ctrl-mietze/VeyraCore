# VeyraCore 0.1.0 implementation report

## Delivered

- Independent single-module Android application, Kotlin + Jetpack Compose / Material 3.
- Verified package identity: `ctrl.veyracore`; app label `VeyraCore`; version `0.1.0`; min SDK 33, target SDK 35, compile SDK 36.
- OLED (`#000000`, `#0B0A0F`, `#A855F7`) and Berry (`#100C14`, `#1B1520`, `#E83A67`, `#B078EB`) selectable palettes, persisted locally; edge-to-edge system bars and labelled navigation.
- Home, Settings, and Logs are always present. Modules and Superuser appear only after a live UID-0 result from bounded `su -c id`.
- Real device model/manufacturer/Android/kernel/fingerprint display; six setup indicators remain unknown except for evidence actually collected.
- Root probe uses bounded subprocesses. It does not infer root from a path or installed manager.
- Module adapter reads `/data/adb/modules` only through `su` after verification; property files are parsed as text, module IDs are constrained, enable/disable uses only `disable`, and removal is a confirmed next-boot `remove` marker.
- Structured diagnostics persist app-private; export uses `MediaStore.Downloads` without broad storage permission.
- Provider/runtime interfaces and exact profile matcher are ready for a future independently reviewed implementation. The built-in supported profile registry is intentionally empty.
- Documentation and CI debug workflow included. No third-party root source, payload, binary, profile catalog, kernel hook, or driver was copied.

## Verification performed

- `gradlew.bat testDebugUnitTest assembleDebug --no-daemon` passed on 01 Nov 2026 with JDK 17 / Android SDK API 36.
- Five JVM tests passed: three exact-profile matcher cases and two module-ID sanitization cases.
- `aapt dump badging` verified the generated APK reports `package: name='ctrl.veyracore'`, `versionName='0.1.0'`, `sdkVersion:'33'`, and `targetSdkVersion:'35'`.
- Debug APK copied and existence verified: `C:\Users\Mietze\Downloads\VeyraCore-v0.1.0-dev2-debug.apk` (108,159,547 bytes after stable API-36 dependency alignment; SHA-256 `50f0954452e1dfc3b6468c364dfdf9139cd1fa389d6ba768c7c39f24d115ac30`).

## Deliberately not claimed / remaining work

- No GhostLock, DirtyFrag/CVE-2026-43284, Root My Galaxy, or Veyra Load exploit/runtime is integrated.
- No Veyra Load daemon or policy backend exists; Superuser intentionally provides diagnostics only.
- No device-level root/module behavior was exercised on a physical rooted Android device in this workstation build. The APK and JVM logic are verified, while on-device outcomes remain device/provider dependent.
- An original adaptive Veyra launcher icon has now been added, rebuilt, and verified.

The independent source is committed and published in ctrl-mietze/VeyraCore. This remains a development preview, not a production jailbreak release.
