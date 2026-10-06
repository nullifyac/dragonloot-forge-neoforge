# Configuration startup and restart tests

These development runs start real dedicated GameTest servers for Forge 1.18.2,
1.19.2 and 1.20.1, and NeoForge 1.21.1. The separate gear tests check actual
registered item attributes, durability, stone mining speed, horse protection,
enchantability, native flight gates, continuous flight without passive wear,
and runtime scale reloads while gear remains frozen.

From the repository root:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/test-gear-config-profiles.ps1
```

Pass `-Versions 1.18.2` to select one version. Use `-Java17Home` and `-Java21Home`
when the installed JDKs differ from the local defaults. Coordinate with any other
test process compiling the same module. Runs are sequential, limited to a 2 GB
server heap and two worker CPUs; preparation uses one Gradle worker and a 768 MB
heap. The server-only launch-export init script is explicitly opt-in and leaves
normal builds and client runs unchanged.

Each version starts in a fresh directory under its ignored `run` folder. The
first launch supplies only `defaultconfigs/dragonloot-common.toml`, with the
managed config absent. The second launch edits the managed config and restarts
the same isolated configuration directory, retaining the original default config.
Each launch uses a fresh GameTest world so saved test structures cannot block
later sky-access fixtures. Matching
`gear-expectations.properties` files contain explicit expected outcomes, rather
than deriving expectations from the mod's config/material getters.

| Setting / registered outcome | First run | Full restart |
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

The selected damage and enchantability values exceed the previous upper bounds.
These are test inputs; production defaults remain unchanged.

The runner exports the exact Forge/NeoForge-generated Java launch and starts it
independently to capture its native exit status. Java argument files avoid
Windows command-line limits; only loader-related environment entries are
exported. A successful profile requires exit code 0 and the GameTest server's
all-required-tests-passed summary, required output artifacts and preservation of
the original default config. Prior logs are moved out of the run directory so a
later launch cannot reuse an earlier success summary. New attempts begin with a
pending report, and prepare/runtime exit codes are retained even on failure.
Expected values, actual config, launch
metadata, logs, source hash and JSON reports/manifests are retained beneath
`release/test-results/2026-10-06/config-profiles`. `-ResultsDirectory` can preserve
a separate run. Previous profile logs are copied to `history` before a rerun.
Existing client/server worlds are never used.

The exporter accounts for the tested build tools' deferred launch configuration:
[ForgeGradle 6 assembles the RunConfig inside its execution action](https://github.com/MinecraftForge/ForgeGradle/blob/FG_6.0/src/common/java/net/minecraftforge/gradle/common/util/runs/MinecraftRunTask.java),
and [Gradle 8.14 applies lazy JVM arguments at execution](https://github.com/gradle/gradle/blob/v8.14.0/platforms/jvm/language-java/src/main/java/org/gradle/api/tasks/JavaExec.java).
It reuses ForgeGradle's token generator and includes the resolved lazy argument
list; it exports only the dedicated server run.

Forge 1.16.5 has no native GameTest engine. Its corresponding real-server harness
and three-profile runner are documented in
[SMOKE-TESTS.md](../DragonLoot-1.16.5-forge/DragonLoot-1.16/SMOKE-TESTS.md).
Graphical rendering and optional compatibility mods require their own checks.
