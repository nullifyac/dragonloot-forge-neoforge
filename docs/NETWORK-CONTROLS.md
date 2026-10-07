# Known upstream issue: flying kicks during long falls

Known issue: affected Forge/NeoForge dedicated servers can kick players during
long falls with `allow-flight=false`, including without Dragon Loot installed.
This is an upstream loader issue, not a Dragon Loot defect. Dragon Loot does not
patch the server's general movement or floating checks.

## Confirmed scope

Fresh real clients connected to dedicated servers with empty client and server
mod directories reproduced the kick on these exact versions:

| Minecraft | Loader | Observation without Dragon Loot |
| --- | --- | --- |
| 1.20.1 | Forge 47.3.0 | An ordinary fall from Y280 was interrupted by a flying kick while still descending. |
| 1.21.1 | NeoForge 21.1.65 | An ordinary fall from Y240 was interrupted by a flying kick while still descending. |

The same compiled movement defect was found on the pinned Forge 36.2.39,
40.2.17 and 43.3.0 servers for Minecraft 1.16.5, 1.18.2 and 1.19.2 respectively.
That is a static finding; fresh loader-only real-client comparisons on those
three versions were not completed. No fixed loader release or complete BMC5 v51
modpack reproduction is claimed.

## Why some sessions work

The affected loader code tests a collision residual in place of the original
vertical movement when deciding whether a player is hovering. It can therefore
mistake continued descent for hovering and kick the player after roughly four
seconds.

Short falls can end before that timeout. Active Elytra-style gliding, including
the winged Dragon chestplate, is exempt from this check. The inspected
singleplayer servers allow flight; dedicated servers configured with
`allow-flight=true` also bypass this kick path. Those successful sessions do not
establish that the affected loader's check is correct. Changing that property
also changes the server's general flight enforcement.

## Project scope and retained evidence

This issue is outside Dragon Loot maintenance. Keep it documented when it affects
testing or player reports; do not add a general loader workaround to the mod.

The experimental 1.1.17 correction was withdrawn before CurseForge publication.
Its tag and investigation remain historical records, not a supported release.
See [the withdrawal record](RELEASE-1.1.17.md).

Existing local evidence remains under the ignored
`release/test-results/2026-10-07/` directory:

- `network-fix/loader-only/`: NeoForge reproduction with empty mod directories.
- `network-fix/1.20.1-forge-loader-only/`: Forge reproduction with empty mod directories.
- `upstream-flight/production-forge/`: installed production-class comparisons.

The earlier Dragon Loot integration controls, including their flight settings,
remain recorded in [RUNTIME-RESULTS.md](RUNTIME-RESULTS.md).
