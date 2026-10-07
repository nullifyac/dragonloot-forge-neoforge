# Configuration startup and restart tests

The gear regressions use registered item attributes and real Minecraft server
APIs. They cover mining speed, sword damage, tool/armor durability, toughness,
protection, horse armor, enchantability, native flight gates and passive wear.
Reload assertions check that runtime scale settings change while startup gear
values remain frozen until a process restart.

## Run the profiles

From the repository root, replace the JDK placeholders:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/test-gear-config-profiles.ps1 -Java17Home 'C:/path/to/jdk17' -Java21Home 'C:/path/to/jdk21' -ResultsDirectory ./release/test-results/config-profiles
```

This selects Forge 1.18.2, 1.19.2 and 1.20.1, and NeoForge 1.21.1. Pass
`-Versions 1.20.1` to select one port. Runs are sequential and use a 2 GB server
heap and two worker CPUs; Gradle preparation uses one worker and a 768 MB heap.
Avoid concurrent builds of the same module. The server-only launch exporter is
opt-in and leaves ordinary builds/client launches unchanged.

Forge 1.16.5 predates GameTest. Its corresponding three-profile harness is
documented in [SMOKE-TESTS.md](../DragonLoot-1.16.5-forge/DragonLoot-1.16/SMOKE-TESTS.md).

## Profile behavior

Each version gets an isolated generated configuration directory. The first
launch provides only `defaultconfigs/dragonloot-common.toml`, with the managed
config absent. The next launch edits the generated managed config and restarts
that configuration directory while preserving the original default file.
Each launch uses a fresh GameTest world so earlier structures cannot contaminate
sky-access fixtures.

`gear-expectations.properties` contains explicit expected outcomes independent
of the mod's config/material getters. Representative runner inputs are:

| Setting or registered outcome | First launch | Edited config after restart |
| --- | --- | --- |
| Mining speed | 41 | 43 |
| Material attack damage | 25 | 29 |
| Sword attack modifier | 28 | 32 |
| Tool durability | 4020 | 4087 |
| Forge chest durability | 1750 | 1785 |
| NeoForge chest durability | 800 | 816 |
| Chest protection | 17 | 19 |
| Toughness | 6 | 7 |
| Horse protection | 23 | 25 |
| Tool enchantability | 70 | 80 |
| Minimum scale drops | 5 | 6 |

These are test inputs, not production defaults. The damage/enchantability inputs
also exercise values above the former configuration limits.

## Result checks and scope

The runner exports the build tool's prepared Java launch and executes it directly
to record the native exit status. Java argument files handle Windows command-line
limits; exported environment entries are limited to loader configuration.
Success requires Java exit 0, all required GameTests passing, expected output
artifacts and preservation of the original default config. Logs/reports are
moved aside before each launch so stale success messages cannot pass a rerun.

The selected results directory contains expected values, actual configuration,
launch metadata, logs, source hashes and JSON summaries. Failed preparation and
runtime exits remain available; previous reports are retained in `history`.
The isolated generated worlds are independent of existing gameplay worlds.

These tests establish server configuration lifecycle and gear behavior. They do
not establish client rendering, automatic client/server config synchronization,
or optional-mod compatibility. Use [integration tests](COMPATIBILITY-TESTS.md)
and the [client checklist](TESTING.md) for those cases.
