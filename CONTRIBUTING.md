# Contributing to VeyraCore

VeyraCore is a first-party Android/Kotlin Compose project focused on an independent interface, diagnostics and an honest root-provider boundary.

## Development workflow

1. Create a focused feature branch and keep unrelated Android projects untouched.
2. Run `./gradlew testDebugUnitTest assembleDebug --no-daemon` with a configured Android SDK and JDK 17.
3. Keep the existing UID-0 proof and per-action permission checks intact.
4. For a device profile, provide manufacturer, model, full build fingerprint, exact `uname -r`, ABI, page size, Android security patch and device-specific reproducible test evidence.
5. Document failed tests, soft reboot versus cold boot, and the recovery method. Do not mark success without independent verification.
6. Include license and attribution notices for any third-party source. Rebranding upstream code or drivers never erases the original license or authors.

## Interface principles

Keep Veyra OLED black and violet, Veyra Berry dark raspberry and rose. Use native Android Compose controls, accessible touch targets, responsive portrait/landscape layouts, and clear distinction between unknown, unsupported and verified states.

## Scope of a pull request

Independent components are welcome: logging, device inspection, module metadata, profile validation, tests, UI components, diagnostic export, and explicit external-app handoffs. Native exploit and privileged kernel changes require separate technical and license reviews; do not place unverified binaries or private signing material in the repository.
