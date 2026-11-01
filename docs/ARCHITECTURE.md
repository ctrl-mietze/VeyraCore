# Architecture — independent VeyraCore application

The `ctrl.veyracore` Android app contains a Compose UI, validated method registry, bounded root boundary and local module adapter. Unlike a kernel-patching tool, it does not replace the boot image or attempt to write an Android kernel.

| Layer | Responsibility |
|---|---|
| `MainActivity.kt` | Compose state, bottom tabs, root status, import picker, diagnostic screens |
| `RootRoutes.kt` | Explicit route identifiers, upstream references and availability states |
| `Core.kt / SafeShell` | UID0 probe, timeout-bound process runner, safe module markers |
| `ModuleImporter.kt` | SAF staging, bounded ZIP inspection, explicit install confirmation and runtime delegation |
| `LocalStore` | Persistent theme selection, capped app-local diagnostic journal |
| `RootProvider / VeyraRuntimeBridge` | Contracts for future *separately validated* native providers, not current exploit implementations |

**Authority:** VeyraCore only marks root verified when a bounded authorized `su -c id` invocation actually returns UID0. The presence of KernelSU, a certificate, executable file or claim of system UID1000 is not sufficient. Each mutating module operation repeats its root check.

**Modules:** The file adapter reads `/data/adb/modules` and handles supported disable/remove markers through an approved root context. Selected ZIP files are copied to private cache after a document-picker selection and are inspected without extracting locally. The installer checks an existing executable `ksud` and hands the ZIP to that compatible runtime only when authorized and explicitly confirmed. No module scripts are directly executed by VeyraCore. A matching policy backend is not bundled.

**Separation:** Exploit discovery is metadata-only. Distinct future interfaces will be required for native GhostLock/DirtyFrag providers, exact manufacturer build matching, a first-party privileged helper, and a first-party SU-policy backend. No remote payload download or dynamic code loading is in scope.

**Testing:** Kotlin JVM tests cover exact profile matching, module IDs and ZIP validation; physical device tests remain necessary for UI, real root grants and module installation.
