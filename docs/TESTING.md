# Testing Dragon Loot

Build and test each supported port with its own loader and JDK. The repository's
PowerShell helpers target Windows; the module Gradle wrappers can also be used
with the matching JDK on other systems.

| Minecraft | Loader pin | JDK | Development server tests |
| --- | --- | --- | --- |
| 1.16.5 | Forge 36.2.39 | 8 | Separate smoke-test mod |
| 1.18.2 | Forge 40.2.17 | 17 | GameTest |
| 1.19.2 | Forge 43.3.0 | 17 | GameTest |
| 1.20.1 | Forge 47.3.0 | 17 | GameTest |
| 1.21.1 | NeoForge 21.1.65 | 21 | GameTest |

## Build and validate

From the repository root, with the required JDKs and Python 3.11+ installed:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/build-low-memory.ps1 -AllVersions -ModVersion 1.1.16-dev
py -3 scripts/validate_resources.py
py -3 scripts/validate_resources.py --version 1.21.1 --jar DragonLoot-1.21.1-neoforge/DragonLoot-1.21/build/libs/dragonloot-1.1.16-dev.jar
```

The helper defaults to `assemble`, selects installed matching JDKs, uses one
Gradle worker and a 1 GB Gradle heap, and builds ports sequentially. Use
`-MinecraftVersion` for one port, `-JavaHome` to select its JDK, or `-Offline` once
dependencies are cached. Replace the development version when testing another
version. Source preparation can start additional Java processes.

The validator checks resource JSON, metadata, local asset references and declared
Mixin classes. With `--jar`, it also checks packaged resources, Java targets,
Mixin registration/refmaps, license inclusion and exclusion of test harnesses.
These checks do not execute Mixins or verify gameplay or rendering. Forge builds
use refmaps; NeoForge 1.21.1 uses official names.

## Development server suites

Run a selected 1.18.2+ suite, or use `-Tasks compileGametestJava` to compile it
without starting Minecraft:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/build-low-memory.ps1 -MinecraftVersion 1.20.1 -ModVersion 1.1.16-dev -Tasks runGameTestServer
```

GameTest and smoke-test sources are excluded from production and sources JARs.
Test JVMs use a 2 GB heap cap. Require a passing assertion report and a successful
native Java exit; compilation or server startup alone is insufficient.

- [Projectile coverage and direct launch](PROJECTILE-TESTS.md).
- [Configuration first-run and restart profiles](CONFIG-PROFILE-TESTS.md).
- [Pinned optional integrations](COMPATIBILITY-TESTS.md).
- [1.16.5 smoke tests](../DragonLoot-1.16.5-forge/DragonLoot-1.16/SMOKE-TESTS.md).

## Packaged dedicated servers

Test production JARs separately to catch reobfuscation, side stripping and
packaging failures. The helper expects filenames of the form
`dragonloot-<modversion>-<minecraft>-<loader>.jar`. For example, after building
1.20.1 from the command above with `-Tasks assemble`:

```powershell
New-Item -ItemType Directory -Force ./release/server-jars | Out-Null
Copy-Item ./DragonLoot-1.20.1-forge/DragonLoot-1.20/build/libs/dragonloot-1.1.16-dev.jar ./release/server-jars/dragonloot-1.1.16-dev-1.20.1-forge.jar
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/compatibility/Prepare-ServerInstallers.ps1 -AssetRoot ./release/test-assets
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/compatibility/Test-PackagedServers.ps1 -Versions 1.20.1 -Profile dragonloot -ModVersion 1.1.16-dev -ProductionDirectory ./release/server-jars -AssetRoot ./release/test-assets -ResultsDirectory ./release/test-results/packaged-server -Java17Home 'C:/path/to/jdk17'
```

Replace the JDK placeholder. Use `-Java8Home` or `-Java21Home` for those ports.
The helper verifies official installer/library hashes, creates an isolated
loopback server, checks loaded versions and `Done`, requests console listings,
then stops and saves it. It reads existing build/launcher caches when preparing
server libraries. Split preparation and execution with `-Phase Prepare` and
`-Phase Run`; keep the same paths and version for both phases.

For optional mods, prepare the [compatibility assets](COMPATIBILITY-TESTS.md) and
use `-Profile all` with the same asset root. Startup/shutdown reports establish
packaging and lifecycle behavior, not connected-client gameplay.

To connect a client, select one version and add `-HoldReady -ServerPort 25621
-OperatorName TestPlayer`, replacing `TestPlayer` with the player's actual name.
Join `127.0.0.1:25621` with a matching fresh test client;
append console commands, including `stop` when finished, to the generated
`server-commands.txt`. This mode uses an offline profile in the disposable world.

## Client checks

Use a fresh instance with the pinned loader, default resource packs and no
shaders. To create a Prism import ZIP, add
`--prism-output release/dragonloot-1.21.1-prism.zip` to the validator's JAR command;
it refuses to overwrite an existing ZIP. Import it in Prism and select the JDK
from the table. Minecraft assets, loader downloads and launcher authentication
remain normal launcher requirements.

Check the following on every applicable port:

- Trident use in survival and creative, both hands, short-charge rejection,
  pickup, save/reload, Loyalty, Impaling, Channeling and wet/dry Riptide.
- Trident inventory sprite, held/charging 3D model, projectile rendering and glint
  in first and third person, including after resource reload.
- Native winged-chestplate eligibility, rockets, landing, water, levitation,
  almost-broken armor and absence of passive flight wear. Ordinary Dragon chest
  armor must not grant flight by itself.
- Configured gear after a full process restart, runtime scale/anvil settings,
  horse protection, recipes, anvil synchronization and optional armor perks.
- Applicable optional integrations and a connected dedicated-server session.

Lightning rods are unavailable on 1.16.5. Better Combat profiles start at 1.18.2;
custom projectile dispensing applies only to 1.21.1. Follow the detailed guides
for those boundaries. Keep exact versions, assertion reports, native exits and
logs for each run; each port needs its own validation.
