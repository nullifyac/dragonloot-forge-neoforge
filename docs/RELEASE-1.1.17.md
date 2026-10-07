# Dragon Loot 1.1.17 - October 7, 2026

This build restores the server's original downward-movement check when deciding
whether a player is hovering. It fixes incorrect flying kicks during long falls
and Slow Falling on affected loaders with `allow-flight=false`. The correction
runs on the server; updating a client alone does not change a dedicated server's
checks. Normal flight permissions, movement acceptance, collision checks and the
hover timeout remain in place.

The cause, older behavior and exact verification scope are explained in
[NETWORK-CONTROLS.md](NETWORK-CONTROLS.md). Each module's changelog uses the same
concise player-facing entry.

## Supported builds

| Minecraft | Pinned loader | Required Java | Production file |
| --- | --- | --- | --- |
| 1.16.5 | Forge 36.2.39 | 8 | `dragonloot-1.1.17-1.16.5-forge.jar` |
| 1.18.2 | Forge 40.2.17 | 17 | `dragonloot-1.1.17-1.18.2-forge.jar` |
| 1.19.2 | Forge 43.3.0 | 17 | `dragonloot-1.1.17-1.19.2-forge.jar` |
| 1.20.1 | Forge 47.3.0 | 17 | `dragonloot-1.1.17-1.20.1-forge.jar` |
| 1.21.1 | NeoForge 21.1.65 | 21 | `dragonloot-1.1.17-1.21.1-neoforge.jar` |

All five builds support client and server. This update retains the 1.1.16 gear,
trident, configuration and integration changes.

## Verification

All five final assemblies exited 0. The ten production/source JARs, checksums and
artifact manifest are staged in the ignored `release/1.1.17/` directory. They
passed resource, Java target, license, listener registration/refmap and test-harness
exclusion checks. Final production payloads match the preserved corrected
`1.1.17-dev` artifacts except version metadata. Source archives additionally
receive the existing, byte-identical module license. All unchanged production
classes and existing refmap entries match the verified 1.1.16 release.

Fresh final packaged dedicated servers passed on all five pins with
`allow-flight=false`: expected loaded versions, startup, datapack/player listing,
normal stop, world save and native Java exit 0. These are lifecycle checks and
do not claim a connected client's gameplay.

The initial 1.16.5 packaged attempt exposed incomplete generated Mixin mappings
after incremental compilation. Main Java compilation on that port now disables
incremental compilation so the processor regenerates the complete refmap and
shadow mappings. Its rebuilt classes and retained mappings were independently
compared with 1.1.16, its six-scenario native suite passed again, and a separate
corrected packaged server passed. Failed artifacts and logs are preserved.

The six movement regression scenarios use the actual server packet handler and
hover timer: accepted descent, illegal stationary hover, the exact vertical
threshold, rejected/pending-teleport movement, native Elytra flight and Dragon
winged flight. Test transport and client motion are simulated in those server
suites; they do not establish full client physics or rendering. All six scenarios
passed on each port: one smoke aggregate on 1.16.5, 29/29 required GameTests on
each of 1.18.2, 1.19.2 and 1.20.1, and 30/30 on NeoForge. Every native process
exited 0; test-report success was checked separately from process exit.

Real Prism client/server comparisons have reproduced the bare-loader failure and
confirmed corrected ordinary falls and Slow Falling on 1.20.1 and 1.21.1.
NeoForge additionally retained the real illegal-hover kick. New client starts
then failed because the Windows display could not supply a monitor
or OpenGL context. The 1.16.5, 1.18.2 and 1.19.2 real client comparisons and the
1.20.1 real hover check remain pending, separate from the native regression suites.

Build all ports from the repository root:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/build-low-memory.ps1 -AllVersions
```

Check staged production servers explicitly:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/compatibility/Test-PackagedServers.ps1 -Profile dragonloot -ModVersion 1.1.17 -ProductionDirectory ./release/1.1.17 -ResultsDirectory ./release/test-results/2026-10-07/release-1.1.17/production-servers
```

## Publication

The 1.1.17 files have not been uploaded to CurseForge. The earlier 1.1.16 files
and tag remain unchanged. The browser upload is pending a usable Windows display;
local release preparation is separate from publication.

Local evidence is retained under `release/test-results/2026-10-07/`:

- `network-regression/final-matrix.json`: all five native movement suites.
- `network-fix/release17-validation.json`: final assemblies, package and payload checks.
- `network-fix/reviewed-network-results.md`: real client comparison scope.
- `network-fix/fg4-mapping-repair/`: the corrected 1.16.5 build and repeated suite.
- `release-1.1.17/production-servers/`: the initial packaged checks, including the preserved 1.16.5 failure.
- `release-1.1.17/production-servers-corrected16/`: the separate final 1.16.5 pass.
