# Security policy

VeyraCore is an experimental Android root-management workspace. The current preview does not ship a kernel exploit, native privilege-escalation daemon or boot-image patcher. It can launch separate installed root applications only after the user taps them.

## Reporting problems

Please open a private security advisory through the repository's **Security** tab for a vulnerability involving app permissions, shell command construction, module-ZIP validation, package visibility, or logs. Avoid publishing passwords, signing keys, authentication tokens, full personal device identifiers, private build fingerprints or sensitive log files in public issues.

## Security boundaries

- UID 0 must be verified by live process output, never inferred from installed package names, a certificate or System UID 1000.
- Root actions should be explicit, bounded and observable.
- Module archives are untrusted and must be inspected before any compatible privileged installer handles them. A clean ZIP check does not make an unknown module safe.
- Root-provider profiles require exact manufacturer, model, firmware, kernel, ABI, page size and security-patch verification.
- Original Veyra Root, ZF6Root and Root My Galaxy Next are separate applications with independent risk and authorization contexts.

Do not describe experimental ports as universally supported. Keep a working recovery procedure before testing native root tooling on a physical device.
