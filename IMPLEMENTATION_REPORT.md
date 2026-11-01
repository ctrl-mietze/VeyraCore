# VeyraCore v0.2.1-dev — Implementation report (2026-11-01)

## Current build — 0.2.1-dev

- Original `ctrl.veyracore` Android app and original Veyra OLED / Berry palettes retained. Existing user apps are not overwritten.
- Added real installed-app discovery and allowlisted explicit launcher handoff for Veyra Root, ZF6Root, and Root My Galaxy Next.
- Added automatic bounded UID-0 recheck after returning from external root apps; launch alone never changes the verified root state.
- Added ABI, page size, and security-patch diagnostics.
- Research index expanded from 14 to **18** verified repository links, **not executable root ports**.
- Kept real existing-root module ZIP staging/validation and compatible existing-runtime module controls.
- Added five new tests: installed-tool allowlist (3) and research-index integrity (2).
- **Verified on Windows: `testDebugUnitTest assembleDebug --no-daemon -Pkotlin.incremental=false -Pkotlin.compiler.execution.strategy=in-process` — BUILD SUCCESSFUL; 14/14 JUnit tests.**
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk` — **59,198,927 bytes**.
- SHA-256: `b6aa482841d593d5eee97f0c70b1096e838dc96ada754002e706ba61527edc4e`.

### Not delivered

- Native GhostLock or DirtyFrag exploit chain, privileged Veyra Load daemon and kernel drivers, universal certificate-based reroot and an independent SU-policy backend remain unimplemented.
- No device-root attempt or cold-boot persistence test has been performed using this 0.2.1 app. Installed-tool handoff and UI require physical-device acceptance tests.

---

## Historical baseline — v0.2.0-dev

## Implemented in this development iteration

- Continued the independent ctrl.veyracore v0.1.0 Compose baseline; no replacement of the existing user's other apps.
- Applied Veyra OLED and Veyra Berry design tokens grounded in existing Veyra Root / Raspberry Manager source.
- Added `RootRoutes.kt`: Existing Root, GhostLock, DirtyFrag, Veyra Reroot, with explicit statuses, source URL actions and a 14-source expandable port research index instead of nonfunctional pretend launch buttons.
- Added `ModuleImporter.kt`: Android OpenDocument ZIP choice, app-private stage and bounded ZIP path/entry/size validation; user confirmation; existing-ksud delegation only following fresh UID0 proof.
- Added module import UI, diagnostics and error logging. Existing module refresh, disable and removal controls remain.
- Hardened bounded root shell output consumption to avoid stalling on pipe output, and reconfirmed root for mutating module actions.
- Added four unit tests for valid module archives, missing module.prop, traversal and absolute entry rejection.
- Updated documentation and package version metadata.

## Deliberately not implemented

- No native exploit/driver copied, renamed or executed; no bootstrap, injection, kernel patch or universal device root.
- No independent privileged `veyra_loadd`/SU policy manager and no fabricated allow/deny entries.
- No direct KernelSU source copying, driver 3.3.0 redistribution or use of other projects' private signing keys.
- No auto-root after reboot, certificate elevation or bootloader modification.
- Module ZIP installer relies on an already present compatible ksud runtime and successful root authorization; compatibility requires actual physical device tests.

## Build and verification

- `gradlew.bat testDebugUnitTest assembleDebug --no-daemon` passed on Windows / JDK 17 / Android SDK 36 (01 Nov 2026).
- **9/9 JVM tests passed**, including four new ZIP safety checks.
- Debug APK built: `app/build/outputs/apk/debug/app-debug.apk`, 59,136,003 bytes.
- SHA-256: `62fa101c13cc2537d4219bd498375d095bc455ed2dea2b5178fd9ccba1499b3b`.
- Build log from the Surface workspace: `docs/build-v020.log` (local build artifact, intentionally not committed).
- CI and real-device root checks are still required before any stable release. A successful debug build does **not** prove a working native root exploit.
