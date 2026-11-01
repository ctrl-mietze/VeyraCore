# Port registry

| Boundary | Current endpoint | State |
|---|---|---|
| Android app | `ctrl.veyracore` | Implemented |
| Veyra Load Unix socket | `/dev/socket/veyra_loadd` | Probe-only; future daemon contract |
| Existing root command | `su -c` | Used only for fixed, bounded commands |
| Module directory | `/data/adb/modules` | Read/marker-file adapter after UID-0 verification |

There are no listening TCP/UDP ports, localhost services, web APIs, ADB servers, or exported app services in v0.1.0.
