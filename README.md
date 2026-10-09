# DragonLoot - Forge/Neoforge

Original mod (Fabric): [DragonLoot by Pois1x](https://github.com/GitPois1x/DragonLoot)

**DragonLoot** adds Ender Dragon loot and dragon-scale upgrades to Minecraft. This repository contains implementations for **Forge** (1.16.5, 1.18.2, 1.19.2, 1.20.1) and **NeoForge** (1.21.1).

[Advanced Netherite compatibility datapacks](datapack/README.md) are also
available for these ports and the original Fabric mod. Choose the pack matching
your Minecraft version and loader.

## Overview

DragonLoot focuses on late-game progression by:
- Dropping dragon scales from the Ender Dragon (configurable)
- Unlocking dragon-tier armor, tools, and weapons
- Adding ranged upgrades like the Dragon Bow, Crossbow, and Trident
- Introducing the Dragon Anvil and an upgraded chestplate with Elytra

## Repository Structure

```
dragonloot-forge/
|-- DragonLoot-1.16.5-forge/
|   `-- DragonLoot-1.16/            # Minecraft 1.16.5 (Forge)
|-- DragonLoot-1.18.2-forge/
|   `-- DragonLoot-1.18/            # Minecraft 1.18.2 (Forge)
|-- DragonLoot-1.19.2-forge/
|   `-- DragonLoot-1.19/            # Minecraft 1.19.2 (Forge)
|-- DragonLoot-1.20.1-forge/
|   `-- DragonLoot-1.20/            # Minecraft 1.20.1 (Forge)
|-- DragonLoot-1.21.1-neoforge/
|   `-- DragonLoot-1.21/            # Minecraft 1.21.1 (NeoForge)
|-- datapack/                      # Versioned compatibility datapacks
`-- release/                       # Local built JARs (ignored by Git)
```

## Quick Start

### Prerequisites
- Java Development Kit (JDK) 8 for 1.16.5
- Java Development Kit (JDK) 17 for 1.18.2-1.20.1
- Java Development Kit (JDK) 21 for 1.21.1
- Gradle (included via gradlew)

### Building a Version

Navigate to the desired version directory and run:

```bash
cd DragonLoot-1.20.1-forge/DragonLoot-1.20
./gradlew build
```

On Windows, you can also use:

```bash
.\gradlew.bat build
```

The compiled mod will be available in `build/libs/`.

## Features

### Core Additions
- Dragon scales drop from the Ender Dragon (configurable)
- Dragon armor set, horse armor, and dragon tools
- Dragon sword, bow, crossbow, and trident
- Dragon Anvil block

### Upgrades and Crafting
- Smithing upgrades that turn netherite gear into dragon gear
- Upgraded dragon chestplate that merges with Elytra
- Fire-resistant dragon equipment

### Configuration
Tune balance in `config/dragonloot-common.toml`:
- Scale drop amounts and extra rolls per player
- Armor and tool durability, damage, mining speed, and enchantability
- Dragon Anvil level-cap behavior

Gear and perk settings require a full game/server restart. Use matching gear
settings on the server and clients; this COMMON config is not synchronized.

## Version-Specific Details

### Forge Versions (1.16.5, 1.18.2, 1.19.2, 1.20.1)
Each Forge version includes:
- Forge loader integration
- Data-driven recipes and mixin tweaks

### NeoForge (1.21.1)
Latest version with:
- NeoForge loader integration
- Updated registrations and render hooks

## Development

All five supported Minecraft versions are actively maintained. Check changes
against each applicable port's APIs and document version-specific limitations.
See the [testing guide](docs/TESTING.md) for build validation and regression
commands, [optional integration tests](docs/COMPATIBILITY-TESTS.md) for dependency
profiles, and [build reproducibility](docs/BUILD-REPRODUCIBILITY.md) for tool pins.
Player-facing changes are listed in the module changelogs and
[1.1.16 release notes](docs/RELEASE-1.1.16.md).

Known issue: affected Forge/NeoForge servers can kick players during long falls
with `allow-flight=false`, including without Dragon Loot installed. This is an
upstream loader issue; [details and tested scope](docs/NETWORK-CONTROLS.md).

### Project Structure
Each version follows the standard Minecraft mod development structure:
```
DragonLoot-1.20/
|-- src/
|   |-- main/
|   |   |-- java/          # Source code
|   |   `-- resources/     # Assets and configs
|-- build.gradle           # Build configuration
|-- gradle.properties      # Gradle properties
`-- settings.gradle        # Gradle settings
```

### Building from Source
1. Clone the repository
2. Navigate to your desired version
3. Run `./gradlew build` (or `.\gradlew.bat build` on Windows)
4. Find the built JAR in `build/libs/`

### Dependencies
- Forge/NeoForge only (no required runtime dependencies)

## License

DragonLoot is licensed under the **GPL-3.0-or-later**.

## Contributing

Contributions are welcome! Please consider:
1. Opening an issue to discuss major changes
2. Testing across all supported versions
3. Keeping config defaults and balance in sync between versions
4. Updating the per-version changelog when behavior changes

### For Players
- Download [DragonLoot](https://www.curseforge.com/minecraft/mc-mods/enderdragon-loot)

## Version Support Timeline

| Version | Modloader | Status | Java |
|---------|-----------|--------|------|
| 1.16.5  | Forge     | Active | 8    |
| 1.18.2  | Forge     | Active | 17   |
| 1.19.2  | Forge     | Active | 17   |
| 1.20.1  | Forge     | Active | 17   |
| 1.21.1  | NeoForge  | Active | 21   |
---

**Last Updated**: October 2026
