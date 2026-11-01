# Root provider status — v0.2.1-dev

| Provider / surface | Status | What VeyraCore does |
|---|---|---|
| Veyra Load runtime | Future | Interface only; no daemon, policy bridge, driver, or socket service shipped. |
| GhostLock / GhostSam research | Unverified | Presents an honest unavailable card; no chain, offsets, payloads, or compatibility promise. |
| DirtyFrag / CVE-2026-43284 | Unverified | Presents research metadata only; no exploit code or KMI table. |
| Root My Galaxy profile discovery | Unverified | Exact-match model exists, but the registry intentionally contains no imported profiles. |
| Existing `su` provider | Implemented | Bounded UID-0 probe and, after verification, bounded module operations. |
| Installed Veyra Root / ZF6Root / RMG Next | Implemented hand-off | Checks local installed apps; only launches after a tap; re-checks `su -c id` on return. Does not run their native exploit inside VeyraCore. |

No button in this version starts a root exploit inside VeyraCore or modifies a boot image or kernel. External app launch is explicitly labelled and followed by fresh privilege verification after returning to VeyraCore.
