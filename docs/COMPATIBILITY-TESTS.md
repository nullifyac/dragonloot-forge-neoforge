# Optional integration tests

Optional mods are not mandatory Dragon Loot dependencies. The
[pinned manifest](../scripts/compatibility/manifest.json) records loader targets,
project/version IDs, official download/source links, required dependencies,
actual JAR metadata versions and checksums. Use its complete dependency closure;
a matching Minecraft version alone does not establish loader compatibility.

| Minecraft | Better Combat profile | External wing provider in pinned profiles |
| --- | --- | --- |
| 1.16.5 | Unavailable | None; the selected Enigmatic Legacy lacks the newer back-slot wings |
| 1.18.2 | Available | Enigmatic Legacy |
| 1.19.2 | Available | Enigmatic Legacy |
| 1.20.1 | Available | Enigmatic Legacy and Icarus |
| 1.21.1 | Available | Icarus |

Caelus and Advanced Netherite profiles are provided for all five ports. Other
individual profiles are available only where the manifest selects matching
upstream builds. The manifest is a reproducible test set, not a guarantee for
other releases or arbitrary modpacks.

## Run a profile

From the repository root in PowerShell, replace the JDK placeholder:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/compatibility/Prepare-CompatibilityMods.ps1 -AssetRoot ./release/test-assets
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/compatibility/Run-CompatibilityProfile.ps1 -Minecraft 1.20.1 -Profile all -ModVersion 1.1.16-dev -AssetRoot ./release/test-assets -JavaHome 'C:/path/to/jdk17'
```

Select another supported Minecraft version and its JDK: Java 8 for 1.16.5,
17 for 1.18.2-1.20.1, or 21 for 1.21.1. Individual profile names are `caelus`,
`better-combat`, `advanced-netherite`, `enigmatic-legacy` and `icarus`.
Use the same asset root for every helper. To stage the selected optional JARs
for a separate production/client instance:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/compatibility/Stage-CompatibilityProfile.ps1 -Minecraft 1.20.1 -Profile all -AssetRoot ./release/test-assets
```

Downloads are checksum-verified; staging rejects unexpected JARs. The opt-in
Gradle initializer checks loader pins and hashes, adds Forge dependencies through
`fg.deobf`, and isolates test directories. It does not add dependencies to normal
mod builds. The runner uses a 2 GB game heap, one Gradle worker and a 1 GB Gradle
heap, requires at least 4 GiB free physical memory, and holds a per-module lock.
Avoid concurrent builds of that module.

Each run preserves logs, launch metadata and native exit status under its asset
root. `Get-CompatibilityResult.ps1` verifies expected loaded mod versions,
required-case counts and applicable integration markers, then writes a
`.verified-result.json`. The 1.16.5 smoke JSON must also report zero failed
assertions; a normal server shutdown alone is insufficient.

## Assertions and boundaries

Better Combat tests inspect the live server weapon registry: Dragon sword, axe
and trident must resolve their intended category, attack combo and range. The
pinned older APIs expose `attackRange`; the NeoForge 2.4 API exposes `rangeBonus`.
Client attacks, animation and resource-pack priority still need manual checks.

The ordinary gear/projectile suites run alongside the selected mods. They cover
registered stats, mining speed, horse defense, frozen startup gear settings,
native flight eligibility, passive wear, throwing, pickup and persistence.

Applicable Curios cases equip the registered upstream wing item in its real
back slot and require the Caelus attribute grant. Empty-chest and vanilla-armor
controls accompany ordinary Dragon armor. These fixtures invoke the Caelus
server flight-request handler with a synthetic sender and tick the real
ServerPlayer through `doTick`; FakePlayer's ordinary `tick` is empty. Removal must
restore the captured pre-equip attribute and stop flight. Compare against that
baseline rather than assuming zero, because Caelus defaults vary by version.
The fixtures do not assign the grant or flight flag themselves.

Those assertions do not transmit real client requests or verify graphics.
Repeat grant/removal, native wings, rockets and combat in a fresh production
client and on a dedicated server. The pinned provider items are
`enigmaticlegacy:enigmatic_elytra` on 1.18.2-1.20.1 and
`icarus:white_feathered_wings` on 1.20.1/1.21.1. The legacy 1.16.5 profile has no
applicable external-wing case.

Advanced Netherite configuration and bonus drops are not inherited by Dragon
Loot. Configure Dragon gear explicitly and test pack-specific recipes/datapacks
separately. Development checks do not establish reobfuscated production loading;
see [packaged server and client testing](TESTING.md).

## Upstream references

- [Better Combat source](https://github.com/ZsoltMolnarrr/BetterCombat).
- [Caelus source](https://github.com/illusivesoulworks/caelus).
- [Curios commands](https://docs.illusivesoulworks.com/curios/commands).
- [ForgeGradle dependencies](https://docs.minecraftforge.net/en/fg-6.x/dependencies/)
  and [NeoGradle](https://docs.neoforged.net/toolchain/docs/plugins/ng/).
