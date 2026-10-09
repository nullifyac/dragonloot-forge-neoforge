# Reproducible builds

Use each module's Gradle wrapper and the matching JDK. Build plugins and loaders
are pinned to these versions:

| Minecraft | Loader | JDK | Gradle wrapper | Build plugin | MixinGradle |
| --- | --- | --- | --- | --- | --- |
| 1.16.5 | Forge 36.2.39 | 8 | 6.8.3 | ForgeGradle 4.1.16 | 0.7.38 |
| 1.18.2 | Forge 40.2.17 | 17 | 7.6.2 | ForgeGradle 5.1.77 | 0.7.38 |
| 1.19.2 | Forge 43.3.0 | 17 | 7.6.2 | ForgeGradle 5.1.77 | 0.7.38 |
| 1.20.1 | Forge 47.3.0 | 17 | 8.1.1 | ForgeGradle 6.0.54 | 0.7.38 |
| 1.21.1 | NeoForge 21.1.65 | 21 | 8.14 | NeoGradle 7.1.20 | Not applied |
| 26.1.2 | NeoForge 26.1.2.114 | 25 | 9.2.1 | ModDevGradle 2.0.148 | Not applied |

Set `JAVA_HOME` to the appropriate JDK and run from the chosen module:

```sh
./gradlew clean assemble --no-daemon --max-workers=1
```

On Windows, use `gradlew.bat`. The repository's PowerShell helper selects the
matching installed JDK and builds all six modules sequentially with one worker
and a 1 GB Gradle heap:

```powershell
./scripts/build-low-memory.ps1 -AllVersions -Tasks @('clean', 'assemble')
```

Production and source JARs are written to each module's `build/libs/` directory.
Both include the module license and exclude development GameTest and smoke-test
code. Gear or runtime checks are described in [TESTING.md](TESTING.md).

Forge 1.16.5 disables incremental main Java compilation so Mixin processing
regenerates the complete refmap and shadow mappings. Keep this safeguard when
changing its build configuration. A clean build also removes stale classes after
source files or mixin registrations are removed.

## Compare artifacts

Preserve a reference JAR before rebuilding. Use the same source revision, mod
version, pinned dependencies and JDK when comparing builds. With Python 3.10 or
newer:

```sh
python scripts/compare-jar-payloads.py reference.jar rebuilt.jar --output comparison.json
```

The comparison checks every non-directory entry, rejects duplicate entries and
returns exit code 1 if entries are added, missing or changed. Run it separately
for production and source JARs. ZIP timestamps and compression can change the
archive hash even when all contained bytes match.

Resource validation requires Python 3.11 or newer. Validate source resources and
each built production JAR with
[validate_resources.py](../scripts/validate_resources.py):

```sh
python scripts/validate_resources.py
python scripts/validate_resources.py --version 1.20.1 --jar DragonLoot-1.20.1-forge/DragonLoot-1.20/build/libs/dragonloot-1.1.16.jar
```

This checks metadata, model references, declared mixins, Forge refmap packaging,
Java targets, license inclusion and development-test exclusions. Artifact checks
complement the runtime checks; they do not establish gameplay or compatibility.
