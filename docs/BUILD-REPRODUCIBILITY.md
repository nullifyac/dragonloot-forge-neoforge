# Build plugins and artifact comparison

The maintenance builds use the exact ForgeGradle versions loaded during runtime validation. Dynamic plugin selectors have been replaced with fixed versions; NeoGradle was already fixed.

| Minecraft | Loader | JDK | Build plugin | MixinGradle |
| --- | --- | --- | --- | --- |
| 1.16.5 | Forge 36.2.39 | 8 | ForgeGradle 4.1.16 | 0.7.38 |
| 1.18.2 | Forge 40.2.17 | 17 | ForgeGradle 5.1.77 | 0.7.38 |
| 1.19.2 | Forge 43.3.0 | 17 | ForgeGradle 5.1.77 | 0.7.38 |
| 1.20.1 | Forge 47.3.0 | 17 | ForgeGradle 6.0.54 | 0.7.38 |
| 1.21.1 | NeoForge 21.1.65 | 21 | NeoGradle 7.1.20 | Not applied |

The previously resolved MixinGradle `0.7-SNAPSHOT` and the published `0.7.38` have identical decompressed code and resources, identical entry sets, and identical dependency metadata. Their only differing JAR entry is `META-INF/MANIFEST.MF`, which changes the version label. Both identify Jenkins build 38 and Git commit `f800b26d2b180d98d9aa9355e5b3086d71218508`. The fixed version therefore preserves the tested implementation.

The tested snapshot SHA-256 is `61d4dd3a40eeaf126ac9c5ffdea74c284dfcb34a38baa256f7c99d023a0a04a9`; the fixed `0.7.38` SHA-256 is `f347b0edd348802cebeddc966eb37d3631b364f14fa6c2513ff0900c783b7da7`. Official publication evidence is available in the [0.7.38 POM](https://repo.spongepowered.org/repository/maven-public/org/spongepowered/mixingradle/0.7.38/mixingradle-0.7.38.pom), [plugin marker POM](https://repo.spongepowered.org/repository/maven-public/org/spongepowered/mixin/org.spongepowered.mixin.gradle.plugin/0.7.38/org.spongepowered.mixin.gradle.plugin-0.7.38.pom), and [snapshot metadata](https://repo.spongepowered.org/repository/maven-public/org/spongepowered/mixingradle/0.7-SNAPSHOT/maven-metadata.xml). The metadata captured on 2026-10-06 identifies snapshot `0.7-20230803.124500-15`.

On 2026-10-06, all five modules assembled sequentially with `mod_version=1.1.16-dev`, one Gradle worker and a 1 GB Gradle heap. Every build exited with code 0. Each rebuilt production JAR and source JAR was compared against a preserved copy of the release artifact: all ten comparisons found identical decompressed entry sets and bytes. ZIP timestamps and compression can change raw archive hashes. The original release files were left untouched.

Evidence is under `release/test-results/2026-10-06/build-reproducibility/`: `final-results.json`, `RESULTS.md`, before/after plugin coordinates and hashes, assembly logs and exit codes, per-JAR payload reports, the MixinGradle equivalence report, and preserved prior artifacts. This verifies the plugin pin change against the prepared release artifacts; it does not claim dependency locking or a build without existing dependency caches.

The follow-up helper audit captured real success and invalid-option JVM exit codes on JDKs 8, 17 and 21, exercised a controlled smoke preparation failure with stale successful artifacts present, and reviewed saved config/restart evidence. All eight checks passed; a separate comparison of differing production/source archives correctly returned exit code 1. Evidence is in `helper-status-checks/`. These checks exercised failure reporting without repeating the already successful Minecraft suites. The runners now move previous logs/reports aside before launch, begin new reports pending, retain native output/status on timeouts, verify their isolated working directory, and require the original default config to remain unchanged.

To assemble all five modules again:

```powershell
./scripts/build-low-memory.ps1 -AllVersions -ModVersion 1.1.16-dev
```

To capture actual loaded build plugins for one module, choose an output path outside its release JAR:

```powershell
$auditInit = Join-Path (Get-Location) 'scripts/audit-build-plugins.init.gradle'
$auditOutput = Join-Path (Get-Location) 'release/plugin-audit-1.20.1.json'
./scripts/build-low-memory.ps1 -MinecraftVersion 1.20.1 -ModVersion 1.1.16-dev -Tasks @(
    'assemble', 'auditLoadedBuildPlugins', '-I', $auditInit, "-PpluginAuditOutput=$auditOutput"
)
```

Preserve reference JARs before rebuilding. With Python 3.10 or newer, compare each rebuilt artifact using:

```powershell
python ./scripts/compare-jar-payloads.py ./release/reference.jar ./path/to/build/libs/dragonloot-1.1.16-dev.jar --output ./release/payload-comparison.json
```

The comparison rejects duplicate ZIP entries, includes every non-directory entry, and exits with code 1 if any entry is missing, added, or changed. Development smoke-test and GameTest source sets remain outside production and source release JARs.
