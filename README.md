# VeyraCore

**VeyraCore** is an independent Android root-management and temporary-root research project by ctrl-mietze.

- Android application ID: `ctrl.veyracore`
- Two visual themes: **Veyra OLED** (true black + violet) and **Veyra Berry** (rose/violet + charcoal).
- Device-first compatibility discovery; exact firmware and kernel profiles must be verified, not guessed.
- The Home / Settings / Logs navigation is always present; Modules and Superuser appear only when verified root access exists.
- Architecture separates Android UI, exploit providers, privileged runtime, and module-management interfaces.
- Root state must be verified on-device. A successful certificate or setup step is **not proof of UID 0**.

## Status

**Experimental development.** This repository is being established from a clean, independent codebase. Do not treat an unintegrated exploit or a UI preview as a working rooting method.

## Relevant upstream research

- [Root My Galaxy Next](https://github.com/rushiranpise/Root-My-Galaxy-Next) — rooted-device profiles and helper research.
- [DirtyFrag](https://github.com/mitschud/DirtyFrag) — CVE-2026-43284 research and Samsung adaptations.
- [KernelSU](https://github.com/tiann/KernelSU) — SU policy and module compatibility reference.
- [GhostSam](https://github.com/snothin/GhostSam) — Samsung Ghostlock research.

Third-party source and payload binaries are not included in the initial tree; adding any requires verification of provenance, redistributability, device compatibility, and original notices.

## Build

The Android project and CI workflow are under development. Do not download APKs from unverified sources claiming to be official VeyraCore builds.
