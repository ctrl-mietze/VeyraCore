# Root provider status — VeyraCore 0.2.0-dev

The registry is an evidence boundary, not a list of hoped-for ports. A method may be called `supported` only after its exact profile and a reproducible target-device run are both available. No exploit profile is shipped in this version.

| Method ID | Method | Status in 0.2.0-dev | What is actually implemented | Provenance |
|---|---|---|---|---|
| `ghostlock` | GhostLock | `unavailable` | Research metadata, exact-profile contract, safe selection, and no-run preparation message. | [GhostLock-Galaxy](https://github.com/wxxsfxyzm/GhostLock-Galaxy) |
| `dirtyfrag` | DirtyFrag | `blocked` | Honest blocked state; no CVE logic, payload, KMI table, or kernel module. | [DirtyFrag](https://github.com/mitschud/DirtyFrag) |
| `vx-cert-reroot` | VX Cert / Re-Root | `requires external runtime` | Legacy helper/runtime architecture is documented; no certificate mutation, helper APK, or re-root action is included. | Read-only legacy architecture review |
| `authorized-su` | Existing authorized `su` | `supported` only after proof | Bounded `su -c id`, SELinux/socket evidence, safe module listing, marker operations, and optional verified `ksud module install`. | VeyraCore `SafeShell` implementation |

## Exact profile rule

`ProfileMatcher.matchesExact` requires equality for:

- manufacturer and model;
- firmware/build identity and complete build fingerprint;
- complete kernel release and KMI;
- primary ABI; and
- page size.

Missing or `unknown` values are not wildcards for provider compatibility. The default registry contains an empty profile set, so no research card can become a compatibility claim on a Galaxy, Pixel, S25 Ultra, or any other device.

## Safe preparation state

Selecting a method persists only its method ID. Preparing a method writes a log entry stating that a verification plan was selected; it does not download, execute, flash, reboot, patch, install a helper, or alter root policy. A future provider must add its own reviewed implementation and tests before its status can change.

## Existing root backend

`SafeShell` is not an exploit provider. It observes and uses an already-authorized `su` endpoint only after a fresh UID-0 proof. Module operations are deliberately narrow: read text metadata, create/remove the `disable` marker, create the confirmed next-boot `remove` marker, and call `ksud module install` only after the command's help surface is verified. Install success is reported only after a fresh module listing contains the requested ID.

## Veyra Load / Superuser

There is no first-party Veyra Load policy daemon or grant backend in this release. Superuser is therefore read-only and shows the actual UID proof plus a socket probe. UID 1000/system and UID 2000/ADB shell are not accepted as UID 0, and settings cannot spoof a grant.
