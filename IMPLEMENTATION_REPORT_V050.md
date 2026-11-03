# VeyraCore 0.5.0-dev — integrierte Veyra-Root-Profildaten

Stand: 3. November 2026  
Android-Anwendung: `ctrl.veyracore`  
Quelle: [Veyra Root](https://github.com/ctrl-mietze/Veyra) und bisherige [VeyraCore](https://github.com/ctrl-mietze/VeyraCore) 0.4-Branch

## Tatsächlich umgesetzt

**VeyraProfileEngine.kt** ist eine neue, interne, read-only Root-Profilerkennung nach den abgestuften Belegregeln von Veyra Root `SupportManifest.kt`. Sie prüft Geräte-Modell, Geräte-Codename, Build-ID, Build-Fingerprint und den Kernel mit Unterscheidung von Kernel-Serie, dreiteiliger Version und vollständigem Kernel-Release. Auch `exactMatch.kernelRelease` wird erkannt. Daten werden nicht automatisch zu einem Root-Erfolgsnachweis hochgestuft.

**Direkt mitgelieferte Veyra-Root-Daten**, aus der bestehenden Root-App-Quellbasis übernommen:
- `app/src/main/assets/veyra-profiles/dfroot-targets-v3.json`: 12 DF-Profile
- `app/src/main/assets/veyra-profiles/pixel-targets-v3.json`: 29 Pixel-Profile

Diese Dateien enthalten ausschließlich Daten und Artefakt-Beschreibungen; die darin genannten Kernel-Exploits, .so-Dateien, Kernelmodule, Daemons und Helper-Binärdateien wurden **nicht** übernommen. Ein Profil-Eintrag ist deshalb nicht ausführbar. Die App erkennt und kennzeichnet Angaben zu Artefaktgröße und SHA-256, wenn vorhanden, um einen Belegstatus statt eines erfundenen Kompatibilitätsversprechens anzuzeigen.

**Online-Profile**: Die bestehenden beiden Root-My-Galaxy-Payload-Support-Manifeste werden weiterhin ausschließlich als begrenzt gelesene JSON-Metadaten abgerufen. Der neue Evidence-Parser ergänzt die bisherige VeyraCore-Profiltabelle. Die interne Ansicht sortiert nach Evidenzstufe, nicht nach einer behaupteten Exploit-Erfolgsgarantie.

**Startseite**: Die bisherigen externen Root-App-Karten sind aus der Hauptansicht entfernt. Ein eigener Bereich „Veyra Root · interne Profildaten“ zeigt erkannte Kandidaten, Quellen, Kernel-Familie und die Qualität der Nachweise. Die Rootprüfung findet innerhalb von `ctrl.veyracore` per nutzerautorisierter `su -c id` statt. Module und Superuser arbeiten weiter über bereits autorisierte externe Root-Runtimes; die bisherigen Helper-/Benachrichtigungs-/OLED/Berry-Funktionen bleiben erhalten.

## Was weiterhin nicht implementiert wurde

- **Keine native GhostLock/DirtyFrag-Kernel-Exploit-Engine** in VeyraCore, keine automatische Ausführung der 57 verlinkten fremden Repositories.
- Kein Payload-Download, keine Ausführung von .so/.ko-Binärdateien, keine eigenen Kernelpatches und keine neue privilegierte Root-Installation auf einem bislang nicht gerooteten Gerät.
- Kein Plattformzertifikat-/System-UID-Bypass; die vorhandene signaturgeprüfte Helper-Aktualisierung bleibt auf bereits vertrauenswürdig installierte UID-1000-Helper begrenzt.
- Keine geräteübergreifende Erfolgsgarantie und keine praktischen Root-/Reroot-Tests auf einem Gerät ohne Root.
- Der neue interne Profildaten-Teil und die bestehende autorisierte `su`-Kontrollschicht sind funktional unabhängig von einer zweiten Root-App, **aber sie sind keine aus Veyra Root übernommene Kernel-Exploit-Laufzeit**.

## Herkunft und Prüfbarkeit

Ursprung der Manifest-Daten: `ctrl-mietze/Veyra`, Veyra-Root-v2-Lokalquellen. Die Daten-Hashes auf dem Gerät bei der Übernahme:
- DF: `e0246f4f16195bbd71fa864bd81acb698ce609982e517fb39b10d599a1a08058`
- Pixel: `bd5a31a2cd793920e2e8e911c345fed9c63ce5ae691f9141325513f40841257d`

Codeherkunft der Matching-Regeln: orientiert an Veyra Root `SupportManifest.kt`, für die eigenständige App neu geschrieben. Upstream-Projekte bleiben in `docs/PORT_REGISTRY.md` und `THIRD_PARTY.md` verlinkt. Kein KernelSU-Fork. Das originale Veyra-Root-Repository und die vorhandenen installierten Apps wurden nicht verändert.

## Build- und Teststand

Die Build-Umgebung verwendet einen isolierten Gradle-Cache, weil ein anderer Prozess den globalen Cache belegte. Android Build :app:assembleDebug: BUILD SUCCESSFUL. JUnit :app:testDebugUnitTest: **31 Tests bestanden, 0 Fehler**. APK: /storage/emulated/0/Download/VeyraCore-v0.5.0-dev-debug.apk, signierte Debug-APK fuer Paket ctrl.veyracore, versionCode 6, versionName 0.5.0-dev. APK-SHA-256: 7da95c4e36e55840e9d7de2a9d3c05e4040f68756316e8a3c8099bdb0206e008. Ein echter Root- oder Helper-Reroot-Test in Android wurde nicht durchgefuehrt.