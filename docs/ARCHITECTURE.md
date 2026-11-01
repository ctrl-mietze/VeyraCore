# Architecture

VeyraCore is an independent Kotlin/Jetpack Compose application. Its boundaries are deliberate:

- `MainActivity` provides Compose state and navigation only.
- `SafeShell` is the one bounded privileged-command boundary. It executes a fixed command set through `su -c`, uses timeouts, validates module identifiers, and never accepts arbitrary UI command text.
- `RootProvider` and `VeyraRuntimeBridge` are contracts for a future, separately reviewed Veyra Load runtime. They have no exploit implementation in 0.1.0.
- `ProfileMatcher` requires exact manufacturer, model, build fingerprint, and kernel equality. The shipped registry is empty.
- `LocalStore` persists theme choice and structured local diagnostics using app-private preferences. Download export uses `MediaStore.Downloads` and does not request broad storage access.

`su -c id` with an actual UID 0 response is the sole root verification condition. Manager presence, `su` path existence, certificates, and setup indicators are not verification.

When root is verified, the module adapter reads `/data/adb/modules` and manages only `disable` and `remove` marker files. It does not implement kernel operations or a KernelSU backend. `module.prop` is treated as untrusted text and parsed without sourcing it.
