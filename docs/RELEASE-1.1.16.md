# Dragon Loot 1.1.16 - October 7, 2026

This release covers Forge 1.16.5, 1.18.2, 1.19.2 and 1.20.1, plus NeoForge 1.21.1.
Each port has its own player-facing [changelog](../DragonLoot-1.16.5-forge/DragonLoot-1.16/CHANGELOG.md)
in its module. Published files are listed on the
[Enderdragon Loot CurseForge project](https://www.curseforge.com/minecraft/mc-mods/enderdragon-loot/files/all).

## Release verification

All five final assemblies exited 0 using the repository's pinned loaders and
matching JDKs. The production JARs passed resource, model, mixin, refmap, Java
target, license and regression-harness exclusion checks; source JARs passed
source-payload and regression-harness exclusion checks. Production
payloads differ from the tested `1.1.16-dev` JARs only by replacing the version in
the manifest and expanded loader metadata. All other entry bytes, including
gameplay classes and resources, are identical. The original source payloads are
identical to their tested development archives. Current source builds additionally
include the module's license; the original published archives remain unchanged.

All five final production JARs then passed fresh packaged dedicated-server checks
with the frozen optional-mod profiles: correct Minecraft/loader/mod versions,
startup, datapack/player listing, world save, normal stop and native Java exit 0.
The selected Prism gameplay, configuration and integration checks are recorded
separately in [RUNTIME-RESULTS.md](RUNTIME-RESULTS.md).

| Minecraft | Loader | Required Java | Production file |
| --- | --- | --- | --- |
| 1.16.5 | Forge 36.2.39 | 8 | `dragonloot-1.1.16-1.16.5-forge.jar` |
| 1.18.2 | Forge 40.2.17 | 17 | `dragonloot-1.1.16-1.18.2-forge.jar` |
| 1.19.2 | Forge 43.3.0 | 17 | `dragonloot-1.1.16-1.19.2-forge.jar` |
| 1.20.1 | Forge 47.3.0 | 17 | `dragonloot-1.1.16-1.20.1-forge.jar` |
| 1.21.1 | NeoForge 21.1.65 | 21 | `dragonloot-1.1.16-1.21.1-neoforge.jar` |

All files support client and server. Better Combat presets apply to 1.18.2 and
newer; there is no matching official 1.16.5 release. Gear configuration changes
require a full restart and matching client/server settings. Mining speed is
configurable; Advanced Netherite's settings are not copied automatically.

## Known limitation

Known issue: affected Forge/NeoForge servers can kick players during long falls
with `allow-flight=false`. This was reproduced without Dragon Loot installed on
NeoForge 21.1.65 and Forge 47.3.0, so it is an upstream loader issue rather than a
Dragon Loot defect. [Details and tested scope](NETWORK-CONTROLS.md).
The unrelated 1.1.17 workaround was withdrawn before CurseForge publication;
1.1.16 remains the published release. The complete reported BMC5 v51 pack remains
untested.

## Reproduction and local evidence

Build all ports from the repository root:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/build-low-memory.ps1 -AllVersions
```

Final artifacts, source JARs, checksums and artifact manifest are under the ignored
`release/1.1.16/` directory. Build/package/payload evidence is in
`release/test-results/2026-10-06/release-1.1.16/final-results.json`; the final
packaged-server matrix is in
`release/test-results/2026-10-06/production-servers/release-1.1.16-final/final-matrix.json`.
Existing development test evidence is retained. Reproduce packaged checks using
the final-release options in [TESTING.md](TESTING.md).
