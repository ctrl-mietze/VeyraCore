# Root provider status — v0.1.0

| Provider / surface | Status | What VeyraCore does |
|---|---|---|
| Veyra Load runtime | Future | Interface only; no daemon, policy bridge, driver, or socket service shipped. |
| GhostLock / GhostSam research | Unverified | Presents an honest unavailable card; no chain, offsets, payloads, or compatibility promise. |
| DirtyFrag / CVE-2026-43284 | Unverified | Presents research metadata only; no exploit code or KMI table. |
| Root My Galaxy profile discovery | Unverified | Exact-match model exists, but the registry intentionally contains no imported profiles. |
| Existing `su` provider | Implemented | Bounded UID-0 probe and, after verification, bounded module operations. |

No button in this version starts a root exploit, jailbreaking flow, boot-image patch, or kernel driver operation. If a later build opens an installed external tool, that action must be labelled as an external transition and must re-run verification upon return.
