# VeyraCore — Visual Language

> A product UI, not a disguised kernel manager. Calm controls, accurate state, deep blacks and a restrained Veyra signature.

The second-generation VeyraCore takes the distinct violet accent and soft, rounded surfaces of Veyra Root, the dense but legible VX Core status cards, and the raspberry/berry color vocabulary of Raspberry Manager. It does not reproduce another project's UI or stock KernelSU navigation.

## Named themes

| Semantic token | Veyra OLED | Veyra Berry | Usage |
| --- | --- | --- | --- |
| Canvas | `#000000` | `#100B13` | True-black OLED canvas / warm charcoal |
| Main surface | `#0C0A10` | `#1A131E` | Panels, menus and bottom rail |
| Raised surface | `#18131F` | `#2A1C2D` | Selected methods, dialogs |
| Accent | `#B777FF` | `#FF6F9A` | Active navigation, primary CTA |
| Secondary | `#D5B5FF` | `#C9A4FF` | Supporting actions and accents |
| Text | `#F8F5FB` | `#FFF7FB` | Primary copy |
| Secondary text | `#C9BDCE` | `#D9C3D3` | Metadata, subtitles |
| Border | `#4A3C54` | `#594152` | 1 dp outlined cards |
| Verified | `#83DDAC` | `#83DDAC` | Verified UID 0 / success |
| Needs attention | `#F6C86F` | `#F6C86F` | Not yet checked / pending |
| Error | `#FF8E9D` | `#FF8E9D` | Incompatible / failed |

The OLED palette is intentionally truly black, with minimal violet surfaces and a distinctly lilac primary hierarchy. Berry's brighter accessible UI tokens derive from Raspberry Manager's original `PiPink #E83A67` and `PiPurple #B078EB`; a little purple gives contrast rather than turning the app into a fully red dashboard. Both schemes retain identical geometry and status semantics.

## Architecture of the screen

**Home**: compact top brand with genuine device status; prominent Root / Re-Root actions; live verified UID indicator; device fingerprint and kernel separated from verified compatibility.

**Root**: named cards for GhostLock, DirtyFrag and VX Cert / Reroot, grouped by proven capabilities. Method choice does not imply device compatibility. Detailed requirements include exact fingerprint, full kernel version, ABI, memory page size, KMI, payload provenance and verified build hash.

**Settings**: appearance, diagnostic and explicit experimental controls. Persist the chosen theme. Never hide the uncertainty about Root inside settings.

**Logs**: chronological, monospace excerpts with severity and direct MediaStore Downloads export. The log is human-readable and contains the actual report location.

**Modules**: only after an independently verified UID-0 response. Module name, id, author, version and enabled state have separate visual weight. A ZIP preview is never treated as successful installation.

**Superuser**: only after UID 0; distinguish app UID, Android system UID 1000 and ADB shell UID 2000; a connection is not permission elevation. Grant/Deny cannot appear functional before a real policy backend exists.

## Layout rules

- Safe-area insets apply everywhere, including rotated devices. Controls must never be drawn behind Android navigation bars.
- On compact width, use measured/adaptive bottom controls with short labels; do not force six long tabs into equally tiny cells. On wide width, navigation rail and expanded content are preferred.
- 20 dp outer horizontal margin, 12–16 dp vertical section rhythm, 16–20 dp card padding and roughly 18–22 dp corner radius; avoid deeply stacked nested cards.
- Header information can wrap on a narrow device. Full kernel fingerprints use selectable/copyable monospace text rather than clipped claims.
- Positive states are green only on direct evidence. All untested or not applicable exploit routes remain neutral/amber, never green.
- Typography remains a familiar Android system/Material3 family; no unlicensed font dependencies.
- Motion is subtle: a quick fade/size transition for page selection and a determinate busy indicator during verification, with no animated “root succeeded” without real evidence.
- Native Android affordances take priority over gradients and decorative glass. A Veyra flower mark may use the project's own original licensed source asset, not an unrelated stand-in.

## Evidence-to-color contract

`UNKNOWN` is not `FAILED`. `AVAILABLE` is not `SUPPORTED`. `SUPPORT_PROFILE_MATCH` is not `ROOT_VERIFIED`. Only a successful live `uid=0` result can display “Root verified”.

Original VeyraCore architecture and visual research were observed in the owner's local sources. References to KernelSU are strictly technical interoperability; downstream licenses and credits remain mandatory for any imported third-party implementation.
