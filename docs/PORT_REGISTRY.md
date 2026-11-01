# Root / Port Registry

Root methods here are **catalog entries only**, not shipped exploits or device compatibility guarantees. The application also exposes a 14-entry cross-brand research index; all entries are linked metadata, not bundled native code. Each profile requires exact manufacturer, model, firmware fingerprint, kernel release, ABI and page-size validation before any provider can be enabled.

| Route | Upstream | In v0.2.0-dev |
|---|---|---|
| Existing Root / SU | Existing system `su` | UID0 verification and limited module actions |
| GhostLock | [Cross-device research](https://github.com/fynks/awesome-temporary-root) | Metadata-only, provider unavailable |
| DirtyFrag | [mitschud/DirtyFrag](https://github.com/mitschud/DirtyFrag) | Metadata-only, provider unavailable |
| Root My Galaxy Next | [Upstream](https://github.com/rushiranpise/Root-My-Galaxy-Next) | Link-only, no payload imported |
| GhostSam | [Samsung research](https://github.com/snothin/GhostSam) | Referenced, no exploit imported |
| Veyra Reroot / Cert | Future Veyra Load helper | Neither UID0 nor native daemon implemented |

### Privilege transport

- `su -c`: bounded, authorized existing root session, never an untrusted free-form user command.
- `/data/adb/modules`: module directory read / limited marker changes.
- `/data/adb/ksu/bin/ksud` or compatible resolved `ksud`: optional pre-existing module installer only.
- `/dev/socket/veyra_loadd`: probed only; no independent Veyra Load daemon shipped.

There are no app-supplied TCP/UDP listeners, ADB servers or arbitrary shell endpoints.

For future native integration each route will need a strict per-build profile, signed binary provenance, recovery procedure, tests on actual device builds, separate user consent and licensing review.
