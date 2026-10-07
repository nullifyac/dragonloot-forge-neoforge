# Network flight controls - October 6-7, 2026

All five pinned loaders' installed production classes contain a code bug that can
mistake an ordinary long fall for hovering. Real client behavior is confirmed on
NeoForge 21.1.65 and Forge 47.3.0. A fresh real client connected to a NeoForge 21.1.65 dedicated server
with no mods reproduced the kick while continuing to descend. The corrected
Dragon Loot build passed the corresponding high fall and Slow Falling controls,
and deliberately stationary hovering still triggered the normal kick.

The remaining fresh real client comparisons are **blocked by the Windows display**. Forge 1.20.1
has confirmed bare-loader and corrected fall results; its hover control remains
pending. The 1.16.5, 1.18.2 and 1.19.2 comparisons are pending. Compiled behavior
and native handler tests are recorded separately from those comparisons. All
current dedicated controls use `allow-flight=false`.

## Current verification status

| Minecraft | Exact loader | Compiled original-Y loss | Fresh bare-loader real client | Corrected real client |
| --- | --- | --- | --- | --- |
| 1.16.5 | Forge 36.2.39 | Installed production confirmed | Pending; not launched in this fresh matrix | Pending |
| 1.18.2 | Forge 40.2.17 | Installed production confirmed | Pending; not launched in this fresh matrix | Pending |
| 1.19.2 | Forge 43.3.0 | Installed production confirmed | Pending; not launched in this fresh matrix | Pending |
| 1.20.1 | Forge 47.3.0 | Confirmed | Ordinary high fall kicked | High fall and Slow Falling passed; hover pending |
| 1.21.1 | NeoForge 21.1.65 | Confirmed | Ordinary high fall kicked | High fall and Slow Falling passed; hovering still kicked |

The final native handler regressions passed on all five pins: one six-scenario
smoke aggregate on 1.16.5, 29/29 required GameTests on each of 1.18.2, 1.19.2 and
1.20.1, and 30/30 on 1.21.1. Every run used a fresh world and
`allow-flight=false`, checked the loaded pin, saved normally and exited Java 0.
These server API tests establish packet handling and timeout behavior with
simulated client movement; they do not establish a real client connection or a
full gliding trajectory. Earlier four-scenario results remain historical evidence.
The exported development classpaths report mod version `1.1.16-dev`; the matrix
records hashes of the actual new correction and test sources. That metadata label
is retained. Packaged corrected client observations used the separate
`1.1.17-dev` test JARs, and the reproduction commands below build current 1.1.17
sources rather than relabeling the archived native runs.

The first 1.16.5 packaged 1.1.17 draft failed startup because a partial
ForgeGradle 4 compilation replaced the whole-source Mixin mappings with only the
new listener's entries. The existing anvil shadow remained unmapped. Disabling
incremental compilation for main Java sources and rebuilding restored all 14 old
mapping owners plus the new one; all 54 existing production classes remain byte
for byte identical to 1.1.16. No anvil logic or injection requirement changed.
The corrected native six-scenario rerun passed, and the final packaged 1.1.17
JAR subsequently passed dedicated-server startup, loaded-version checks and saved
shutdown with Java 0 on Forge 36.2.39. That production run had no client join and
does not complete the pending real client comparison.

The NeoForge operator-reviewed records establish the following:

| Case | Actual network observation |
| --- | --- |
| Bare loader, empty client/server mod directories; grounded survival start, no flight abilities/effects, normal gravity | Fall from Y240 continued through accepted Y142.48, Y125.62, Y107.99 and Y89.70, then kicked before reaching the floor at Y-60. |
| Corrected build; the same ordinary high fall | Reached Y-60 and remained connected through the ten-second check. |
| Corrected build; Slow Falling from Y40 | Remained descending and connected for eight seconds; final sampled server Y was approximately -12.99. |
| Corrected build; deliberate client hover at Y40 | Fourteen accepted-position samples stayed at Y40; the normal floating kick occurred near four seconds. Server flight abilities and fall-flying remained false; server `NoGravity` was absent/default false. |

The hover fixture only changes local client movement for the negative control.
The bare positive baseline has no test mod. Fixture `System.out` telemetry was
not present in the archived Minecraft logs; stable server-accepted positions and
the server's logged disconnect reason establish that control. Fixed NeoForge
screenshots were black, so no visual pass is claimed for its corrected cases.
The bare-loader NeoForge kick screenshot was readable. The two NeoForge servers
saved and exited Java 0; lifecycle success is separate from gameplay outcomes.

The operator subsequently confirmed the Forge 47.3.0 bare-loader kick during a
descent from accepted Y280 to Y123.464. Its corrected high fall reached Y-60 with
`OnGround: 1b` and a connected-player check. Corrected Slow Falling remained
airborne and connected for nine seconds, reaching accepted Y-23.868. Its deliberate
hover control remains pending because its fresh client retry could not create a
working display. The primary monitor was unavailable; the supported
`earlyWindowControl=false` fallback reached Minecraft but produced GLFW 65542 / WGL
no-supported-OpenGL errors. Its render-stack capture is retained. Configuration
was restored, the fixture removed, and every owned production server stopped
normally with Java 0. No further client outcome is inferred from that preparation.

## Why normal play can hide it

Vanilla exempts active Elytra gliding from the floating predicate. Ordinary short
falls can land before the ordinary-gravity timeout of more than 80 floating ticks,
approximately four seconds. The need for a long uninterrupted fall plausibly
explains why reports may be sparse; neither report frequency nor a minimum drop
height was measured here.

Minecraft's integrated singleplayer server sets flight allowed to true in all
five inspected versions. Dedicated `allow-flight=true` suppresses the same kick
path. A successful singleplayer session or flight-enabled dedicated session
therefore does not disprove the defect. The property is a broad workaround, not
the targeted code correction.

The actual published **1.1.15 NeoForge** JAR was inspected. Its registered
`LivingEntityMixin` preserved an already-active glide while airborne, not riding
and wearing the upgraded Dragon chestplate; `PlayerEntityMixin` recognized that
chestplate during the start-flight path, and the client Mixin requested that start.
It contains no packet-listener correction. Inferring that preserved gliding could
mask the loader issue follows from those compiled methods and vanilla's gliding
exemption. It does not protect an empty-chest ordinary fall, and that old JAR has
not been run through the fresh network matrix.

The old artifact SHA-256 is
`69725ce4b5b0bd9907e56ec32d2949aeac68321f7dc0f54b21e0035c7fefbf8e`.
Its embedded metadata confirms version 1.1.15 for Minecraft 1.21.1 and NeoForge
21.1.65 or newer. The compiled wing methods are retained with that artifact.

## Defect and correction boundary

Vanilla saves the original accepted movement packet's Y delta before applying
player movement and collision correction. Its floating predicate tests the saved
value against `-0.03125`. Valid accepted downward movement below that threshold
clears floating.

The inspected loader classes lose that saved copy. They overwrite the variable
with the post-movement collision residual, normalize the residual to zero, and
then use it for the floating test. This can count a real descent as hovering.
Vanilla also normalizes the collision residual: the missing saved value is the
regression, not the normalization itself.

| Minecraft | Vanilla floating reads saved original Y | Pinned loader reads overwritten residual |
| --- | --- | --- |
| 1.16.5 | local 32 | local 21 |
| 1.18.2 | local 33 | local 21 |
| 1.19.2 | local 33 | local 21 |
| 1.20.1 | local 31 | local 19 |
| 1.21.1 | local 31 | local 19 |

The new Mixins capture the unchanged movement vector's Y before `player.move`
and add the original descent threshold to the existing accepted floating result.
Forge uses standard `ModifyArg` and an injection after the field write; NeoForge
shares the value within the individual invocation and wraps the original
operations. Exact targets and counts were checked against compiled bytecode, and
control-flow inspection confirms capture precedes every accepted floating write.

The correction retains the other flight/hover gates, movement and collision
calculations, timeout, kick and actual flight state. It can clear an erroneous
floating result during descent; it cannot turn an existing false result into true.
The additional condition is redundant on an otherwise matching loader that already
uses the correct saved delta. Startup checks require exactly one matching capture
and field write; this is not a guarantee for arbitrary future loader layouts.

Each older port needs the bare-loader high fall, corrected high fall and Slow
Falling, and deliberate hover with normal server flight gates. The current table
marks only confirmed cases; Forge 1.20.1 has completed its positive comparisons
but still needs the real client negative control. Native vanilla Elytra and Dragon
wing eligibility exemptions passed the final handler suite; physical flight and
optional grants/removal in these fresh network comparisons remain separate from
those API assertions. Pending results
require operator review of accepted positions and the gameplay outcome.

## Upstream status: no verified fixed NeoForge release

The latest published Minecraft 1.21.1 NeoForge checked on October 7 is
**21.1.256**. Both its official userdev and installer artifacts were downloaded
and verified against their official SHA-1 files. Their binary patches were applied
to the matching original vanilla classes with the configured official
binarypatcher and normal patch checksums; both patch operations exited 0.
The reconstructed production listener retains the same faulty predicate. This is
a static binary result: **21.1.256 was not launched**.
[Official metadata](https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml),
[inspected production installer](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.256/neoforge-21.1.256-installer.jar).

That installer still selects NeoForm `1.21.1-20240808.144430`. The current official
[1.21.1 packet-listener patch](https://raw.githubusercontent.com/neoforged/NeoForge/1.21.1/patches/net/minecraft/server/network/ServerGamePacketListenerImpl.java.patch)
changes the permitted-flight abilities query but does not restore the saved Y.
The final 21.1.256 production listener SHA-256 is
`f8193992da15902eadb8a77681b1d9ee4ab3bed175ab7029d46061fa6486ba15`.

The NeoForm cache descriptor identifies **Vineflower 1.10.1**. Direct decompilation
of the exact original 1.21.1 listener with that version reproduces the lost saved
copy. The official, checksum-verified **Vineflower 1.12.0** preserves a distinct
copy and uses it in the floating predicate. This reproduces the semantic loss
during decompilation, before NeoForge's source patch.

Vineflower maintainers tracked the corresponding variable-merging bug in
[issue 452](https://github.com/Vineflower/vineflower/issues/452), closed as fixed by
[PR 507](https://github.com/Vineflower/vineflower/pull/507), merged on November 9,
2025. The [1.12.0 release](https://github.com/Vineflower/vineflower/releases/tag/1.12.0)
was published April 29, 2026 and includes correction of overwritten saved
variables being dropped. This establishes an acknowledged decompiler defect
class and directly reproduces its effect on this listener. Focused searches found
no exact NeoForge gameplay issue acknowledgment or corrected 1.21.1 loader release.
A decompiler release does not retroactively correct published loader binaries.

The four Forge comparisons establish the same loss in both development classes
and independently inspected installed production server classes. Their historical
decompiler versions were not established in this audit;
the direct 1.10.1-versus-1.12.0 reproduction specifically covers the inspected
1.21.1 NeoForm input. Cached NeoForge 21.1.217 also retains the defect and remains
a static-only comparison.

## Installed production Forge class selection

The initial older-port audit used mapped ForgeGradle development JARs. A subsequent
independent check used the prepared official installations' patched server JARs
and their matching vanilla SRG inputs. All four production listeners lose the
saved original Y; the matching vanilla classes preserve it.

For Forge 36.2.39, the root launcher selects `fmlserver`.
`LibraryFinder.getMCPaths` returns the patched server JAR first, followed by extra
resources and vanilla SRG. `MinecraftLocator` registers that first JAR as the
Minecraft mod, whose locator supplies the class bytes before URL fallback.
Neither the bootstrap nor Forge universal JAR contains the listener.

For Forge 40.2.17, 43.3.0 and 47.3.0, the official `win_args.txt` selects
`forgeserver`. `CommonServerLaunchHandler` adds vanilla SRG and filtered extra
resources; `ForgeServerLaunchHandler` appends the patched server JAR.
`MinecraftLocator` passes that list to `SecureJar`. The installed Securejarhandler
versions 1.0.8, 2.1.4 and 2.1.10 reverse the list and choose the first existing
class, giving the patched server precedence. This was checked in those installed
loaders' compiled classes without launching Minecraft.

Each inspected artifact is beneath
`network-fix/<minecraft>-forge-loader-only/libraries/net/minecraftforge/forge/<minecraft>-<forge>/forge-<minecraft>-<forge>-server.jar`
in the October 7 evidence directory.

| Minecraft / Forge | Installed patched server JAR SHA-256 |
| --- | --- |
| 1.16.5 / 36.2.39 | `7b37ca44125070199416c78acf525def23615bbc823e2df0fc9f274192fb64d5` |
| 1.18.2 / 40.2.17 | `5ccc9f8cd1d7f08fdc66e175d3f891ed919f5e9dfe1151140adbc2fbd2c9b610` |
| 1.19.2 / 43.3.0 | `0c00f6eed2a16f7f9944d3794c35d7d8639b2943b2a47c33d0e8cce6f19427f9` |
| 1.20.1 / 47.3.0 | `729f188c185e1915b4f70c80676ebdd9b4f24c5de6acc2dede21dd0136cbc287` |

The accompanying manifest records absolute paths, individual listener hashes,
vanilla input hashes and class-selection captures. Installed production code
confirmation does not turn the pending 1.16.5/1.18.2/1.19.2 real client cases into
runtime passes.

## Reproducing the six native handler cases

Use fresh generated test directories, the pinned loaders and the module's required
JDK (8 for 1.16.5, 17 for the other Forge ports, 21 for NeoForge). Run one test JVM
at a time. Accept the Minecraft EULA in that generated working directory and keep
`allow-flight=false`; keep existing player worlds separate.

From a PowerShell prompt at the repository root, the tracked low-memory wrapper
can run the four newer source suites sequentially:

```powershell
foreach ($mcVersion in @('1.18.2', '1.19.2', '1.20.1', '1.21.1')) {
    & .\scripts\build-low-memory.ps1 -MinecraftVersion $mcVersion -ModVersion 1.1.17 -Tasks @('runGameTestServer')
}
```

On 1.16.5, select the dedicated network aggregate instead:

```powershell
& .\scripts\build-low-memory.ps1 -MinecraftVersion 1.16.5 -ModVersion 1.1.17 -Tasks @('runSmokeTest', '-PsmokeCase=network-flight')
```

Its working directory is the module's `run/smokeTest/network-flight`. Require its
new `smoke-report.json` to say case `network-flight`, `passed: true`, zero failures
and a passed `network-flight` aggregate. Native exit 0 alone is insufficient.
For each newer suite require all required tests and the six-test network batch to
pass, plus a normal native Java exit; baseline totals are 29 Forge and 30 NeoForge.
Optional compatibility profiles add their own cases and have different totals.

The tests cover:

1. 121 accepted downward moves without a floating timeout.
2. Unsupported stationary hovering disconnecting at native tick 81.
3. The exact `-0.03125` boundary and the next representable packet coordinate below it.
4. Rejected excessive movement and unacknowledged teleport packets preserving the existing hover timer.
5. Native vanilla Elytra flight-start eligibility preserving the floating exemption for 121 listener ticks.
6. Native upgraded Dragon chestplate flight-start eligibility preserving that same exemption.

The inherited real movement handler and its real `tick()` run in every scenario;
motion packets are simulated and transport records sends/disconnects. The test
sources are `ServerMovementGameTests.java` in each newer module's `src/gametest`
and `NetworkFlightSmokeTests.java` in 1.16.5's `src/smokeTest`.
The tracked [direct launch exporters/helpers](../scripts/compatibility/) provide
an alternative when attached Windows Gradle runs obscure the native process exit.
For NeoForge asset-download stalls, the opt-in
[headless init script](../scripts/compatibility/headless-server.init.gradle) skips
client asset/native downloads for server tasks; pass its absolute path with `-I`
and `-PdragonlootHeadlessServer=true` through the wrapper's `Tasks` array.

## October 7 evidence

Paths below are beneath `release/test-results/2026-10-07/`:

- `network-fix/reviewed-observations.json`: reviewed individual gameplay cases.
  `network-evidence-current.json` retains accepted positions and raw evidence;
  it does not infer a pass from preparation or startup.
- `network-fix/loader-only/owned-runs/20261006T232928383905Z/`:
  fresh bare NeoForge baseline, server logs, case markers and native exit.
- `network-fix/fixed-1.21.1/owned-runs/20261006T233335115327Z/`:
  corrected high fall, Slow Falling and deliberate-hover run.
- `upstream-flight/artifact-manifest.json` and
  `21.1.256-production-server-listener-bytecode.txt`: official URLs/checksums,
  exact inputs and reconstructed latest production bytecode.
- `upstream-flight/older-bytecode-matrix.json`, the `Forge-*-listener-bytecode.txt`
  and `Vanilla-*-listener-bytecode.txt` files: initial mapped development comparisons.
- `upstream-flight/production-forge/installed-production-matrix.json` and
  `AUDIT-CLARIFICATION.md`: subsequent installed production classes, hashes and
  verified launcher selection; full patched/base/loader bytecode captures beside them.
- `network-regression/final-matrix.json` and `RESULTS.md`: all five final six-case
  native handler results, loaded pins, actual Java exits and source hashes.
- `network-fix/fg4-mapping-repair/`: preserved failed packaging outputs and corrected
  complete mappings/class comparison. `network-regression/1.16.5/20261007-000928/`
  retains the corrected dev17 six-scenario rerun. The final packaged startup record
  is `release-1.1.17/production-servers-corrected16/1.16.5-forge-dragonloot/runtime-result.json`;
  its JAR SHA-256 is `45d8aa4d712a2929571ea0ddd04e59e1bda8ae31e436650bcbfb28c7e243bd6c`.
- `network-fix/fixed-1.20.1/display-fallback-render-stack.txt`: the Windows/OpenGL
  blocker for the pending Forge 1.20.1 client hover case and older client comparisons.
- `upstream-flight/listener-control-flow-review.json`: capture/write counts and
  accepted-path dominance for all five pins and NeoForge 21.1.256.
- `upstream-flight/vineflower1.10/` and `vineflower1.12-full/`:
  direct same-input decompiler comparison. Logs preserve unrelated missing-class
  enum warnings separately from the relevant primitive-Y logic.
- `network-audit/dragonloot-1.1.15-1.21.1-neoforge.jar` and
  `published-wing-mixins-bytecode.txt`: actual published old artifact and methods.
- `network-fix/*-IntegratedServer-bytecode.txt`: the four Forge singleplayer
  flight-property captures; the cached original NeoForm source confirms it for
  1.21.1.

## Historical October 6 controls

The following results predate the packet-listener correction and the fresh bare
loader comparison. Their test scopes and limitations are retained.

### Tested scope and observations

Real Prism client, localhost dedicated server, Minecraft 1.21.1, NeoForge
21.1.65, Java 21, and the frozen combined profile in
[manifest.json](../scripts/compatibility/manifest.json). This includes Caelus
7.0.1+1.21.1, Icarus 4.5.0 and Sparkweave 0.508.0. The flat-world floor was Y-60.
These controls used `allow-flight=false`.

| Control | Recorded observation |
| --- | --- |
| Grounded survival player, empty chest/back, no flight input or rockets; teleport to Y160 | Floating kick at 17:47:23.285, 4.060 seconds after teleport. |
| Same empty equipment, 40-block fall from Y-20 | Landed without a floating kick; later unrelated slime damage interrupted the subsequent batch. |
| Valid vanilla Elytra glide, no Curios wings | `FallFlying: 1b` beyond six seconds, remained connected. |
| Vanilla Elytra removed, followed by another empty-chest high fall | `FallFlying: 0b`; kick at 17:57:40.183, 4.089 seconds after teleport. |

The post-death vanilla batch is excluded. Peaceful difficulty and disabled natural
spawning were confirmed before the valid vanilla rerun. The controls establish
that the rejection is broader than equipped Dragon gear or an Icarus grant. At that point, a
loader-only network reproduction had **not** been run.

An independently recorded retry with `allow-flight=true` passed production
throw/pickup, Loyalty, native winged flight and Icarus grant/removal with ordinary
Dragon armor. That setting bypasses the floating predicate; it does not resolve
the default-setting failure. All three network server attempts saved and exited
Java 0, which establishes lifecycle success separately from gameplay outcomes.

### Final class, not the vanilla base JAR

FML 4.0.24's `ProductionServerProvider` combines the base SRG JAR, filtered extra
resources, then `net.neoforged:neoforge:21.1.65:server`. Securejarhandler 3.0.8's
multi-release `UnionFileSystem` reverses those paths and searches the reversed
order. The patched server JAR therefore wins for duplicate classes. Its displayed
primary path remains the base SRG filename. Two agents independently checked this
overlay precedence.

The final `ServerGamePacketListenerImpl.handleMovePlayer` overwrites local19 with
the collision residual at bytecode offsets 731–741, zeros it at 761–762, and reads
that value for floating at 961–967. The base vanilla class instead saves the
original accepted Y delta in local31 at 719–721 and tests it at 1019–1026. Valid
downward movement clears vanilla floating; the final loader class loses that
distinction. The ordinary-gravity timeout is more than 80 floating ticks. Actual
fall-flying exempts the player, matching the vanilla glide control.

| Raw artifact | SHA-256 |
| --- | --- |
| `neoforge-21.1.65-server.jar` | `c689a1e3bc2d4ac263d691561c9ceb16cb0e261c3a55bd66dd5b8cc20cd92b49` |
| Final listener class within that JAR | `85b7501459ba8150385568b468edabc8e0f9193dc9d1222ab75c1262dbc19a10` |
| Base vanilla listener class within `server-1.21.1-20240808.144430-srg.jar` | `6e96efd4cfe5409c3dbbf662165d871e9e8171321b20452edd6ec34e1368ecef` |

### Static upstream comparison

The [exact official 21.1.65 userdev artifact](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.65/neoforge-21.1.65-userdev.jar)
(SHA-1 `a9e4a4a4b15fa7be1aa93fdb0212a8c1de6d837d`) changes `mayfly` to `mayFly()`
near this predicate, without restoring the original-delta copy. Cached NeoForm
renamed bytecode preserves that copy; its decompiled source already loses it.
The corresponding cache descriptor identifies Vineflower 1.10.1. In that inspected
pipeline, the semantic loss occurs during decompile/recompile before NeoForge's
source patches.

Cached NeoForge **21.1.217 was inspected statically only**, with its version
confirmed by embedded metadata and cache descriptors. Its source and compiled
listener retain the same residual predicate; it was not launched for these
controls. The compiled aggregate JAR SHA-256 is
`52ee166efe41e04c462ea59f7c34d76f9357d77d5f69d14ccdbf2adf53499488`.
The current official 1.21.1 branch still uses
[NeoForm 20240808.144430](https://github.com/neoforged/NeoForge/blob/1.21.1/gradle.properties)
and has no restoration hunk in its
[packet-listener patch](https://github.com/neoforged/NeoForge/blob/1.21.1/patches/net/minecraft/server/network/ServerGamePacketListenerImpl.java.patch).
This does not establish every newer published loader's bytecode or identify a
fixed release.

The inspected Caelus/Icarus/Sparkweave hooks do not rewrite this packet listener
or suppress ordinary entity ticks. Icarus acceleration requires active gliding
and wings. Its separate removal recovery can set server Y motion to -0.4 until
ground/water, but is not required to explain the fresh empty-equipment control.

### Retained evidence

All paths below are beneath `release/test-results/2026-10-06/`:

- `production-servers/network-matrix.json`: settings, native exits, valid controls
  and the separate flight-enabled retry.
- `production-servers/network-default-flight-controls/1.21.1-neoforge-all/`:
  actual `logs/latest.log`, `logs/debug.log` and final loader JAR under
  `libraries/net/neoforged/neoforge/21.1.65/`.
- `1.21.1/client-default-controls/` and
  `1.21.1/client-default-vanilla-control-valid/`: root-supervised screenshots.
- `compatibility/NeoProductionPatchedServerListener-bytecode.txt` and
  `ProductionServerGamePacketListenerImpl-bytecode.txt`: final/base comparison.
- `compatibility/ProductionServerProvider-bytecode.txt` and
  `ProductionUnionResolution-bytecode.txt`: class overlay precedence.
- `compatibility/NeoFormPreDecompileListener-bytecode.txt` and
  `CachedNewerNeoServerListener-bytecode.txt`: static pipeline/newer comparison.
- `compatibility/network-flight-kick-audit.md`: complete investigation, upstream
  hook captures and correction history. The initial base-JAR comparison was
  superseded after checking the actual final overlay.
