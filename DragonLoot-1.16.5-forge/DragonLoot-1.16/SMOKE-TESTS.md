# Dedicated-server smoke tests

The `smokeTest` source set supplies a separate development mod. It is loaded only
by `runSmokeTest`; neither its classes nor its metadata enter the release JAR or
sources JAR. Minecraft/Forge versions and the production mod version are unchanged.

Run from this module with a Java 8 JDK:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./run-smoke-tests.ps1 -Java8Home 'C:/path/to/java8'
```

The runner creates fresh isolated server directories under `run/smokeTest`, binds
to localhost on an ephemeral port, and starts a real Forge 36.2.39 dedicated
server with a 2 GB heap and two worker CPUs. It prepares the Forge launch through
Gradle, then starts that JVM separately to capture its actual exit status. This
captures the native process exit directly. Some older attached Gradle runs lost
their daemon after tests completed; the cause remains unproven. No
existing client/server worlds are used. The generated test directories include
an EULA acceptance for these local Minecraft test launches.

Each of three profiles checks registered mining speed, sword attributes, tool
and armor durability, armor toughness/protection, horse protection and
enchantability. It exercises actual trident release, collision damage,
save/load, Loyalty acceleration, owner inventory pickup and the durability
guard. Native player flight checks cover ground, water, levitation, almost-broken
armor, ordinary chest armor and passive wear during real player ticks. Config
API reload checks verify live scale settings and frozen gear settings.

The profiles use defaults, custom first-run `defaultconfigs`, and an edited
existing configuration after restarting the same test server. Custom attack
damage/enchantability values exceed the former limits. JSON assertions, expected
values, actual config, Minecraft logs and JVM exit codes are preserved under
`release/test-results/2026-10-06/1.16.5` by default. Override `-ResultsDirectory`
to retain another run separately. Every profile must pass all six core cases,
exit with code 0 and preserve the original default config. Prior results are
archived before reruns; prior runtime logs/reports are moved aside before each
launch, and a separate runner report begins pending. Prepare/runtime output and
exit codes remain available if a launch fails.

These are server checks. They do not establish client rendering or compatibility
with optional mods; those require their own client/integration runs.
