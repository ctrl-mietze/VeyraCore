# VeyraCore v0.3.0-dev — Implementation and verification report

Updated: 2026-11-02

## Starting point

- Independent GitHub repository: ctrl-mietze/VeyraCore
- Independent applicationId: ctrl.veyracore (not an update to legacy ctrl.mietze.veyracore)
- Original baseline: 0.2.1-dev, default branch main
- This iteration preserves preexisting Material 3 app, OLED/Berry theme, original root probe, module UI, ZIP safety checker, logs/export and conditional Superuser/Modules navigation.

## Actual working source changes

1. RootPortRegistry.kt — 57 GitHub source links, grouped, searchable, filterable; choose a manual reference. A URL does not imply firmware compatibility or executable payload availability.
2. PayloadFeeds.kt — strict bounded HTTPS JSON catalog reader for the original and RMG Next Root-My-Galaxy-Payloads schema 3 manifests. Validates IDs and model/kernel arrays, caches last good metadata, handles unreachable feeds. Does NOT download native exploit files.
3. Core.kt — persisted Automatic Selection, Device Verification and selected source preferences; original UID 0 root shell remains.
4. InstalledRootTools.kt — launch actual installed Veyra Root, VeyraCore (legacy), ZF6Root, Root My Galaxy Next and original Root My Galaxy; return to VeyraCore and independently check UID 0. Provider recommendation is model-/kernel-informed but cannot prove the underlying exploit will succeed.
5. VeyraHelperStatus.kt — interrogates Android PackageManager application UID for the approved installed helpers. Observed on the user's Samsung: ctrl.mietze.veyracore.helper = UID 1000. This evidence is not equal to root UID 0 and not an independent signature history dump.
6. Dedicated local Foreground Service with notification channel and Android notification permission; shows ongoing helper status only after UID1000 evidence and opt-in state, restores after BOOT_COMPLETED where Android allows. Handles exceptions instead of claiming guaranteed persistence on all versions.
7. MainActivity.kt — working Setup Steps 01 Root erhalten -> Cert -> Helper -> Soft reboot evidence separate -> Notification, source selector, provider launcher, separate diagnostics and Settings toggles.
8. Tests — RootCatalogAndMatchingTest.kt with catalog count, category, kernel/model/fingerprint cases; existing tests preserved.
9. docs/PORT_REGISTRY.md / README / docs/ARCHITECTURE.md / THIRD_PARTY.md — design, safety boundaries, credits and explicit unsupported features.

## Verified here

- Galaxy S25 Ultra: Termux su -c id returned UID0 under KSU context. This does NOT mean the new application was already granted its own superuser access.
- Existing legacy VeyraCore package: ctrl.mietze.veyracore version 1.0.1+local.af081c2.
- Existing helper: ctrl.mietze.veyracore.helper version 1.2-vxcore; installed under UID1000.
- Original RMG Next staging process checked in actual DfrInstall.kt / DfrFlow.kt and helper AndroidManifest.xml: package manager signature history, reboot, install system-UID helper, verify UID1000. New project does not edit packages.xml itself.
- Gradle command: bash ./gradlew :app:testDebugUnitTest :app:assembleDebug --no-daemon --console=plain.
- Result: BUILD SUCCESSFUL; 19 JUnit tests, 0 failures, 0 errors, 0 skipped.
- APK: app/build/outputs/apk/debug/app-debug.apk, 59,047,525 bytes; copied to the Samsung Android Downloads folder under VeyraCore-v0.3.0-dev-debug.apk.
- APK SHA-256: 518d0bacfe40d542a012c8887d08ef53a512fc8acde9fc5ed6260c398243a99d.

## Not implemented, not claimed

- NOT 57 native GhostLock/DirtyFrag exploit engines. The 57 are source references, many forks, unverified links or PoCs.
- NOT new Veyra-own independent kernel ring0 Root implementation, nor a one-to-one replacement of KernelSU driver/patch.
- NOT a native helper certificate injector in this independent APK; the app delegates to installed legacy / RMG providers. No full unprivileged Android 16 certificate bypass exists.
- NOT a promise that source catalog matching proves the exploit succeeds on a given build; especially no firmware-precise profile if the source feed does not publish that evidence.
- NOT verified by end-to-end manual interactions on every port/device.
- NOT verified as permanent after user actively dismisses notifications or system force-stops the app; Android policy applies.
- NOT a release-signed production artifact. This is a test build with Android debug signing.

## Device handling

The original Veyra Root and old VeyraCore were left unchanged; no system partition, PackageManager packages.xml, old root scripts or driver was modified. The new APK is a separate package and can be installed alongside the old app. Existing app runtime operations will require user approvals and actual root permissions.

## Next proof criteria

- Launch independent VeyraCore; ensure Root starts UNKNOWN (not fake green), then provide KSU grant and confirm UID0.
- Inspect helper UID1000 through Android PackageManager from this new package.
- Grant POST_NOTIFICATIONS and enable helper notification; confirm notification channel and ongoing status in the UI/OS.
- Check automatic sources selection with real online feeds, and manual selection from the full catalog.
- Verify nothing marks reboot/reroot complete before measuring a real framework restart.
- Only after actual on-device integration tests should this be treated as production-ready.