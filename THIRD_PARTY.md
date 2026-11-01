# Third-party notices and provenance policy

VeyraCore 0.2.0-dev contains no copied exploit source, payload binary, kernel module, driver, helper APK, profile database, private token, or keystore from the projects below. Their public documentation and source links were consulted only to classify research boundaries and to build the offline catalog.

| Project | Upstream | Published license / notice boundary | Use in VeyraCore |
|---|---|---|---|
| DirtyFrag | [mitschud/DirtyFrag](https://github.com/mitschud/DirtyFrag) | Upstream licensing must be followed before any reuse; source and payload review is required. | Documentation metadata only; no CVE implementation, payload, or KMI artifact. |
| KernelSU | [tiann/KernelSU](https://github.com/tiann/KernelSU) | Upstream GPL terms and notices apply to any future source reuse. | Plain attribution to interoperability/module concepts; no source, driver, or binary. VeyraCore remains an independent application. |
| Root My Galaxy Next | [rushiranpise/Root-My-Galaxy-Next](https://github.com/rushiranpise/Root-My-Galaxy-Next) | Upstream license and fork provenance require review before reuse. | Research catalog only; no code, feed, helper, or payload. |
| GhostSam | [snothin/GhostSam](https://github.com/snothin/GhostSam) | Preserve upstream license and NOTICE if any artifact is ever reused. | Research catalog only; no source or prebuilt artifact. |
| GhostLock-Galaxy | [wxxsfxyzm/GhostLock-Galaxy](https://github.com/wxxsfxyzm/GhostLock-Galaxy) | Upstream license and target-specific notices require review before reuse. | Research catalog only; no exploit or offsets. |
| ghostlock-a17 | [mobilehackinglab/ghostlock-a17](https://github.com/mobilehackinglab/ghostlock-a17) | Upstream license and notices require review before reuse. | Research catalog only; no exploit or target data. |
| Root My Pixel | [alex193a/Root-My-Pixel](https://github.com/alex193a/Root-My-Pixel) | Upstream license and notices require review before reuse. | Research catalog only; no Pixel code or payload. |
| GhostLock-oneplus | [JoinChang/ghostlock-oneplus](https://github.com/JoinChang/ghostlock-oneplus) | Upstream license and notices require review before reuse. | Research catalog only; no offsets, binaries, or app code. |
| Root My Device | [tqmane/Root-My-Device](https://github.com/tqmane/Root-My-Device) | Upstream license and exact source provenance must remain intact before reuse. | Research catalog only; no device project or submodule imported. |
| Ghostlock-5.10 | [CrackerCat/GhostLock-5.10](https://github.com/CrackerCat/GhostLock-5.10) | Upstream license and notices require review before reuse. | Research catalog only; no Fire payload or exploit. |
| RootMyVivo | [zenyxx-xd/RootMyVivo](https://github.com/zenyxx-xd/RootMyVivo) | Upstream license and notices require review before reuse. | Research catalog only; no vivo/iQOO source or remote catalog. |

## Independent-source rule

The new VeyraCore source is independently authored. The previous working VeyraCore tree and Raspberry Manager tree were inspected read-only for architecture and visual direction; they are not dependencies and were not copied. Any future integration must record the exact upstream commit, license, notices, hashes, device profile, execution context, and verification evidence before being described as integrated.
