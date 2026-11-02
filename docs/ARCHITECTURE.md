# Architecture — independent VeyraCore application

The `ctrl.veyracore` Android app contains a Compose UI, validated method registry, bounded root boundary and local module adapter. Unlike a kernel-patching tool, it does not replace the boot image or attempt to write an Android kernel.

| Layer | Responsibility |
|---|---|
| `MainActivity.kt` | Compose state, bottom tabs, root status, import picker, diagnostic screens |
| `RootRoutes.kt` | Explicit route identifiers, upstream references and availability states |
| `InstalledRootTools.kt` | Allowlisted local package visibility, explicit user-launched app transitions, return-and-recheck lifecycle; never in-process exploit execution |
| `Core.kt / SafeShell` | UID0 probe, timeout-bound process runner, safe module markers |
| `ModuleImporter.kt` | SAF staging, bounded ZIP inspection, explicit install confirmation and runtime delegation |
| `LocalStore` | Persistent theme selection, capped app-local diagnostic journal |
| `RootProvider / VeyraRuntimeBridge` | Contracts for future *separately validated* native providers, not current exploit implementations |

**Authority:** VeyraCore only marks root verified when a bounded authorized `su -c id` invocation actually returns UID0. The presence of KernelSU, a certificate, executable file or claim of system UID1000 is not sufficient. Each mutating module operation repeats its root check.

**Modules:** The file adapter reads `/data/adb/modules` and handles supported disable/remove markers through an approved root context. Selected ZIP files are copied to private cache after a document-picker selection and are inspected without extracting locally. The installer checks an existing executable `ksud` and hands the ZIP to that compatible runtime only when authorized and explicitly confirmed. No module scripts are directly executed by VeyraCore. A matching policy backend is not bundled.

**Separation:** Exploit discovery is metadata-only. Distinct future interfaces will be required for native GhostLock/DirtyFrag providers, exact manufacturer build matching, a first-party privileged helper, and a first-party SU-policy backend. No remote payload download or dynamic code loading is in scope.

**Testing:** Kotlin JVM tests cover exact profile matching, module IDs and ZIP validation; physical device tests remain necessary for UI, real root grants and module installation.

## VeyraCore 0.3.0-dev — Root, Zertifikat, Helper und Benachrichtigung

Die eigenständige Android-App ist kein Root-Daemon. Sie verwendet einen bereits verfügbaren und vom Benutzer genehmigten su-Provider. Nur ein tatsächlicher su -c id Nachweis von uid=0 setzt den Root-Zustand auf bestätigt. Ein installierter KernelSU-Manager, eine aktive System-UID1000-Sitzung oder ein Modell-/KMI-Feed-Treffer reicht allein nicht.

Der Port-Katalog hält 57 öffentliche Referenzen als Daten. Nur zwei feste HTTPS-JSON-Manifeste werden angefragt. Deren Geräte- und Kernelinformationen sind Suchkriterien, **keine** automatisierte Exploit-Freigabe. Keine fremde Kernel- oder Exploit-Binary wird heruntergeladen oder ausgeführt.

Der ältere VeyraCore-Helper ctrl.mietze.veyracore.helper wurde auf dem Samsung Galaxy S25 Ultra als UID1000 installiert vorgefunden (Android Package Manager). Die neue App fragt die reale Linux-UID dieser PackageManager-Anwendungsinformation und erkennt diese als Nachweis, dass ein System-SharedUser-Helper akzeptiert ist. Die App selbst hat weiterhin ihre normale Android-App-UID.

Das originale RMG-Next-Zertifikatsverfahren passt die Signaturhistorie des Android-Systemnutzers in /data/system/packages.xml an, wartet auf das erneute Laden durch den Package Manager, installiert den als android.uid.system deklarierten Helper und verifiziert UID1000. **Dieser unabhängige Build führt diese privilegierte Dateiänderung nicht durch** und behauptet keine vollständige neue Injektionskette; er öffnet dafür bei Bedarf eine vorhandene Legacy- oder RMG-Provider-App.

Der Notification-Service ist an den nachgewiesenen Helper UID1000, POST_NOTIFICATIONS und eine lokale Nutzeroption gebunden. Er verwendet den Android-ForegroundServiceType specialUse und zeigt nur den Helper-Zustand an; UID0 wird separat geprüft. Eine weitere Boot-Benachrichtigung kann bei erlaubtem BOOT_COMPLETED versucht werden. Die Kernel-Uptime ist kein geeigneter Nachweis eines Soft Reboots des Android-Frameworks; Neustart/Reroot sind daher nicht voreilig grün markiert.

Sicherheitsgrenzen: Keine unbekannten Shell-Befehle aus dem Katalog, kein ungeprüfter Binärdownload, keine Änderung der ursprünglichen App oder /data/system, kein stilles Einspielen fremder Kernel-Patches; Root-Module werden nur nach UID0-Revalidierung an eine bereits nachgewiesene kompatible Modul-Runtime gegeben.