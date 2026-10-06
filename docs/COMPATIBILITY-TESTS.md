# Optional integration verification

The test assets are pinned to official upstream downloads. These optional mods are not mandatory Dragon Loot dependencies. `scripts/compatibility/manifest.json` records the exact project/version IDs, official project and source links, required dependency closure, and SHA-1/SHA-512/SHA-256 hashes of all 42 selected JARs. Downloaded JARs, generated worlds, launch arguments, and logs stay in the ignored `release/test-results/2026-10-06/compatibility` directory.

## Pinned combinations

Versions below are the versions reported by the actual JAR metadata and checked against the runtime mod list. A project page's version label can differ from its JAR metadata.

| Minecraft / loader | Better Combat | Caelus | Advanced Netherite | Enigmatic Legacy | Icarus |
| --- | --- | --- | --- | --- | --- |
| 1.16.5 / Forge 36.2.39 | No official matching build | 1.16.5-2.1.3.2 | 1.12.4 | 2.11.12 | No official matching build |
| 1.18.2 / Forge 40.2.17 | 1.6.2+1.18.2 (upstream beta) | 1.18.1-3.0.0.2 | 1.14.7 | 2.25.0 | No official matching build |
| 1.19.2 / Forge 43.3.0 | 1.7.1+1.19 (upstream beta) | 1.19.2-3.0.0.6 | 1.14.7 | 2.26.5 | No official matching build |
| 1.20.1 / Forge 47.3.0 | 1.9.0+1.20.1 | 3.2.0+1.20.1 | 2.1.3 | 2.30.1 | 2.14.1 |
| 1.21.1 / NeoForge 21.1.65 | 2.4.0+1.21.1 | 7.0.1+1.21.1 | 2.3.1 | No official matching build | 4.5.0 |

The combined profiles also include the following required upstream dependencies. The lock contains their individual provenance and hashes.

| Minecraft | Additional external dependencies |
| --- | --- |
| 1.16.5 | Curios 1.16.5-4.1.0.0; Patchouli 1.16.4-53.3 |
| 1.18.2 | Curios 1.18.2-5.0.9.2; Patchouli 1.18.2-71.1; Cloth Config 6.5.102; Player Animator 1.0.2+1.18 |
| 1.19.2 | Curios 1.19.2-5.1.6.4; Patchouli 1.19.2-77; Cloth Config 8.3.103; Player Animator 1.0.2 |
| 1.20.1 | Curios 5.14.1+1.20.1; Patchouli 1.20.1-85-FORGE; Cloth Config 11.1.136; Player Animator 1.0.2-rc1+1.20; Common Network 1.0.6-1.20.1; Resourceful Config 2.1.3 |
| 1.21.1 | Curios 9.5.1+1.21.1; Cloth Config 15.0.140; Player Animator 2.0.4+1.21.1; Common Network 1.0.21-1.21.1; Resourceful Config 3.0.11; Sparkweave 0.508.0 |

Icarus 4.5.0 and Sparkweave 0.508.0 were selected for the repository's exact NeoForge 21.1.65 pin. Newer inspected releases require newer NeoForge loaders. A matching Minecraft version alone does not establish loader compatibility. Some Enigmatic Legacy API dependency records omit dependencies declared mandatory inside their JARs; the frozen profiles include those dependencies. The selected 1.16.5 Enigmatic Legacy 2.11.12 does not contain the newer Majestic Elytra/back-slot feature and does not require Caelus itself. Its startup and Dragon Loot's own native flight are tested with Caelus loaded; an unavailable external-wing feature is not claimed as tested there.

## Reproduction

From the repository root in PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/compatibility/Prepare-CompatibilityMods.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/compatibility/Stage-CompatibilityProfile.ps1 -Minecraft 1.20.1 -Profile all
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/compatibility/Run-CompatibilityProfile.ps1 -Minecraft 1.20.1 -Profile all
```

Repeat the last command with `1.16.5`, `1.18.2`, `1.19.2`, and `1.21.1`. The runner accepts `-JavaHome` for a Java 8/17/21 JDK installation and `-AssetRoot` for another isolated test directory. Java 8 is required for 1.16.5, Java 17 for 1.18.2–1.20.1, and Java 21 for 1.21.1. Available individual profiles are `caelus`, `better-combat`, `advanced-netherite`, `enigmatic-legacy`, and `icarus`, where an official matching build exists.

The downloader checks all three recorded hashes. Staging rejects unexpected JARs instead of copying every cached download. The opt-in Gradle init script checks the module's exact Minecraft/loader pins, verifies SHA-256, attaches Forge dependencies through `fg.deobf`, isolates run directories, and passes the expected actual mod versions into the tests. Main build files retain their ordinary dependencies.

The runner limits the test JVM to 2 GiB, uses one Gradle worker and a 1 GiB Gradle heap, and refuses to start when less than 4 GiB physical RAM is free. It holds a per-module runner lock and creates a fresh timestamped working directory so previous generated configuration/worlds cannot contaminate a new profile. Coordinate other Gradle/client processes separately. On Forge 1.18.2–1.20.1 it exports the exact prepared JavaExec launch and runs Java directly so a test's native exit status remains visible. The exporter resolves ForgeGradle 6's deferred run configuration through its own token helpers. On NeoForge it retains NeoGradle's normal JavaExec execution, which resolves its lazy JVM arguments. The headless init script disables client-only asset/native actions only for explicitly requested server runs with the inspected NeoGradle 7.1.20 task layout.

Each run writes timestamped stdout/stderr or Gradle logs and a native exit-code file. The 1.16.5 runner also requires `smoke-report.json` to report no failed assertions: normal server shutdown alone gives Java exit 0 even when the smoke harness records an assertion failure. Preserve failed attempts. Existing Prism instances, accounts, and user worlds are not modified by these helpers.

The runner calls `Get-CompatibilityResult.ps1` after execution. It checks native/preparation exit codes, actual pinned mod-version markers, required-case counts, Better Combat registry evidence where applicable, and every external grant/removal control where a provider exists. It writes `.verified-result.json`; running that verifier against the five final captures also produces the ignored `combined-matrix.json`. Missing tests or an empty successful launch cannot silently pass this guard.

## Assertions and limits

Optional checks require the exact pinned mod IDs and actual versions to be loaded. Better Combat's live server weapon registry must resolve the dragon sword, axe, and trident to their intended categories, attack combos, and a valid range. The NeoForge 2.4 API uses `rangeBonus`; earlier tested APIs use `attackRange`.

Gear regressions verify registered/configured attributes, mining speed, horse defense, frozen startup configuration, flight eligibility gates, and sixty real player ticks of winged-chestplate flight without passive armor wear. Projectile suites cover throwing, pickup and persisted loyalty/projectile behavior. These assertions run with all selected optional mods loaded.

Additional Curios checks on 1.18.2–1.21.1 equip the actual registered upstream wing item in a real Curios back slot and require its Caelus flight attribute to reach the provider's eligibility threshold. Empty-chest and vanilla-armor controls accompany ordinary Dragon armor. A Forge/NeoForge FakePlayer supplies a real ServerPlayer and its inherited `doTick` runs the actual player tick; its ordinary `tick` method is intentionally empty. The tests invoke the actual Caelus server request handler with a synthetic sender context and maintain forty native flight ticks. Removing the Curio must restore the captured pre-equip attribute and stop flight. These tests do not assign a flight flag or attribute modifier themselves. Graphical input, rendering, and packet transmission require separate production-client verification.

Initial external-wing fixture failures are retained: vanilla `Player.tryToStartFallFlying` does not represent the separate Caelus request path on the inspected Forge versions. In the 1.19.2 controls, the real Enigmatic item provided attribute value 1.0 with empty, vanilla, and Dragon chest slots while that vanilla method returned false in all three cases. This was evidence to correct the test entry point; it was not evidence of Dragon armor interference.

The removal assertion also compares against the real pre-equip value: inspected Caelus versions use baseline 0.0 on 1.18.2/1.19.2 and 0.1 on 1.20.1/1.21.1. An initial strict-zero assertion incorrectly rejected correctly removed modifiers on the latter versions. The failed 1.16.5 draft assumed a newer Enigmatic item/back slot; inspecting its exact JAR showed those features absent, so that inapplicable case was removed and is not reported as a passing external-grant test.

The original reported Caelus crash has not been reproduced from the user's exact modpack and configuration. Passing these pinned minimal combinations establishes the tested behavior; it does not establish compatibility with every modpack, datapack, or future upstream release. Advanced Netherite's loaded configuration does not automatically redefine Dragon Loot's balance or provide unimplemented extra drops.

Production packaged-JAR dedicated-server and graphical-client results are recorded separately in [TESTING.md](TESTING.md). The dedicated-server checks are intended to catch side stripping, reobfuscation, and production-only mod-loading issues; development GameTests alone cannot establish those properties.

## Primary upstream references

- [Better Combat source and weapon attributes](https://github.com/ZsoltMolnarrr/BetterCombat) and [official version listings](https://modrinth.com/mod/better-combat/versions).
- [Caelus source](https://github.com/illusivesoulworks/caelus) and [official version listings](https://modrinth.com/mod/caelus/versions).
- [Curios command documentation](https://docs.illusivesoulworks.com/curios/commands). On the tested 1.18.2–1.20.1 combinations, equip `enigmaticlegacy:enigmatic_elytra` in the back slot; on 1.20.1/1.21.1 use `icarus:white_feathered_wings`. Slot command support does not imply that every older mod contains those items or slot types.
- [ForgeGradle dependency handling](https://docs.minecraftforge.net/en/fg-6.x/dependencies/) and [NeoGradle documentation](https://docs.neoforged.net/toolchain/docs/plugins/ng/).
- [NeoForge dedicated-server installation](https://docs.neoforged.net/user/docs/server/).

## Runtime results

The initial combined profiles passed the version-specific suite before the later external-wing cases were added: 1.16.5 7/7, 1.18.2 25/25, 1.19.2 13/13, 1.20.1 25/25 after expansion, and 1.21.1 26/26. Each passing result had native exit code 0 and runtime mod-version assertions. The final applicable suites below were rechecked by the result verifier; an earlier green suite is not presented as covering a subsequently added case.

| Minecraft | Final combined assertions | Native exit | External Curios grant/removal | Captured log prefix under `release/test-results/2026-10-06/compatibility/` |
| --- | --- | --- | --- | --- |
| 1.16.5 | 7/7; JSON report passed, zero failures | 0 | N/A: pinned legacy content lacks this provider item | `1.16.5-all-20261006-151902` |
| 1.18.2 | 26/26 | 0 | Enigmatic; all three chest controls passed | `1.18.2-all-20261006-151424` |
| 1.19.2 | 26/26 | 0 | Enigmatic; all three chest controls passed | `1.19.2-all-20261006-151726` |
| 1.20.1 | 27/27 | 0 | Enigmatic and Icarus; all six controls passed | `1.20.1-all-20261006-150925` |
| 1.21.1 | 27/27 | 0 | Icarus; all three chest controls passed | `1.21.1-all-20261006-151138` |

All five isolated packaged production-JAR dedicated servers reached `Done`, loaded their pinned optional mod sets, answered console commands, saved their worlds, and shut down with actual Java exit 0. Exact installers, Dragon Loot artifact hashes, staged dependency hashes, and launch/shutdown records accompany [PROJECTILE-TESTS.md](PROJECTILE-TESTS.md) and the ignored dated production-server reports. This establishes production startup/shutdown, not every in-game integration behavior.
