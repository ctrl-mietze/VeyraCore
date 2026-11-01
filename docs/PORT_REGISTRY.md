# Root / Port Registry — VeyraCore 0.2.1-dev

## Actual implemented capabilities

Root methods in the research catalog are **references only**, not bundled executable exploits or device compatibility guarantees. This Android app exposes an 18-entry research index. The independently authored runtime detects existing root using a bounded UID-0 probe.

| Route | Source | VeyraCore behavior |
|---|---|---|
| Existing Root / SU | Already installed Android `su` | Verifies actual UID 0, allows limited module operations |
| Installed Veyra Root | `ctrl.mietze.veyraroot` | Explicit user-tapped external app launch; recheck on return |
| Installed ZF6Root | `de.veyracore.zf6root` | Explicit external launch, no supported-device assumption |
| Installed Root My Galaxy Next | `dev.rushiranpise.rmgnext` | Explicit external launch, no copied exploit or driver |
| GhostLock research | https://github.com/YuKongA/ghostlock-app | Catalog only; no root execution within VeyraCore |
| DirtyFrag research | https://github.com/mitschud/DirtyFrag | Catalog only; kernel/driver unavailable |
| GhostSam research | https://github.com/snothin/GhostSam | Research link only |
| Veyra Reroot / Cert | Future Veyra Load backend | No independent root daemon or privileged certificate implementation |

## Support matching

When native provider development begins, exact manufacturer, model, build fingerprint, kernel release, Android ABI, page size and security patch must be checked for each supported profile. Kernel version family/KMI alone does not prove that a native exploit works. Some upstream ports only support selected firmware builds and may panic on unverified devices.

The installed-app handoff deliberately provides **no blanket compatibility assertion**. Root state is checked again when VeyraCore returns to the foreground.

## Existing-privilege module support

- `su -c id` verifies real UID 0, not system UID 1000.
- `/data/adb/modules` metadata and safe module markers are handled only after real root authorization.
- A compatible installed `ksud` can optionally install validated module ZIPs after explicit confirmation.
- `/dev/socket/veyra_loadd` may be probed; no Veyra Load daemon is shipped yet.

No network listener, hidden root grant, background exploit, boot-patcher, or arbitrary shell endpoint is bundled.

A future first-party native backend requires independently validated code, strict firmware profiles, rollback testing, exact signed artifact provenance, and third-party license compliance. In particular, KernelSU GPL sources cannot simply be renamed and redistributed while denying the original provenance.
