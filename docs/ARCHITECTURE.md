# VeyraCore 0.2 architecture

VeyraCore is an independent Kotlin/Jetpack Compose application. The implementation separates UI state, evidence collection, root-provider research, and safe module operations so that an unverified research claim cannot become an action button by accident.

## Runtime boundaries

- `MainActivity.kt` owns the Compose surface, adaptive navigation, theme persistence wiring, and user confirmation dialogs.
- `VeyraViewModel` owns observable state and calls only named operations on `SafeShell`. It never accepts arbitrary command text from the UI.
- `SafeShell` is the bounded privileged boundary. It runs `su -c` with timeouts for `id`, SELinux/socket probes, a fixed module listing, marker-file operations, the verified `ksud module install` capability check, and the explicitly confirmed ZIP install.
- `RootIdentity` parses only a leading `uid=` field. `gid=0`, a warning followed by `uid=0`, a manager package, or a `su` path cannot produce a UID-0 result.
- `RootProvider`, `RootProviderRegistry`, `ProviderEvaluation`, and `ProviderProvenance` model future method integrations. Provider preparation is a no-run verification plan; there is no exploit download interface.
- `ProfileMatcher.matchesExact` requires a complete profile: manufacturer, model, firmware/build identity, fingerprint, full kernel release, KMI, ABI, and page size. The default registry has no exploit profiles.
- `ModuleZipVerifier` reads a bounded ZIP stream, rejects absolute/traversal/duplicate paths, rejects Unix symlink metadata from the central directory, and parses `module.prop` as inert text. It does not extract or source archive files.
- `LocalStore` persists only theme/method selection and bounded structured diagnostics in app-private preferences. Log export uses `MediaStore.Downloads`.

## Module flow

1. A user picks a ZIP through SAF.
2. VeyraCore verifies size, central-directory structure, paths, entry count, uncompressed limits, symlink metadata, and required text metadata.
3. The preview remains local and inert.
4. The app checks `ksud module install --help` through the already-authorized `su` backend. If the command is not proven, the install control remains disabled.
5. After an explicit confirmation, the ZIP is copied to app cache, verified again, passed to `ksud`, deleted from app cache, and verified against a fresh module listing. The UI explains that module activation may wait until the next reboot.

## Legacy architecture reference

The earlier working VeyraCore checkout was reviewed read-only. Its `VeyraCoreActivity.kt` coordinated multiple screens, `InstallViewModel.kt` modeled staged installation phases, and `DfrInstall.kt` separated an external `app_process` helper from the app UI. VeyraCore 0.2 uses that separation as a design reference only. It does not import the old helper, certificate path, payload staging, exploit code, KernelSU code, or driver.

## Security and provenance rules

- Unknown exploit binaries are never downloaded automatically.
- No boot image, partition, certificate store, policy database, or helper identity is changed.
- A research repository link is not a Veyra compatibility claim.
- No upstream source is copied without license and notice review. KernelSU is referenced for interoperability concepts only; this project remains independent.
