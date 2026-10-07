# DragonLoot testing

Updated October 6, 2026 after the memory upgrade. Real server regressions,
configuration profiles and Prism client verification cover all five supported
versions. Recorded results and remaining cases are in
[RUNTIME-RESULTS.md](RUNTIME-RESULTS.md); compilation alone does not count as a
runtime pass.

## Preparation results

| Check | Result |
| --- | --- |
| All five mod assemblies | Passed sequentially with each pinned loader, Gradle wrapper and matching JDK. |
| Isolated server regressions | Executed against actual Minecraft servers; expanded projectile, gear and optional integration suites are documented below. |
| All five ports' resource trees | Passed: 437 JSON resources, metadata, mixin class declarations and local model/texture references. |
| Five development JARs and source JARs | Passed: current resources/source, declared mixins, Forge refmaps/manifest registration, correct Java targets, licenses and no game regression classes. |
| Five Prism archives | Prepared with each pinned loader, current JAR, SHA-256 checksum and this test plan. |
| Runtime / visuals / integration | Per-version results and exact coverage are recorded in [RUNTIME-RESULTS.md](RUNTIME-RESULTS.md). |

Every supported port receives the same review and release gate, using its own
Minecraft APIs. Availability of a framework or dependency determines applicable
tests, rather than a preference for the newest version.

| Minecraft | Pinned loader | Gradle | Build JDK | Assembly / GameTest compilation |
| --- | --- | --- | --- | --- |
| 1.16.5 | Forge 36.2.39 | 6.8.3 | 8 | Passed / framework unavailable |
| 1.18.2 | Forge 40.2.17 | 7.6.2 | 17 | Passed / passed |
| 1.19.2 | Forge 43.3.0 | 7.6.2 | 17 | Passed / passed |
| 1.20.1 | Forge 47.3.0 | 8.1.1 | 17 | Passed / passed |
| 1.21.1 | NeoForge 21.1.65 | 8.14 | 21 | Passed / passed |

The recorded October 6 gameplay checks use the local version `1.1.16-dev`.
The source release version is now `1.1.16`; final artifacts and their verified
relationship to those tested builds are described in
[RELEASE-1.1.16.md](RELEASE-1.1.16.md).
Development artifacts are retained in the ignored `release/` directory, named
`dragonloot-1.1.16-dev-<minecraft>-<loader>.jar`, with corresponding
`-sources.jar` and `-prism.zip` files. `SHA256SUMS.txt` covers these local artifacts.

## Build and validate without starting Minecraft

From the repository root on Windows (JDKs 8, 17 and 21, plus Python 3.11+):

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-low-memory.ps1 -AllVersions -ModVersion 1.1.16-dev
foreach ($version in @('1.18.2','1.19.2','1.20.1','1.21.1')) {
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-low-memory.ps1 -MinecraftVersion $version -ModVersion 1.1.16-dev -Tasks compileGametestJava
}
py scripts/validate_resources.py
py scripts/validate_resources.py --version 1.21.1 --jar DragonLoot-1.21.1-neoforge/DragonLoot-1.21/build/libs/dragonloot-1.1.16-dev.jar --prism-output release/dragonloot-1.1.16-dev-1.21.1-neoforge-prism.zip
```

The build helper selects the appropriate installed JDK, limits Gradle to one worker
and a 1 GB heap, and stops its daemon after the build. Its default version is
NeoForge 1.21.1; `-MinecraftVersion 1.20.1` selects Forge. Use `-JavaHome` to choose
a JDK explicitly and `-Offline` after dependencies have been cached. Minecraft
source preparation may launch additional Java processes, so the heap option is
not a limit on the total memory used by all build tools.

The helper defaults to `assemble`, which packages the mod without runtime work.
`-AllVersions` builds sequentially and reports any failing versions after trying
the entire matrix. The second command compiles the isolated game regressions;
1.16.5 predates Minecraft's GameTest framework and uses its isolated development
smoke mod plus Prism client checks instead.
These commands use a new
PowerShell process because this machine's default policy disables `.ps1` files;
it does not change the saved execution policy. Plain Gradle `build` also schedules
NeoGradle's JUnit setup, including game assets/native downloads, so it is not the
preparation command used here.

Use `--version` and `--jar` for each port's own built JAR. `--prism-output` stages
that port with its pinned loader; it refuses to overwrite an existing archive.
The validator checks JSON syntax/duplicate keys, mod metadata, declared mixin
classes, local model/texture references, and optionally the actual JAR resources,
Java target, mixin packaging and license. Forge ports require their declared
refmaps; NeoForge 1.21.1 uses official names and has no refmap declaration.
These checks cannot verify injected methods,
model rendering, networking or other mods' runtime behavior.

## Prism baseline

Import the prepared ZIP using **Add Instance → Import** in Prism. The archive
contains only the locally built DragonLoot mod, an instance manifest and this test
plan. Each version has a separate archive with the repository's pinned loader,
a 512 MB minimum and 2 GB maximum game heap, and automatic Java selection.
Confirm Prism chooses Java 8 for 1.16.5, 17 for 1.18.2–1.20.1 or 21 for 1.21.1.
It still needs Minecraft/loader downloads and your normal launcher login. No
existing instance, account settings or save is modified by preparing the ZIP.

Use a fresh creative world, default resource packs and no shaders. Close the build
before launching the game. Keep the baseline instance and duplicate it separately
for each compatibility case. Run one instance at a time; defer the full BMC5 pack
until the machine has enough memory. Record exact mod/loader versions and retain
`logs/latest.log` and any crash report.

## Runtime matrix

This is the regression checklist, not a claim that every row has passed.
Consult [RUNTIME-RESULTS.md](RUNTIME-RESULTS.md) for completed checks and limits.
Repeat applicable rows on every port. Lightning
rods exist from 1.17 onward, so their Channeling case is unavailable on 1.16.5.
Better Combat has no matching 1.16.5 release; its integration row applies to
1.18.2–1.21.1. The dispenser integration added here uses the 1.21.1 projectile
item API and applies only to that port.

| Case | Steps and expected result |
| --- | --- |
| Startup / reload | Launch standalone. No mixin errors; `/reload` succeeds and recipes are present. |
| Trident throw | Give yourself `dragonloot:dragon_trident`, switch to survival, hold use for at least 10 ticks and release. One projectile spawns, one item leaves inventory, one durability is consumed, and the target takes damage. Repeat from offhand. |
| Trident creative | Throw in creative and walk through the projectile. Inventory stays unchanged; the creative projectile cannot yield a duplicate survival item. |
| Pickup / save | Throw a named, damaged, enchanted trident into a block; save/quit/reopen, then pick it up. Name, damage and enchantments remain intact; it stays a Dragon trident. |
| Loyalty | Enchant with Loyalty III and throw at a block and a mob. It returns to the owner and is picked up once; other players cannot steal it. Repeat with a full inventory, owner death and a relog. |
| Impaling / Channeling | Compare damage against a valid aquatic target with/without Impaling. In a thunderstorm under open sky, Channeling strikes a mob and lightning rod; covered/dry-weather cases do not create lightning. Vanilla tridents must still work. |
| Riptide | In rain/water Riptide launches the player without spawning a projectile. The existing lava allowance also works; it cannot activate while dry outside lava. Repeat from offhand and verify durability goes to the correct hand. |
| Dispenser (1.21.1) | Dispense Dragon tridents; projectiles retain Dragon item identity and effects and can be recovered normally. |
| Trident rendering | Inspect inventory, dropped item, item frame, first/third person and both hands. Inventory/frame/ground use the sprite; held and charging states use the textured 3D model. Reload resources and inspect enchantment glint, including after projectile save/reload. |
| Winged armor | Equipped upgraded chestplate starts and sustains flight; normal Dragon chestplate does not. Landing/water/levitation stops flight normally. Nearly broken armor cannot start flight. Rockets work; wings render correctly. Existing behavior has no passive flight durability drain. |
| Caelus | Add the Minecraft/loader-matching Caelus API. Repeat winged armor tests, Caelus-granted flight and flight denial; then test Icarus and Enigmatic Legacy individually with their dependencies. Record evidence if a crash persists. |
| Better Combat | Add a matching Better Combat build and required dependencies. Sword, axe and trident inherit the standard corresponding attack presets; trident throw/Riptide still work. Verify without fallback name matching if practical. Repeat with custom combat resource packs to check priority. |
| Config startup | In the instance's `config/dragonloot-common.toml`, set mining speed to 39, material damage above the old 20 cap, toughness to 4 and a distinct armor durability. Fully restart; tool behavior, item attributes and armor durability use the edited values. Defaults remain the original balance. |
| Config first run | In a fresh Forge instance, supply a valid `defaultconfigs/dragonloot-common.toml` before first launch. Both the initial gear and generated config use those settings. Existing user config takes priority. |
| Config reload | Change gear settings while running, reload config, and verify gear stays consistent until process restart. Drop chance/anvil settings can reload. Use matching configs on dedicated server and clients; no automatic synchronization is provided. |
| Horse armor | Dragon horse armor grants 18 armor by default; changing `dragon_armor_protection_horse` and restarting changes its defense. |
| Optional perks | With perks off, no pacification or bonus tooltips. With perks on and after restart, Dragon armor pacifies gazing Endermen, Piglins and Phantoms as documented. No tooltip promises bonus tool drops; tooltips translate with Advanced Netherite absent. |
| Advanced Netherite | Add a matching build. Compare actual effective-block mining speed, damage, durability and protection before/after upgrading. Configure Dragon stats explicitly to suit the pack; Advanced Netherite config and bonus drops are not inherited. Test datapack recipes separately. |
| Dedicated server | Start a separate test server with matching configs and connect. Repeat throwing/pickup, anvil sync, flight and save/reload. Check that client rendering classes are not loaded on the server. |
| BMC5 1.21.1 v51 | After minimal cases pass and memory permits, reproduce the reported pack case. Record its actual loader/mod versions rather than assuming they match current standalone versions. |

## Automated server regressions

The `gametest` source set on 1.18.2-1.21.1 is included only in the dedicated
`gameTestServer` run. Minecraft 1.16.5 has a separate `smokeTest` development mod.
Neither harness is packaged in the release or source JARs. See:

- [Projectile suites and direct launch fallback](PROJECTILE-TESTS.md).
- [First-run config and full process restart profiles](CONFIG-PROFILE-TESTS.md).
- [Pinned optional mods and compatibility reproduction](COMPATIBILITY-TESTS.md).
- [Movement handler regressions and real flight-disabled server controls](NETWORK-CONTROLS.md).
- [Minecraft 1.16.5 smoke tests](../DragonLoot-1.16.5-forge/DragonLoot-1.16/SMOKE-TESTS.md).

A normal GameTest invocation is:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\build-low-memory.ps1 -MinecraftVersion 1.20.1 -Tasks runGameTestServer
```

Each game server has a 2 GB maximum heap. Run versions sequentially on constrained
machines and retain actual test results, logs and process exit codes. On this
machine, ForgeGradle 5/Gradle 7.6.2 can report a disappearing daemon after a clean
1.18/1.19 server shutdown. An exported direct Java launch distinguishes this build
wrapper failure from gameplay failure; it must still assert all required tests
and exit successfully.

NeoGradle's client asset download HEAD request stalled on this network. The
opt-in headless initializer skips client assets/natives for server runs only;
Prism client testing still downloads and uses the real assets. This distinction
is documented in the compatibility instructions.

## Packaged dedicated servers

These checks use the production JARs in `release/`, official pinned loader
installers and the frozen optional-mod profiles. From the repository root:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/compatibility/Prepare-CompatibilityMods.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/compatibility/Prepare-ServerInstallers.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/compatibility/Test-PackagedServers.ps1
```

The default selects `1.1.16-dev` JARs from `release/`. To check staged final
release JARs, add `-ModVersion 1.1.16 -ProductionDirectory ./release/1.1.16`.
When using separate `-Phase Prepare` and `-Phase Run` commands, retain those same
values and the exact `-ResultsDirectory` for both phases.
The runner creates isolated servers for all five versions, binds them to loopback,
checks the loaded versions and `Done` message, requests datapack/player listings,
then stops and saves each server. It records native exit codes and JAR hashes.
`-Profile dragonloot` selects a standalone mod server; `-Java8Home`, `-Java17Home`
and `-Java21Home` override the local JDK defaults. Startup and shutdown checks
have a separate scope from an actual connected client's gameplay; see
[RUNTIME-RESULTS.md](RUNTIME-RESULTS.md) for both outcomes. The recorded NeoForge
default-flight control failure and loader investigation are in
[NETWORK-CONTROLS.md](NETWORK-CONTROLS.md).

## Release gate

Mark each applicable row with exact versions and observations. Only publish a
release after baseline, changed functionality and dedicated-server cases pass.
Keep a separate result for every supported version; a newer port passing does not
clear the older ports for release. Runtime testing uses separate dated instances and generated test worlds.
Preserve existing launcher accounts, instances and saves. Testing does not
publish a release.
