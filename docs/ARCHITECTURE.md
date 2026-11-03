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

## Version 0.4.0-dev – eigenständige Kontrollschicht

VeyraLoadRuntime ist eine eigenentwickelte Kotlin-Schnittstelle über vorhandene, vom Nutzer freigegebene Root-Implementierungen. Sie prüft die Root-Identität mit einem echten Prozess-Aufruf und unterscheidet SU_ONLY, KERNELSU, MAGISK und NONE. Sie ersetzt keine Kernel-Berechtigungsarchitektur; System-UID1000 und UID0 sind strikt getrennt.

VeyraModuleInstaller ermittelt den verfügbaren Modultransport nach Root-Revalidierung, prüft den Importpfad und führt nur fest codierte Installer-Subcommands aus. Das ZIP wird im privaten App-Cache gegen Traversal- und Größenangriffe überprüft. Ausgabe- und Fehlercodes stammen von den tatsächlichen externen Installern.

HelperInspector kontrolliert Paketname, PackageManager-UID, Version und Zertifikat-Hash. Die Update-Strecke HelperPackageInstaller öffnet ausschließlich für bereits akzeptierte UID1000-installierte Helper-Pakete mit identischer Signatur den Android-Paketinstaller. Ein beliebiges APK oder eine neue Signatur kann nicht in den System-SharedUser eingeschleust werden. Ein Android-Signaturbypass ist nicht Bestandteil dieser Updatefunktion.

Die Foreground-Service-Benachrichtigung wird nicht mehr während der Initialisierung des ViewModels gestartet; sie wird aus einer sichtbaren Activity oder aus zulässigem Boot-Kontext angefordert. Die tatsächliche Android-Sichtbarkeit wird separat abgefragt.

## 0.5.0-dev — Internal Veyra Root profile evidence

VeyraProfileEngine.kt reads bundled metadata copied from Veyra Root's local DF and Pixel target manifests, plus the existing bounded online support feeds. It parses schema version 3, validates profile IDs, checks payload and daemon descriptor hashes for well-formedness when declared, and keeps the manifest SHA-256 for provenance. It never loads or runs native payload artifacts.

Evidence is graded in ascending order: no match, wildcard/kernel-series candidate, exact model with version-only kernel, exact full uname kernel-release, and full release with matching declared build-id. exactMatch.kernelRelease from Veyra Root SupportManifest is folded into the comparison to prevent silently downgrading exact firmware evidence to a broad KMI match. This ranking does not itself certify exploit compatibility.

The Home screen no longer foregrounds external app launchers. Root status remains solely an authorized UID0 verification. Profiles are not treated as root capabilities, and the internal control plane cannot grant root without an existing authorized SU backend.