# VeyraCore Design System

## Visual foundation

VeyraCore borrows **design direction** from the user's Veyra Root (real AMOLED black and violet) and Raspberry Manager (distinct raspberry-red emphasis). It does not copy assets or third-party source.

| Token | OLED | Berry |
|---|---|---|
| Canvas | #000000 | #100C14 |
| Cards | #080808 | #1B1520 |
| Primary | #A855F7 | #E84A77 |
| Secondary | #D2A6FF | #B17DF6 |
| Main text | #F8F5FB | #F8F5FB |
| Confirmed state | #83DDAC | #83DDAC |
| Unknown / needs action | #F6C86F | #F6C86F |

## Layout and interactions

- Native Material 3 Compose, dark-only product identity, top device metrics and home root summary.
- 20dp rounded card containers, subdued outlines, no transparency gimmicks, no blinding gradients.
- A persistent bottom navigation rail-like selection for Home, Settings, Logs; only expose Modules and Superuser with verified root.
- Responsive scrollable pages, natural typography, touch targets at least 44dp, buttons that either work or clearly declare unavailable status.
- Setup step colors reflect actual checks. Certificate acceptance, soft reboot and UID0 remain independent states.
- Long device build fingerprints may wrap or be abbreviated with an explicit details view; never make screen horizontal scrolling necessary.
- Use the original adaptive VeyraCore icon. Any future flower/brand art must be original or user-supplied.

## User trust

GhostLock and DirtyFrag cards are **informational until a verified provider exists**. A green state indicates evidence, not intent. More detailed diagnostic logs are exported to Android Downloads; no background upload.
