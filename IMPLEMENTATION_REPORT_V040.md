# VeyraCore 0.4.0-dev — Runtime- und Helper-Integration

**Stand:** 2. November 2026  
**Basis:** Pull Request #8, Branch `feat/0.3-root-catalog-helper-20261102`  
**Paket:** `ctrl.veyracore` (getrennt von `ctrl.mietze.veyracore`)

## Tatsächlich im Quellcode umgesetzt

### Veyra Load
- Eigenständige Kotlin-Kontrollschicht `VeyraLoadRuntime.kt` mit frischer UID-0-Abfrage über den bereits vorhandenen, nutzerautorisierten `su`-Provider.
- Getrennte Erkennung von vorhandener KernelSU-Runtime, Magisk-Runtime oder reinem SU-Zugriff.
- SELinux-Status und lesbarer Modulbestand werden nur mit tatsächlicher Root-Antwort angezeigt.
- Die Oberfläche erhält eine separate Runtime-Karte, einen Aktualisieren-Knopf und konkrete Diagnosedaten.
- Kein Kopieren/Umbenennen von KernelSU-Kernel-Treibern; Veyra Load ist die unabhängige Managementschicht, nicht ein eigenständiger Privilege-Escalation-Exploit.

### Kompatible Module
- Verifiziertes Staging nach ZIP- und Pfadprüfung bleibt erhalten.
- `ModuleRuntime.kt` wählt **KernelSU `ksud module install`** oder **Magisk `magisk --install-module`** auf Grundlage vorhandener, erkannter CLIs.
- Vor Installationsaufrufen wird UID 0 erneut geprüft. Keine Nutzereingaben als freie Shell-Befehle. Fehler der echten Installer bleiben sichtbar.
- Auslesen, Deaktivieren und Entfernen installierter Module wurden aus 0.3 beibehalten.

### Zertifikat und Helper
- `HelperInspector` überprüft Paketkennung, installierte UID, Version und SHA-256 des APK-Signer-Zertifikats über Android PackageManager.
- Bei mehreren vorhandenen Helpers hat ein tatsächlich bestätigter UID-1000-Helper Vorrang gegenüber einem gleichnamigen Normal-UID-Paket.
- Die neue, eigenständige `HelperPackageInstaller`-Strecke nimmt nur freigegebene Helper-Paketkennungen an, validiert den APK-Signer gegen die bereits **installierte** UID-1000-Variante, lehnt Downgrades ab und übergibt die Installation nach ausdrücklicher Benutzerbestätigung an den Android-Paketinstaller.
- Eine neue System-UID-Berechtigung wird dabei **nicht** erschlichen; auf einem Gerät ohne bereits akzeptiertes System-UID-Zertifikat funktioniert diese Update-Strecke nicht als Erstinstallation.
- Vorhandener Veyra-Helper `ctrl.mietze.veyracore.helper` bleibt kompatibel. Kein Modifizieren von `/data/system/packages.xml`.

### Benachrichtigungen
- Bestehender Foreground-Service bleibt an eine vom PackageManager bestätigte Helper-UID1000 gebunden.
- Beim Start der App wird der Dienst nicht mehr aus dem ViewModel-Initialisierer gestartet. Erst wenn die Activity sichtbar ist oder Android die Boot-Ausführung erlaubt, wird er aktiviert.
- Die Benachrichtigung behauptet keine Root-Berechtigung allein aufgrund von UID1000.

### Payload-Katalog
- 57 kuratierte GhostLock-/DirtyFrag-/Root-Projektreferenzen und zwei GitHub-Support-Metadatenfeeds aus 0.3 bleiben erhalten.
- Modell-/Kernel-Matches sind Profile, keine endgültige Exploit-Kompatibilitätszusage. Eine echte Firmwareprüfung ist nur möglich, wenn der Upstream die betreffenden Metadaten bereitstellt.
- Manuelle Auswahl, optionale Vorprüfung und Öffnen installierter Provider bleiben möglich.

## Nicht als erledigt ausgegeben

- Keine eigene native GhostLock-/DirtyFrag-Kernel-Exploit-Engine für alle 57 Repositories.
- Keine unprivilegierte, geräteübergreifende Eskalation zu Root und keine KernelSU-Treiberkopie.
- Kein eigenständiger Bypass der Android-System-Signaturprüfung oder automatische Aufnahme eines eigenen APK-Zertifikats in System-SharedUser-Daten.
- Kein eigenständiger KernelSU-kompatibler Superuser-Allow/Deny-Kerntreiber.
- Keine erfolgreichen End-to-End-Root-/Helper-Update-/Neustart-Tests auf echten Geräten.
- Eine erfolgreiche APK-Kompilierung allein beweist nicht, dass eine System-UID-Installation oder ein fremder Exploit funktioniert.

## Verifikation und Veröffentlichung

Der Quellstand wurde gegen Android SDK 36 für `ctrl.veyracore` gebaut. Der finale Build ist erfolgreich: 24 JUnit-Tests bestanden, 0 Fehler; BUILD SUCCESSFUL. Debug-APK auf dem Galaxy S25 Ultra: /storage/emulated/0/Download/VeyraCore-v0.4.0-dev-debug.apk. SHA-256: 5e358d1dd14b061e0a8a365809297e11241c79e32bc0f938afe3586511c1f82f. Installierte Altversionen und der vorhandene System-Helper wurden nicht verändert. Änderungen werden nur in einen separaten GitHub-Entwicklungsbranch übernommen; kein Merge in `main` und keine automatische Geräteinstallation.

## Quellen und Namensnennung

- [Root My Galaxy Next](https://github.com/rushiranpise/Root-My-Galaxy-Next) – getrennte Recherche zur System-UID-Helper-Architektur.
- [Root My Galaxy Payloads](https://github.com/BuSung-dev/Root-My-Galaxy-Payloads) – Geräteprofile.
- [KernelSU-Modulformat](https://github.com/tiann/KernelSU) – Modulkompatibilität.
- [DirtyFrag-Forschung](https://github.com/mitschud/DirtyFrag) – Referenz, keine native Codeübernahme.

Für die hier hinzugefügten Runtime- und Helper-Management-Dateien wurden keine nativen Kernel-Exploit-Binaries aus diesen Repositories übernommen.