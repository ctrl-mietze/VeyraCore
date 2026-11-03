#!/usr/bin/env python3
"""
Generate a VeyraCore APK source tree that embeds the complete Veyra Root v2 Android source.

Pinned upstream source, original native libraries/asset catalogs, and third-party notices
are retained. This is a DEVELOPMENT INTEGRATION, not validated to acquire kernel root
under the new application ID; native helper paths/cert bootstrap require separate review.

Run from the VeyraCore repo: python3 tools/prepare_internal_engine.py
Optionally build: python3 tools/prepare_internal_engine.py --build
"""
from __future__ import annotations

import argparse
from pathlib import Path
import shutil
import subprocess

UPSTREAM = "https://github.com/ctrl-mietze/Veyra.git"
UPSTREAM_COMMIT = "c5d8246e972bf5b2704fcd4387650e3f118c59f9"
PACKAGE_ID = "ctrl.veyracore"
BASE = Path(__file__).resolve().parents[1]
ENGINE = BASE / ".build" / "integrated-android"


def run(*args: str, cwd: Path | None = None) -> None:
    subprocess.run(args, cwd=cwd, check=True)


def replace_once(value: str, old: str, new: str, filename: str) -> str:
    n = value.count(old)
    if n != 1:
        raise RuntimeError(f"{filename}: expected exactly one occurrence, found {n}: {old[:80]}")
    return value.replace(old, new, 1)


def modify(path: Path, old: str, new: str) -> None:
    contents = path.read_text()
    path.write_text(replace_once(contents, old, new, str(path)))


def prepare(refresh: bool) -> None:
    if refresh and ENGINE.exists():
        shutil.rmtree(ENGINE)
    if not (ENGINE / ".git").exists():
        ENGINE.parent.mkdir(parents=True, exist_ok=True)
        run("git", "clone", "--no-checkout", UPSTREAM, str(ENGINE))
        run("git", "checkout", "--detach", UPSTREAM_COMMIT, cwd=ENGINE)
    actual = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ENGINE, text=True).strip()
    if actual != UPSTREAM_COMMIT:
        raise RuntimeError(f"Engine pinned revision mismatch: {actual}")
    app = ENGINE / "app"
    for subdir in (
        "src/main/java/ctrl/veyracore",
        "src/test/java/ctrl/veyracore",
    ):
        src = BASE / "app" / subdir
        dest = app / subdir
        if not src.is_dir():
            raise RuntimeError(f"Missing VeyraCore files: {src}")
        shutil.copytree(src, dest, dirs_exist_ok=True)

    gradle = app / "build.gradle.kts"
    text = gradle.read_text()
    text = replace_once(text, 'val appVersionBase = "2.0.0"',
                        'val appVersionBase = "0.5.0-internal"', str(gradle))
    text = replace_once(text, 'val appVersionCode = 200_000_000',
                        'val appVersionCode = 500_001', str(gradle))
    text = replace_once(text, 'applicationId = "ctrl.mietze.veyraroot"',
                        'applicationId = "ctrl.veyracore"', str(gradle))
    # Own dev signing identity: never claim to be the separately signed Veyra Root release.
    # This does not change firmware compatibility or native artifact verification.
    head = text.index("        debug {", text.index("    buildTypes {"))
    tail = text.index("\n        }", head)
    block = text[head:tail]
    block = block.replace('buildConfigField("boolean", "RELEASE_HARDENED", "true")',
                          'buildConfigField("boolean", "RELEASE_HARDENED", "false")')
    block = block.replace('buildConfigField("boolean", "PUBLIC_RELEASE", "true")',
                          'buildConfigField("boolean", "PUBLIC_RELEASE", "false")')
    block = block.replace("isDebuggable = false", "isDebuggable = true")
    text = text[:head] + block + text[tail:]
    gradle.write_text(text)

    modify(app / "src/main/res/values/strings.xml",
           '<string name="app_name">Veyra Root</string>',
           '<string name="app_name">VeyraCore</string>')

    manifest = app / "src/main/AndroidManifest.xml"
    old_activity = """        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <!-- What a long press on the app icon offers, without the app being opened first. -->
            <meta-data
                android:name="android.app.shortcuts"
                android:resource="@xml/shortcuts" />
        </activity>"""
    new_activity = """        <!-- Both UIs and the original Root engine are compiled into this ONE APK. -->
        <activity android:name=".MainActivity" android:exported="false" />
        <activity android:name="ctrl.veyracore.MainActivity"
            android:label="VeyraCore" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        <service android:name="ctrl.veyracore.VeyraHelperNotificationService"
            android:exported="false" android:stopWithTask="false"
            android:foregroundServiceType="specialUse">
            <property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="Local helper status notification" />
        </service>
        <receiver android:name="ctrl.veyracore.VeyraHelperBootReceiver"
            android:exported="false">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>"""
    modify(manifest, old_activity, new_activity)
    modify(app / "src/main/res/xml/file_paths.xml",
           "</paths>", '    <cache-path name="helper-update" path="helper-apk/" />\n</paths>')

    home = app / "src/main/java/ctrl/veyracore/MainActivity.kt"
    ui = home.read_text()
    start = ui.index("    fun startRecommendedRoot(context: Context) {")
    end = ui.index('    @JvmName("applyBerryPalette")', start)
    updated = """    fun openInternalRootEngine(context: Context) {
        runCatching {
            context.startActivity(
                android.content.Intent(context, ctrl.mietze.veyraroot.MainActivity::class.java)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onSuccess {
            pendingExternalRootCheck = true
            providerStatus = "Integrierte Veyra-Root-Oberfläche geöffnet."
            log(Severity.INFO, "Veyra Root Engine (intern)", "Native Source innerhalb ctrl.veyracore")
        }.onFailure {
            providerStatus = "Root-Oberfläche konnte nicht geöffnet werden: " + (it.message ?: "Fehler")
            log(Severity.ERROR, "Interne Root Engine", providerStatus ?: "")
        }
    }
    fun startRecommendedRoot(context: Context) = openInternalRootEngine(context)
"""
    ui = ui[:start] + updated + ui[end:]
    ui = ui.replace('InstalledRootTools.known.forEach { tool -> item { InstalledToolPanel(vm, tool) } }',
    """item {
        val embeddedContext = LocalContext.current
        Panel {
            Text("Veyra Root 2.0: Source und lokale Payloads sind in dieser APK enthalten.",
                style = MaterialTheme.typography.bodyMedium)
            Text("Diese Entwicklungsintegration verwendet die native Root-Oberfläche im selben Paket. "
                + "Ein Geräteprofil oder Source-Eintrag beweist noch keine erfolgreiche Root-Ausführung.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { vm.openInternalRootEngine(embeddedContext) },
                modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Security, null)
                Spacer(Modifier.width(8.dp))
                Text("Interne Root-Engine öffnen")
            }
        }
    }""")
    ui = ui.replace("Root-Provider starten", "Interne Root-Engine öffnen")
    ui = ui.replace("Vorhandene Root-App wirklich öffnen", "Native Root-Engine im selben APK öffnen")
    ui = ui.replace("v0.4.0-dev", "v0.5.0-internal")
    ui = ui.replace("VeyraCore 0.4.0-dev", "VeyraCore 0.5.0-internal")
    home.write_text(ui)
    print(f"Source prepared in {ENGINE}")
    print("Engine and native assets present, but helper path compatibility is NOT validated.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--fresh", action="store_true", help="Recreate pinned source checkout")
    parser.add_argument("--build", action="store_true", help="Run Android debug build")
    args = parser.parse_args()
    prepare(refresh=args.fresh)
    if args.build:
        run("bash", "gradlew", ":app:assembleDebug", "--no-daemon", "--console=plain",
            "-x", ":app:lintVitalAnalyzeDebug", cwd=ENGINE)
