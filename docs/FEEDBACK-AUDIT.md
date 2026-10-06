# Feedback audit - October 5, 2026

Maintenance covers all five ports equally: Forge 1.16.5, 1.18.2, 1.19.2 and
1.20.1, plus NeoForge 1.21.1. The repo started clean at `8246d20`; its existing
code already included optional Advanced Netherite-style armor pacification and
fixed stat adjustments. User comments are useful leads, but do not identify
exact logs, configurations or loader versions for every case. A defect found in
one port is not assumed to exist in the others.

## Findings and prepared changes

| Feedback | Assessment | Change / remaining work |
| --- | --- | --- |
| Cannot throw Dragon trident; durability disappears | Confirmed in NeoForge 1.21.1: `AbstractArrow` calls the overridden default pickup method before the subclass item field is initialized. That port also omitted the actual entity damage call, returning pickup logic and saving the dealt-damage flag. The four Forge ports already constructed their projectile stack safely, applied hit damage and saved that flag. | Repair the NeoForge constructor, item persistence and vanilla 1.21 damage/enchantment flow. In Forge, separately correct Loyalty interpolation, restore enchanted appearance after entity reload and guard against releasing an almost-broken trident. Forge 1.18.2/1.19.2/1.20.1 also gain returning owner pickup and Channeling lightning-rod hits. Runtime results and exact coverage are recorded in [RUNTIME-RESULTS.md](RUNTIME-RESULTS.md). |
| Held trident looks like inventory sprite | Confirmed across all five ports: the generated item model never selects the existing in-hand renderer. A thrown projectile having a 3D model does not demonstrate held-item correctness. | Use each loader's supported separate-perspective/transform model for held and charging 3D views, preserving inventory sprites. Register renderer resource-reload handling in 1.18.2 and later. Per-version client visual results are recorded in [RUNTIME-RESULTS.md](RUNTIME-RESULTS.md). |
| Caelus incompatibility | The specific reported crash remains unconfirmed without logs. All ports used flight mixins that cancel the flight update Caelus also modifies and bypass normal flight gates. | In every port, use native `canElytraFly`/`elytraFlightTick` winged-chestplate item hooks and remove the three flight mixins. Preserve no passive flight wear. Integration with the matching Caelus release still needs runtime testing. |
| Better Combat compatibility | Upstream fallback rules already match ordinary sword/axe/trident names, so the comments alone do not prove incompatibility. The requested Better Combat project has no Forge 1.16.5 release. | Add explicit optional weapon presets for Forge 1.18.2/1.19.2/1.20.1 and NeoForge 1.21.1 without a mandatory dependency. Mark 1.16.5 integration as not applicable. Animations, attributes and throwing need runtime checks. |
| Config damage ceiling / settings ignored | Range caps restrict pack tuning in every port. NeoForge also omitted the older Forge ports' early config bake, allowing saved COMMON settings to load after item/material registration. The Forge branches already had a preload and bake; they did not share that omission. | Expand defensible bounds and add mining-speed and horse-protection settings everywhere. NeoForge uses native STARTUP loading. Harden Forge's existing COMMON preload, preserve supplied `defaultconfigs`, retain an in-memory config copy and explicitly freeze gear settings for the process lifetime. |
| Advanced Netherite mining downgrade | Supported by upstream defaults: diamond Netherite speed is 39 while Dragon tools hardcoded 12. The compatibility flag does not read that mod's config. Installed pack settings may differ. | Add `dragon_tool_mining_speed` with the existing default 12 in every port. Pack authors can configure speed, damage and armor explicitly; automatic inheritance remains unsupported. |
| Advanced Netherite armor downgrade | Toughness 3 versus 4 is correct for the cited upstream defaults, but the whole-set claim is unproven: Dragon has more protection and Minecraft caps the total armor attribute at 30. Lower toughness alone does not settle the damage comparison. | Preserve established defaults and make startup configuration consistent. Compare controlled damage cases using the actual pack configuration at runtime. |
| Modrinth upload | A distribution request, rather than a code compatibility defect. A comment does not establish a destination project/account or authorize publication. | Correct all ports' metadata links to the Forge/NeoForge CurseForge project. Publication remains a separate release action. |

Other confirmed defects and corrections:

- NeoForge `AnimalArmorItem` uses material type `BODY`, which Dragon's defense
  map omitted. Restore default horse protection 18 from the Forge ports and
  expose the same setting everywhere. Forge horse armor already had protection
  18; its default remains unchanged.
- All ports' optional pickaxe/hoe/sword tooltips advertised bonus drops with no
  implementing event or loot code. Remove those claims. Use Dragon Loot's own
  translations for the armor pacification perks that exist, including when
  Advanced Netherite is absent.
- All ports' Enderman pacification mixin listed a nonexistent method alias.
  Target the verified `isLookingAtMe(Player)` method directly; runtime perk
  checks remain part of each version's test matrix.
- The NeoForge mixin config declared a refmap its build setup did not generate.
  Remove the obsolete declaration and generation/copy setup because this
  version uses official names in development and production. Preserve the
  Forge branches' required mapping/refmap setup.

NeoForge Channeling uses the existing vanilla enchantment JSON override, whose
predicates include `#c:tridents`. A higher-priority datapack that replaces that
definition can change its behavior; no extra manual lightning is spawned
alongside the 1.21 data-driven effects. Older Forge versions retain their
version-appropriate enchantment handling. Lightning rods were introduced after
1.16.5, so that behavior does not apply to the oldest port.

## Version-specific implementation

| Minecraft / pinned loader | Config and flight APIs checked | Trident and compatibility scope |
| --- | --- | --- |
| 1.16.5 / Forge 36.2.39 | Existing COMMON preload hardened; `ModConfig.Loading`/`Reloading`, `ForgeConfigSpec.setConfig` and explicit startup gear snapshot. Native item flight hooks use the 1.16 entity/item types. | Separate-perspective held/charging models, Loyalty interpolation, enchanted NBT reload state and release durability guard. Existing legacy owner pickup retained. Better Combat and lightning rods are not applicable. |
| 1.18.2 / Forge 40.2.17 | Existing COMMON preload hardened; `ModConfigEvent.Loading`/`Reloading`, `ForgeConfigSpec.setConfig` and explicit startup gear snapshot. Native `canElytraFly`/`elytraFlightTick` hooks. | Held/charging models, reload listener, returning `tryPickup`, lightning-rod Channeling, Loyalty/NBT/durability corrections and optional Better Combat presets. |
| 1.19.2 / Forge 43.3.0 | Same config lifecycle and native flight hooks as the 1.18.2 port, checked against its pinned APIs. | Held/charging models, reload listener, returning `tryPickup`, lightning-rod Channeling, Loyalty/NBT/durability corrections and optional Better Combat presets. |
| 1.20.1 / Forge 47.3.0 | Same config lifecycle and native flight hooks as the 1.18.2 port, checked against its pinned APIs. | Held/charging models, reload listener, returning `tryPickup`, lightning-rod Channeling, Loyalty/NBT/durability corrections and optional Better Combat presets. |
| 1.21.1 / NeoForge 21.1.65, loader 4.0.24 | Native STARTUP registration with listeners installed first; `ModConfigSpec.gameRestart` preserves gear caches. Native flight hooks use the vanilla Elytra durability gate. | Constructor/damage omissions repaired; base-arrow item persistence with legacy-tag migration, saved dealt-damage state, returning pickup, used-hand durability, custom dispenser projectile and 1.21 enchantment effects. Held/charging models, reload listener and optional Better Combat presets. |

These differences follow each pinned API rather than applying the newest
implementation unchanged to older releases. All five versions share configurable
mining speed, horse protection, expanded stat bounds, native winged flight and
truthful tooltips. Normal flight gates still apply, and almost-broken winged
armor disables flight.

## Configuration caveats

`dragonloot-common.toml` keeps its existing name and nested keys. Gear and perk
settings require a full client/server process restart. Scale/anvil settings can
reload. These configs are not automatically synchronized; server and clients
must use matching gear settings.

Forge's `worldRestart` setting marks entries for the configuration UI but its
reload path still clears value caches. An explicit one-time gear snapshot keeps
material getters consistent with values captured by item constructors. The
early preload now copies the temporary file config into memory before closing
it, honors Forge's first-run `defaultconfigs` location, and fails clearly if
startup configuration cannot be read. NeoForge STARTUP loads before registration,
and `gameRestart` entries retain their baked cache values on reload.

The compatibility option retains its existing fixed durability floor and
sword/axe modifier. It does not add Advanced Netherite bonus tool/mob drops or
copy its per-world config. The referenced Fabric progression addon does not
establish native Forge/NeoForge compatibility.

The armor cap applies to the total attribute; it does not guarantee arbitrary
config values are balanced. Tool base damage is a material bonus rather than
the final weapon damage. Durability bounds prevent integer overflow using each
implementation's actual multiplier: maximum armor base 35 in the four Forge
ports, native armor base 16 in NeoForge, and tool base 67 in all ports.

## Evidence and remaining validation

- [NeoForge enchantments](https://docs.neoforged.net/docs/1.21.1/resources/server/enchantments/): 1.21 damage/Channeling/Riptide behavior uses data-driven effects.
- [Caelus flight mixin](https://github.com/illusivesoulworks/caelus/blob/1.21.x/common/src/main/java/com/illusivesoulworks/caelus/mixin/core/MixinLivingEntity.java) and [NeoForge platform](https://github.com/illusivesoulworks/caelus/blob/1.21.x/neoforge/src/main/java/com/illusivesoulworks/caelus/platform/services/CaelusNeoForge.java): native item hooks participate in its flight evaluation. Older ports require matching Caelus releases for integration testing.
- [Forge 1.16 native item hooks](https://github.com/MinecraftForge/MinecraftForge/blob/1.16.x/src/main/java/net/minecraftforge/common/extensions/IForgeItem.java) and [player flight patch](https://github.com/MinecraftForge/MinecraftForge/blob/1.16.x/patches/minecraft/net/minecraft/entity/player/PlayerEntity.java.patch): the oldest maintained port also supports native winged flight.
- [Better Combat fallback rules](https://github.com/ZsoltMolnarrr/BetterCombat/blob/1.21.1/common/src/main/java/net/bettercombat/config/FallbackConfig.java), [integration guidance](https://github.com/ZsoltMolnarrr/BetterCombat/tree/1.21.1#-integrate-your-mod) and [published versions](https://www.curseforge.com/minecraft/mc-mods/better-combat-by-daedelus/files/all): existing name matching, optional preset format and release availability were checked separately.
- [Advanced Netherite configuration](https://github.com/Autovw/AdvancedNetherite/wiki/Configuration) and [armor](https://github.com/Autovw/AdvancedNetherite/wiki/Armors): upstream defaults support the mining/toughness observations.
- [Fabric progression addon](https://www.curseforge.com/minecraft/mc-mods/dragonloot-advanced-netherite-progression): its listed Fabric 1.20.1 support does not establish support for these native ports.
- [Port project](https://www.curseforge.com/minecraft/mc-mods/enderdragon-loot) and [original Fabric source](https://github.com/GitPois1x/DragonLoot): preserve port identity and upstream attribution separately.

Config and flight APIs were checked against exact source artifacts for
[Forge 36.2.39](https://maven.minecraftforge.net/net/minecraftforge/forge/1.16.5-36.2.39/forge-1.16.5-36.2.39-sources.jar),
[Forge 40.2.17](https://maven.minecraftforge.net/net/minecraftforge/forge/1.18.2-40.2.17/forge-1.18.2-40.2.17-sources.jar),
[Forge 43.3.0](https://maven.minecraftforge.net/net/minecraftforge/forge/1.19.2-43.3.0/forge-1.19.2-43.3.0-sources.jar),
[Forge 47.3.0](https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.3.0/forge-1.20.1-47.3.0-sources.jar),
[NeoForge 21.1.65](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.65/neoforge-21.1.65-sources.jar)
and [loader 4.0.24](https://maven.neoforged.net/releases/net/neoforged/fancymodloader/loader/4.0.24/loader-4.0.24-sources.jar).
The pinned [NeoForge installer](https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.65/neoforge-21.1.65-installer.jar)
and [NeoForm configuration](https://maven.neoforged.net/releases/net/neoforged/neoform/1.21.1-20240808.144430/neoform-1.21.1-20240808.144430.zip)
confirm the production pipeline uses Mojang names for classes, fields and methods.

This audit records source-level findings and prepared changes, not a claim that
every build or gameplay scenario has passed. Server regression suites are available for all five ports: GameTests on
1.18.2 and later, and an isolated smoke mod on 1.16.5. Per-version runtime
results are recorded in [RUNTIME-RESULTS.md](RUNTIME-RESULTS.md). Other code, such as anvil
packet timing, dynamic recipe priority, legacy Fabric metadata and unlisted
mixins, warrants separate review. See [TESTING.md](TESTING.md) for build results,
runtime cases and the release gate.
