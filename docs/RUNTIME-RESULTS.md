# Runtime results — October 6, 2026

Real Minecraft server and Prism client checks were run after the machine was
upgraded to 16 GB RAM. Each supported port uses its own pinned loader and Java
version. This is the completed-evidence record; [TESTING.md](TESTING.md) is the
broader checklist. Every version has its own results and applicable limitations.

The independent loader issue affecting long falls is documented as a known
limitation in [NETWORK-CONTROLS.md](NETWORK-CONTROLS.md). The unrelated 1.1.17
workaround was withdrawn. These records describe the published 1.1.16 gameplay
and integration runs, with their original flight settings.

Final `1.1.16` release assemblies and packaged-server checks were subsequently
verified. Gameplay classes/resources match these tested builds exactly; only
version metadata differs. See [RELEASE-1.1.16.md](RELEASE-1.1.16.md) for that
separate final-artifact evidence.

## Current matrix

| Minecraft / loader | Assembly | Config first run / restart | Pinned combined server suite | Packaged production server | Prism baseline | Prism combined mods |
| --- | --- | --- | --- | --- | --- | --- |
| 1.16.5 / Forge 36.2.39 | PASS, Java 8 | 6/6 + 6/6, native 0 | 7/7 applicable cases, native 0, strict smoke report passed | PASS, native 0 | PASS | PASS |
| 1.18.2 / Forge 40.2.17 | PASS, Java 17 | 23/23 + 23/23, native 0 | 26/26, native 0, including external wing grant/removal | PASS, native 0 | PASS | PASS |
| 1.19.2 / Forge 43.3.0 | PASS, Java 17 | 11/11 + 11/11, native 0* | 26/26, native 0, including external wing grant/removal | PASS, native 0 | PASS | PASS |
| 1.20.1 / Forge 47.3.0 | PASS, Java 17 | 23/23 + 23/23, native 0 | 27/27, native 0, including external wing grant/removal | PASS, native 0 | PASS | PASS |
| 1.21.1 / NeoForge 21.1.65 | PASS, Java 21 | 24/24 + 24/24, native 0 | 27/27, native 0, including external wing grant/removal | PASS, native 0 | PASS | PASS |

\* The 1.19.2 configuration profiles ran four final gear tests and seven original
projectile tests before twelve additional projectile cases were added. They
establish the same gear/config outcomes as the other profiles; their 11-test
totals are not presented as covering the later 23-test baseline.

The completed combined server suites verify actual pinned mod IDs/versions and
Better Combat's live sword/axe/trident weapon registry where available. The
external-wing cases equip real upstream items in Curios and check Caelus flight
requests, native flight and removal with empty, vanilla and Dragon chest slots.
Minecraft 1.16.5 has no matching Better Combat release or lightning rods, and
the pinned Enigmatic Legacy 2.11.12 has no Majestic Elytra/back-slot integration;
those cases are not applicable. The final combined totals are 7/26/26/27/27,
each with native exit 0; earlier green suites are kept separate from these
strengthened final runs.

Prism baseline PASS means the selected real-client regression sequence passed
in a fresh test world. Saved evidence includes held/charging trident views,
offhand/resource-reload views, survival throw/pickup and winged flight where
recorded. It does not mean every row of the broader checklist was executed.
Combined client 1.16.5 passed native Dragon winged flight with Caelus and
Enigmatic Legacy loaded; it does not establish an absent upstream Curios wing
grant. Combined
client 1.18.2 additionally confirmed real Enigmatic flight grant/removal and
Better Combat sword/axe/trident damage observations of 9/11/10 with the controlled
targets. Combined client 1.19.2 also passed both-hand/resource-reload models,
survival throw/pickup (one durability), Loyalty (two total durability), sustained
winged rocket flight without passive chest wear, Better Combat 9/11/10 damage,
and Enigmatic grant/removal while wearing ordinary Dragon chest armor. Combined
client 1.20.1 passed both Enigmatic and Icarus grants/removal, ordinary Dragon
armor without passive wear, rocket consumption, Better Combat 9/11/10 attacks
and the core trident checks. Combined client 1.21.1 also passed its core trident
sequence, actual Icarus grant/removal and controlled sword/axe/trident damage
9/11/10 after confirming target health and the hotbar selection. All five
combined clients passed their applicable sequence. The local production
connection results, including the default-setting control failure, are recorded
below. Prism used Oracle Java 8u51 for 1.16.5, Microsoft Java 17.0.15 for
1.18.2-1.20.1, and Microsoft Java 21.0.7 for 1.21.1. Build and isolated server
checks used the installed Adoptium JDKs 8u504, 17.0.20 and 21.0.12.

## Server regression scope

The expanded 1.18.2/1.19.2/1.20.1 suites each contain 19 projectile cases;
1.21.1 contains 20. They exercise release thresholds, almost-broken release,
offhand durability, custom item persistence, actual damage, natural collision,
Impaling, wet/dry Riptide, Channeling positive/negative conditions, creative
non-duplication and restricted inventory pickup. NeoForge also tests an actual
powered dispenser. Four gear cases cover registered statistics, flight gates,
sixty real ticks of sustained flight without passive wear, and live config reload
with frozen startup gear. Minecraft 1.16.5 uses six grouped real-server smoke
cases because it predates GameTest.

Configuration profiles use distinct nondefault values before the first launch,
then edit the managed config and restart the JVM while retaining the original
default config. Mining speed, attack modifiers, durability, armor, toughness,
horse protection and enchantability must match explicit expected values. Scale
settings reload while gear remains frozen. The completed configuration matrix
has 11 launches and 180 executed required cases; these include repeated cases,
not 180 unique tests.

Reproduce with [PROJECTILE-TESTS.md](PROJECTILE-TESTS.md),
[CONFIG-PROFILE-TESTS.md](CONFIG-PROFILE-TESTS.md),
[COMPATIBILITY-TESTS.md](COMPATIBILITY-TESTS.md), and the
[1.16.5 smoke runner](../DragonLoot-1.16.5-forge/DragonLoot-1.16/SMOKE-TESTS.md).

## Packaged JARs and build verification

All five exact official loader installers and actual packaged local DragonLoot
JARs were used for separate production dedicated-server launches. The frozen
all-mods profiles contain 6/9/9/12/11 top-level mod JARs including DragonLoot,
respectively. Each server reached `Done`, listed enabled datapacks and players,
processed `stop`, saved its worlds and exited Java with code 0. Native debug logs
matched every expected Minecraft/loader/top-level mod ID and version. This checks
production reobfuscation, side loading and packaging independently from
development GameTests.

The NeoForge packaged all-mods profile also completed real localhost client
gameplay in a fresh world at port 25621 with `allow-flight=true`: survival
throw/pickup consumed one durability; Loyalty returned the same item with two
total durability; native winged flight and real Icarus flight with ordinary
Dragon chest armor each consumed a rocket; the winged chest took no passive
damage; removing the Icarus wings stopped native fall-flying. The client
disconnected normally and the server saved/stopped with native exit 0. This
establishes those network behaviors under that explicit server setting, while
the separate default-setting controls below retain their failure.

Five assemblies and their isolated test source sets compiled successfully. The
resource/JAR validator checked 437 JSON resources, metadata, local model/texture
references, required Forge refmaps and excluded regression classes. The local
test version is `1.1.16-dev`; no release had been uploaded during these checks. ForgeGradle selectors
were audited against the actually loaded plugin versions and pinned to 4.1.16,
5.1.77 and 6.0.54; NeoGradle remains 7.1.20. All five post-pin assemblies exited
0, and all ten production/source JARs have identical decompressed entry sets and
bytes to their preserved release copies. MixinGradle 0.7.38 has the same checked
code/resources and dependency metadata as the tested snapshot, with its plugin
manifest version label changed. Original release JARs were preserved. Archive
hashes can change with ZIP metadata even when every contained entry is identical.
The exact plugin audit, preserved-release comparison and reproduction commands
are in [BUILD-REPRODUCIBILITY.md](BUILD-REPRODUCIBILITY.md).

## Evidence locations

All artifacts are ignored local files under `release/test-results/2026-10-06/`:

- `<minecraft>/`: initial native server logs and baseline/combined client
  screenshots; `client-baseline/` and `client-compat/` hold later client logs.
- `config-profiles/final-matrix.json`: exact per-profile counts, source hashes,
  expected values, native exit status and links to retained native logs/reports.
- `compatibility/combined-matrix.json` and `RESULTS.md`: independently revalidated
  final 7/26/26/27/27 integration counts and native exits. Final 1.16.5 report:
  `1.16.5-all-20261006-151902.smoke-report.json`.
- `compatibility/`: frozen upstream manifest, checked JARs and timestamped
  server stdout/stderr or Gradle logs with matching exit-code files. Confirmed
  1.18.2 external-flight/removal run: `1.18.2-all-20261006-151424.*`;
  1.19.2 external-flight/removal run: `1.19.2-all-20261006-151726.*`; 1.20.1:
  `1.20.1-all-20261006-150925.*`; NeoForge:
  `1.21.1-all-20261006-151138.*`.
- `production-servers/final-packaged-all/<minecraft>-<loader>-all/`: official
  installer/library verification, packaged-JAR SHA-256 snapshots, native console
  and debug logs, `server.exitcode` and `runtime-result.json`. NeoForge's
  startup-only result is in `startup-shutdown-evidence/`.
- `production-servers/network-flight-enabled-retry/` and
  `network-default-flight-controls/`: distinct NeoForge worlds, packaged-JAR
  snapshots, client connection/operator evidence, native server logs and saved
  shutdown results. `floating-kick-analysis.md` records the final patched class
  provenance and links the pinned listener bytecode captures.
- `build-reproducibility/`: actual plugin identities, assembly exit statuses and
  comparisons with preserved pre-pin production/source JARs.
- `projectile-report.md` and `production-servers/packaged-server-report.md`:
  detailed source findings, initial/expanded counts and retained failed attempts.

## Corrections and practical limits

Failed attempts are retained. A rod fixture beneath terrain or old saved test
structures was corrected using the actual heightmap; an early native-flight
fixture landed on terrain and now asserts every airborne tick above the heightmap.
Older Caelus integrations use their request pathway rather than vanilla
`tryToStartFallFlying`; removal checks compare the pre-equip attribute because
the upstream default is not zero on every version. A proposed 1.16.5 external
wing case was removed after its pinned Enigmatic JAR was verified to lack that
item/integration; the failed unsupported-case attempt is retained. These corrections are
not presented as evidence of new production defects.

On this Windows machine, attached Forge 1.18.2/1.19.2 Gradle runs sometimes
reported a disappeared daemon after Minecraft completed and saved. Those
unresolved wrapper exits were replaced by exported direct Java launches with
recorded native status. NeoGradle's client asset HEAD request stalled on this
network; the explicit server-only headless initializer skips client asset/native
actions, while Prism client checks still use actual assets. Initial NeoForge
launch export omissions and Windows harness file-sharing/task-result/long-path
issues were corrected and their attempts retained.

The smoke runner requires its structured report to show zero failures as well
as native exit 0; a failed smoke report cannot pass merely because Java exits
normally. The compatibility guard was checked against retained failed smoke and
GameTest artifacts. The packaged-server harness separately requires `Done`,
requested stop, saved shutdown, native exit 0 and verified loaded versions.

The first local dedicated-client gameplay attempt had no effective operator
permissions: a pre-join cache lookup granted a different online UUID from the
connected offline player's UUID. Its rejected commands, screenshots and logs
are preserved in `client-dedicated-before-operator-fix`. Granting permission
after the actual player joined corrected the UUID and synchronized permissions.
The future held-server helper now waits for that join before issuing `op`.
The subsequent default-flight-setting attempt was kicked for floating after
airborne teleports, mode transitions and wing removal. Its server still saved
and exited 0; that lifecycle result is not a gameplay pass. A fresh all-mods
`allow-flight=false` control reproduced the same roughly four-second kick during
a 220-block survival fall with neither Dragon nor Curios gear. Its lower
40-block fall landed normally. A valid vanilla Elytra control maintained native
flight beyond six seconds, then reproduced the kick after Elytra removal and
another high ordinary fall. Thus the observed rejection is broader than Dragon
or Icarus wing eligibility. An earlier post-death vanilla batch is invalid: a
natural slime interrupted it; Peaceful difficulty and disabled natural spawning
were confirmed before the valid rerun.

The exact final NeoForge 21.1.65 server bytecode tests residual Y movement in its
floating condition, unlike the original packet delta used by the prepatch
vanilla JAR. FML's actual patched-class overlay order was independently verified.
That is a concrete loader regression candidate consistent with the no-equipment
and vanilla-removal controls; these checks do not establish the behavior of
every other loader release. DragonLoot's native flight exemptions were preserved.
The inspected NeoForm decompile stage loses the original-delta copy before
NeoForge's source patches. Detailed historical class captures remain in the
local evidence directories listed above. Later loader-only reproductions on
NeoForge 21.1.65 and Forge 47.3.0 confirmed the independent issue; their scope is
recorded in [NETWORK-CONTROLS.md](NETWORK-CONTROLS.md). No verified fixed loader
release is claimed.
The flight-enabled retry passed its gameplay checks, but changing server flight
permission is not presented as resolving default-setting enforcement. Both fresh
follow-up servers saved and exited native Java 0; all failed/interrupted attempts
remain available.

The production 1.16.5 flat-settings parser logged vanilla `Not a registry ops`
and fell back to default flat settings. The NeoForge all-mods loader caught an
upstream Sparkweave client-class metadata lookup on a dedicated server. Both
continued through saved shutdown and native exit 0; their diagnostics are kept.

The host's OpenAL audio device is unavailable, so these checks do not establish
audible sound playback. The full reported BMC5 NeoForge 1.21.1 v51 pack and its
exact configuration have not been reproduced. Localhost networking is tracked
separately below; remote Internet server connections are not tested. Neither
minimal pinned integrations nor ordinary source review establishes compatibility
with every datapack, combat resource pack or future upstream version.

Projectile save tests exercise persistence APIs and explicitly reapply mock
owners; full server restart UUID recovery, every packet timing case and a complete
Loyalty journey are not established by those assertions. The 1.16.5 smoke suite
does not yet automate the full Impaling/Riptide/creative/inventory/Channeling mob
matrix. Anvil packet synchronization, dynamic recipe priority and all optional
pacification perks remain separate checklist cases unless their evidence is
recorded. No publication is authorized by these local test results.

## Local production client connection

| Version / profile | Connection | Throw / pickup | Flight | Disconnect / server save / native exit |
| --- | --- | --- | --- | --- |
| 1.21.1 NeoForge, initial all-mods attempt, `allow-flight=false` | Connected; operator UUID corrected after rejected commands | Interrupted sequence; not counted as a complete pass | Interrupted by floating kick after scripted transitions | Kicked; console stop, saved, native 0 |
| 1.21.1 NeoForge, fresh all-mods retry, `allow-flight=true` | PASS; operator granted to actual joined profile | PASS; survival Damage1, same Loyalty item Damage2 | PASS; native winged and Icarus/ordinary Dragon armor flight, rocket consumption, removal stops flight | Normal disconnect, saved, native 0 |
| 1.21.1 NeoForge, fresh all-mods controls, `allow-flight=false` | Connected; same exact packaged pins | Not a projectile sequence | Vanilla glide PASS; low fall PASS; no-gear high fall and vanilla-removal high fall kicked | Kicked; console stop, saved, native 0 |
