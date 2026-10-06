# NeoForge network flight controls — October 6, 2026

The pinned NeoForge 21.1.65 production server reproduced a floating kick during
ordinary high falls with no Dragon armor or Curios wings equipped. Its final
packet-listener class contains a concrete loader regression candidate consistent
with those controls. Native gliding remained exempt. No Dragon Loot bypass,
loader-pin change, or verified fixed-release claim follows from this result.

## Tested scope and observations

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
that the rejection is broader than equipped Dragon gear or an Icarus grant. A
loader-only network reproduction was **not** run.

An independently recorded retry with `allow-flight=true` passed production
throw/pickup, Loyalty, native winged flight and Icarus grant/removal with ordinary
Dragon armor. That setting bypasses the floating predicate; it does not resolve
the default-setting failure. All three network server attempts saved and exited
Java 0, which establishes lifecycle success separately from gameplay outcomes.

## Final class, not the vanilla base JAR

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

## Static upstream comparison

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

## Retained evidence

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
