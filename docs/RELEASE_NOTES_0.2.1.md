# VeyraCore 0.2.1-dev — Installed root app integration

## New in this development build

- **Installed-app discovery:** Veyra Root (`ctrl.mietze.veyraroot`), ZF6Root (`de.veyracore.zf6root`) and Root My Galaxy Next (`dev.rushiranpise.rmgnext`).
- **Explicit hand-off:** Only an installed, allowlisted root app can be opened, and only after the user taps its card. A hand-off never implies that root succeeded.
- **Return verification:** VeyraCore runs a bounded `su -c id` check after the user returns from the external tool. The existing root-state and module tabs are updated from actual evidence.
- **Device diagnostics:** ABI, memory page size and Android security patch appear alongside the device/build and kernel information.
- **Safer visibility:** Android package queries are limited to the three installed-tool integrations; no broad installed-app inventory is requested.
- **Automated tests:** Test identity uniqueness and prohibit unregistered root-tool entries. Existing root and ZIP validation tests remain.

## Preserved

- Independent Android package `ctrl.veyracore`; original Veyra Root and VeyraCore installations are untouched.
- OLED and Berry themes, local diagnostics export, module import and removal confirmation.
- Real UID-0 verification; neither an installed manager, System UID 1000, nor certificate acceptance is treated as proof of root.

## Scope and limitations

This update adds functional integration **with already installed root applications**, not an embedded GhostLock or DirtyFrag exploit. It does not ship a Veyra Load kernel driver, a native `su` service, cross-device exploitation binaries, a proprietary certificate, or a replacement for KernelSU's policy engine. A Root-My-Galaxy or DirtyFrag kernel-module artifact cannot be renamed and assumed to become a standalone, compatible root runtime.

Before a stable release, validate the external transitions, UI layout and module operations on specific Android firmware versions and review upstream licensing for any future code import. Debug APKs are not production-verified root releases.
