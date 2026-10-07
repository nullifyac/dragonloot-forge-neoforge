# Dragon Loot 1.1.16

Available for Forge on Minecraft 1.16.5, 1.18.2, 1.19.2 and 1.20.1, and
NeoForge on Minecraft 1.21.1.

## Improvements

- Fixed the Dragon Trident's held and charging 3D models and enchanted appearance
  after resource reloads.
- Improved Loyalty returns, pickup and durability handling, and prevented throws
  with nearly broken tridents. Channeling works on lightning rods on 1.18.2 and
  newer.
- On 1.21.1, fixed Dragon Trident throwing, hit damage and offhand durability,
  and added dispenser support.
- Improved winged chestplate compatibility with Caelus and restored normal flight
  restrictions.
- Added mining speed and horse armor protection settings, expanded configurable
  stat limits and improved configuration loading. Default gear balance is
  preserved.
- Corrected armor perk tooltips and removed unsupported bonus-drop claims.
- Added Better Combat weapon presets for Dragon swords, axes and tridents on
  1.18.2 and newer.

## Configuration and integrations

Fully restart Minecraft or the dedicated server after changing gear or perk
settings. Use matching gear settings on clients and servers; the COMMON config
is not synchronized automatically.

Caelus, Better Combat and Advanced Netherite integrations are optional. Dragon
Loot has no mandatory runtime dependencies beyond its loader. Use matching
Minecraft and loader versions for optional mods and their dependencies. Better
Combat has no matching official 1.16.5 release.

Configure Dragon stats independently to suit Advanced Netherite progression;
its settings and bonus drops are not inherited automatically.

## Known issue

Affected Forge/NeoForge servers can kick players during long falls with
`allow-flight=false`, including without Dragon Loot installed. This is an
independent loader issue. See [details and tested scope](NETWORK-CONTROLS.md).
