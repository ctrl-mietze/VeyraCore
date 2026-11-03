# VeyraCore – eingebettete Veyra-Root-Engine (Integrationszweig)

**Stand 3. November 2026 · Experimenteller Source-Port, keine bestätigte Root-APK.**

## Was tatsächlich eingebunden wird

Die Build-Vorbereitung unter \`tools/prepare_internal_engine.py\` erzeugt einen eigenständigen Android-Quellbaum mit der App-ID \`ctrl.veyracore\`. Der Build verwendet den fest gepinnten Original-Quellstand von [Veyra Root](https://github.com/ctrl-mietze/Veyra) (\`c5d8246e972bf5b2704fcd4387650e3f118c59f9\`) einschließlich vorhandener Android-Root-Komponenten, Native-Bibliotheken und Firmware-Payloadquellen. Die vorhandene VeyraCore-Oberfläche sowie Helper-, Logs-, Module- und Root-Statusfunktionen werden im **selben APK** integriert. Die ursprüngliche Veyra-Root-Oberfläche wird von der Core-Startseite als interne Activity geöffnet – kein separates, installiertes Root-Paket wird gestartet.

Dies ist ein Source-Overlay und kein GitHub-Fork von KernelSU. Die Drittquellen und Lizenzen im eingebundenen Veyra-Root-Projekt bleiben zu beachten.

## Build

Aus dem Stammverzeichnis der VeyraCore-Quellen:

\`\`\`bash
python3 tools/prepare_internal_engine.py --build
\`\`\`

Erfordert Git, Android SDK 37 und eine zu Gradle 9.5.1 kompatible JDK-21-Toolchain sowie die upstream deklarierten Android/Compose-Abhängigkeiten. Die Vorbereitung verwendet standardmäßig \`.build/integrated-android\` und verändert die separat installierte Veyra-Root-App nicht.

## Offene technische Gating-Punkte

- **Native Root-Helper-App-ID:** Veyra Root v2 bindet seinen nativen Root-Helper an seinen privaten App-Verzeichnispfad und prüft die Binärdatei gegen einen SHA-256. Für die neue \`ctrl.veyracore\`-Identität ist diese Bindung noch nicht adaptiert/validiert. Ein erfolgreiches APK-Build bedeutet deshalb nicht, dass GhostLock oder ein Reroot-Durchlauf korrekt funktioniert.
- **Zertifikats-/Helper-Ersteinrichtung:** Die schon vorhandene UID-1000-Helper-Installation ist separat. Eine neue Installation durch Änderung der System-Signaturhistorie ist hier weder implementiert noch bestätigt.
- **57 Quellen != 57 Root-Ports:** Die Veyra-Root-Assets enthalten ausführbare unterstützte Profile und daneben Forschungsreferenzen. Fehlende native Payloads können nicht durch das Hinzufügen einer URL ersetzt werden.
- **Integrität:** Es werden weder Kernel-/Firmware-Profilprüfungen noch native Prüfsummen stillschweigend abgeschaltet. Signierte offizielle Veyra-Root-Releases bleiben unberührt.
- **Tests:** Ein abschließender integrierter Build und funktionsfähiges UID0 aus \`ctrl.veyracore\` sind noch nachzuweisen.

**Status:** Source-Integration / Experiment – kein fertiges Root-Release.
