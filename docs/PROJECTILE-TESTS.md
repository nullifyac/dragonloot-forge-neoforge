# Dragon trident regression tests

These development tests exercise real Minecraft server classes. Test classes and
metadata are excluded from production and sources JARs. Use Java 8 for 1.16.5,
17 for 1.18.2-1.20.1 and 21 for 1.21.1; run the 2 GB test JVMs sequentially.

## Coverage

| Minecraft | Projectile coverage | Harness |
| --- | --- | --- |
| 1.16.5 | Three grouped smoke cases | Separate development smoke mod |
| 1.18.2, 1.19.2, 1.20.1 | 19 cases per port | Two projectile GameTest classes |
| 1.21.1 | 20 cases | GameTests adapted to components/enchantment effects |

The newer suites cover independent projectile stacks, offhand throwing,
ten-tick charge acceptance, nine-tick rejection, the almost-broken guard,
real damage/recoil, persisted names/damage/enchantments/hit state, Loyalty,
original-item pickup and natural collision. They also check Impaling on affected
and unaffected mobs, wet/dry offhand Riptide, creative non-duplication,
other-player pickup rejection, full inventory recovery and owner-death drops.

Channeling cases require exactly one bolt for an exposed mob or lightning rod
in a thunderstorm and none in dry weather or under a solid roof. Fixtures assert
sky access using the world's heightmap and restore previous blocks/weather.
Only 1.21.1 covers custom projectile launching from a redstone-powered dispenser;
the newer projectile-item API is not backported to older ports.

Four gear tests accompany each newer baseline, giving 23 required baseline cases
on Forge 1.18.2-1.20.1 and 24 on NeoForge 1.21.1. Optional profiles add their own
cases; do not equate total suite counts with projectile coverage.

## Run a baseline

From the repository root:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/build-low-memory.ps1 -MinecraftVersion 1.20.1 -ModVersion 1.1.16-dev -Tasks runGameTestServer
```

Use another supported 1.18.2+ version, or `-Tasks compileGametestJava` for a
compile-only check. For 1.16.5 use the [smoke runner](../DragonLoot-1.16.5-forge/DragonLoot-1.16/SMOKE-TESTS.md).

A server-only NeoForge launch can skip client assets/native extraction. From
`DragonLoot-1.21.1-neoforge/DragonLoot-1.21`, with `JAVA_HOME` set to a Java 21 JDK:

```powershell
./gradlew.bat --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx1G' '-Pmod_version=1.1.16-dev' -I ../../scripts/compatibility/headless-server.init.gradle -PdragonlootHeadlessServer=true runGameTestServer
```

The opt-in headless initializer applies only to server tasks. Client runs still
require their assets/native libraries.

## Capture the native exit separately

If an attached Gradle run does not provide a reliable native server exit, export
the prepared launch and run it directly. From
`DragonLoot-1.18.2-forge/DragonLoot-1.18`, with `JAVA_HOME` set to a Java 17 JDK:

```powershell
./gradlew.bat --no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx1G' '-Pmod_version=1.1.16-dev' -I ../../scripts/compatibility/export-gametest-launch.gradle '-PlaunchOutput=../../release/test-results/projectile-1.18.2.launch.json' writeGameTestLaunch
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ../../scripts/compatibility/Start-DirectGameTest.ps1 -LaunchFile ../../release/test-results/projectile-1.18.2.launch.json -LogPrefix ../../release/test-results/projectile-1.18.2
```

Substitute the matching module/JDK and output names for another port. Require
both `All N required tests passed` and native Java exit 0. Keep Minecraft logs,
stdout/stderr and failure reports. A passing compile or missing exit status does
not establish runtime success.

## Client and production checks

Use [optional integration profiles](COMPATIBILITY-TESTS.md) and
[packaged dedicated-server testing](TESTING.md) to assess dependency interactions,
reobfuscation and server-side class stripping separately from development tests.

In a fresh client, throw from both hands in survival and creative, test charge
boundaries and use separate Loyalty, Riptide, Channeling and Impaling tridents.
Do not combine mutually exclusive enchantments when testing ordinary gameplay.
Check pickup and save/reload of a named, damaged, enchanted item.

Compare held and charging models in first/third person and both hands, including
a side view. Inventory, ground and frame views use the sprite; held states use
the 3D model. Check projectile rendering and glint, then repeat after `F3+T`.
Server tests cannot establish these visuals, animation or packet timing.

The 1.16.5 automated smoke suite does not cover Impaling, wet/dry Riptide,
creative/full-inventory pickup or exposed/covered/dry Channeling. Test those
manually where applicable; 1.16.5 has no lightning rods. Newer enchantment tests
invoke impact handlers while a separate case checks natural collision; they do
not simulate every client action or save the entire server mid-flight.
NeoForge Channeling uses shipped enchantment data and the trident entity tag;
resource/datapack overrides need their own checks.
