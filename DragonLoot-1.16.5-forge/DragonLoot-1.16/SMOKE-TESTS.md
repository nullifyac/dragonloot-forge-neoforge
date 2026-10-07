# Dedicated-server smoke tests

Minecraft 1.16.5 has no native GameTest framework. The `smokeTest` source set
provides a separate development mod loaded by `runSmokeTest`; its classes and
metadata are excluded from production and sources JARs.

From the repository root, replace the Java 8 JDK placeholder:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./DragonLoot-1.16.5-forge/DragonLoot-1.16/run-smoke-tests.ps1 -Java8Home 'C:/path/to/jdk8' -ResultsDirectory ./release/test-results/smoke-1.16.5
```

The runner prepares Forge's launch through Gradle, then starts Java independently
to capture its native exit. It creates isolated servers under `run/smokeTest`,
binds to loopback on an ephemeral port and uses a 2 GB heap with two worker CPUs.
Generated test servers include EULA acceptance and do not reuse gameplay worlds.

Three profiles exercise production defaults, a custom first-run `defaultconfigs`
file, and an edited managed config after a full process restart. Each runs six
core cases covering registered mining/damage/armor/horse/enchantability values;
actual trident release, damage, persistence, Loyalty, owner pickup and durability
guards; native flight eligibility and passive wear; and live scale reloads while
gear values stay frozen. Flight cases include ground, water, levitation,
almost-broken armor and ordinary chest armor.

Success requires every core case, native Java exit 0 and preservation of the
original default config. Inspect `smoke-report.json` and the runner summary:
server shutdown can return 0 even when an assertion fails. Expected values,
actual configs, Minecraft logs and preparation/runtime exits are retained in the
selected results directory. Previous results are archived before reruns and
stale reports are moved aside before each launch.

These are server assertions, not rendering or networked-client checks. See
[projectile limitations](../../docs/PROJECTILE-TESTS.md),
[optional integrations](../../docs/COMPATIBILITY-TESTS.md) and the
[client checklist](../../docs/TESTING.md) for complementary coverage.
