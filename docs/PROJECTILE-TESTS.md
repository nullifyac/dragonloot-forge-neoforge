# Dragon trident regression tests

These tests run Minecraft's real server classes. Their source sets are excluded
from the production and sources JARs. Use Java 8 for 1.16.5, Java 17 for
1.18.2–1.20.1, and Java 21 for 1.21.1. Keep each game JVM at 2 GB and run versions
sequentially on a machine with limited memory.

## Coverage

| Version | Projectile cases | Framework |
| --- | --- | --- |
| 1.16.5 | Three smoke cases grouping release/charge guard, real damage, save/load/Loyalty/pickup | Separate development smoke mod; no vanilla GameTest framework |
| 1.18.2, 1.19.2, 1.20.1 | 19 per version | `DragonTridentGameTests` plus `DragonProjectileBehaviorGameTests` |
| 1.21.1 | 20 | Same two classes, adapted to data components and enchantment effects |

The 1.18.2+ cases cover independent projectile stacks, a ten-tick offhand throw,
a rejected nine-tick throw, an item becoming almost broken during charging,
real damage/recoil, persisted damage/enchantments/name/hit state, Loyalty return
and original-item pickup, natural tick-driven collision, Impaling V on a guardian
and an unaffected pig, wet/dry offhand Riptide, creative non-duplication,
another player's pickup rejection, a full inventory followed by a freed slot,
and the original item dropping when the owner dies. Channeling checks exactly
one lightning bolt for an exposed mob or lightning rod during a thunderstorm,
and zero bolts in dry weather or under a solid roof.

Only 1.21.1 tests a redstone-powered dispenser launching the custom projectile.
Vanilla tridents on the older supported versions use ordinary item dispensing;
the newer dispenser projectile API is not backported by this change.

Fixtures above terrain use the actual world's heightmap. This matters because
GameTest origins can be below ground and previously saved structures can cover
a fixed relative coordinate. Channeling fixtures restore their previous blocks
and weather. These preconditions are asserted before assessing mod behavior.

The baseline also contains four gear tests per 1.18.2+ version, so a full baseline
has 23 tests on the older three versions and 24 on 1.21.1. Optional compatibility
profiles add their own tests; their totals must not be confused with projectile
counts. See [RUNTIME-RESULTS.md](RUNTIME-RESULTS.md) for recorded outcomes.

## Run the baseline

From the repository root, compile or run one selected version:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/build-low-memory.ps1 -MinecraftVersion 1.20.1 -ModVersion 1.1.16-dev -Tasks runGameTestServer
```

For a NeoForge server that does not require client assets, run this from its
module directory with `JAVA_HOME` pointing to Java 21:

```powershell
./gradlew.bat --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx1G' '-Pmod_version=1.1.16-dev' -I ../../scripts/compatibility/headless-server.init.gradle -PdragonlootHeadlessServer=true runGameTestServer
```

The headless option applies only to dedicated development server tasks. Client
launches still need their assets and native libraries.

On this Windows machine, Forge 1.18.2/1.19.2 attached Gradle runs sometimes
reported a disappeared daemon after Minecraft completed and saved its worlds.
The native server exit status is then unresolved. Export the exact Forge launch
and execute it separately to capture that status. From a Forge module with Java
17 selected, substitute its path and the desired version in these commands:

```powershell
./gradlew.bat --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx1G' '-Pmod_version=1.1.16-dev' -I ../../scripts/compatibility/export-gametest-launch.gradle '-PlaunchOutput=../../release/projectile-1.18.2.launch.json' writeGameTestLaunch
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ../../scripts/compatibility/Start-DirectGameTest.ps1 -LaunchFile ../../release/projectile-1.18.2.launch.json -LogPrefix ../../release/projectile-1.18.2
```

For 1.16.5, use the separate development runner and its profile assertions:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./DragonLoot-1.16.5-forge/DragonLoot-1.16/run-smoke-tests.ps1 -Java8Home 'C:/path/to/java8'
```

Record both the `All N required tests passed` message and native exit code 0.
Save `latest.log`, `debug.log`, stdout/stderr and any failure report. A successful
compile or an unavailable exit status is not a runtime pass. Re-run a failed
fixture after correcting its setup; retain the original failed attempt.

## Optional mods and packaged servers

The frozen dependency manifest and profile runners are documented in
[COMPATIBILITY-TESTS.md](COMPATIBILITY-TESTS.md). After preparing that manifest's assets:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/compatibility/Run-CompatibilityProfile.ps1 -Minecraft 1.20.1 -Profile all
```

Dedicated production launches assess packaged JARs separately from development
GameTests. The harness below uses exact official loader installers, validates
installer/library/Mojang SHA-1 metadata, copies the local production DragonLoot
JAR and pinned optional JARs, and uses a fresh isolated localhost server with a
2 GB heap. Cache files are read only. Set `ResultsDirectory` to a new location:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/compatibility/Test-PackagedServers.ps1 -Versions 1.20.1 -Phase Prepare -ResultsDirectory ./release/packaged-server-check
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/compatibility/Test-PackagedServers.ps1 -Versions 1.20.1 -Phase Run -ResultsDirectory ./release/packaged-server-check
```

The default profile is `all`; `-Profile dragonloot` loads only DragonLoot. The
preparation phase needs Python 3.11+ and the verified installer assets produced
by `Prepare-ServerInstallers.ps1`. Preparation does not launch Minecraft. The
run phase requires the unchanged packaged JAR snapshot, waits for `Done`, sends
`gamerule doMobSpawning false`, `datapack list enabled`, `list`, and `stop`, and records saved shutdown plus the
actual Java exit status in `runtime-result.json`. This verifies packaging and
server startup/shutdown; it does not prove client behavior or every optional
mod's gameplay interaction. Natural spawns are disabled to keep flat-world
slimes from interrupting controls; explicitly summoned test targets remain usable.

For a supervised local client connection, select one version and add
`-HoldReady -ServerPort 25621 -OperatorName YourTestProfile -TimeoutSeconds 1800`
to the run command. Connect the matching test client to `127.0.0.1:25621`.
Operator permission is granted after the named player actually joins, using
their connected profile rather than a potentially different cached online UUID.
Use `-AllowFlight` explicitly for a modded-flight test fixture when needed; it
sets the server's flight allowance and is recorded in the result. A successful
run with that setting alone does not establish default `allow-flight=false`
behavior. Assert actual fall-flying state and use a vanilla Elytra control when
assessing a floating kick during scripted airborne teleports or mode changes.
After the server reports ready, append console commands to that isolated
server's `server-commands.txt`; append `stop` after disconnecting. The harness
then records the same saved-shutdown/exit checks. This mode uses an offline
operator profile in the disposable test world only.

## Client reproduction and remaining gaps

Use a fresh test instance with the matching Minecraft/loader pin. Obtain a
Dragon Trident, switch to survival, hold use for at least ten ticks, and release.
One projectile should appear, one durability should be used, and the held item
should leave the inventory until collected. A nine-tick release should keep the
item unchanged. Repeat from the offhand. Give separate tridents Loyalty III,
Riptide III, Channeling I and Impaling V; do not combine mutually exclusive
enchantments when assessing normal gameplay.

For the render check, compare the Dragon and vanilla tridents in first person
and from the side in third person, both idle and charging, in both hands.
Front-facing screenshots can foreshorten the shaft. Check the inventory/ground
icon separately, enchantment glint, and repeat held-item views after `F3+T`.
The icon is expected in inventory views; hands must use the three-dimensional
trident model. Server regressions cannot establish these visual results.

The 1.16.5 automated smoke suite does not yet cover Impaling, wet/dry Riptide,
creative/full-inventory pickup or Channeling's exposed/covered/dry mob cases.
Apply the applicable manual scenarios to that version; lightning rods were not
present in Minecraft 1.16.5. The 1.18.2+ tests directly exercise impact
handlers for enchantment cases while a separate case checks natural collision;
they do not simulate every networked player action, save the whole server
mid-flight, verify audible sounds, or establish client animation/packet timing.
NeoForge Channeling depends on the shipped enchantment data and trident entity
tag; another datapack overriding that definition requires its own verification.
