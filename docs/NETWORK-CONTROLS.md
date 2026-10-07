# Known issue: flying kicks during long falls

Affected Forge/NeoForge dedicated servers can kick players during long falls
with `allow-flight=false`. This occurs without Dragon Loot installed and is an
independent loader issue.

The behavior was reproduced with empty client and server mod directories on
Minecraft 1.20.1 with Forge 47.3.0 and Minecraft 1.21.1 with NeoForge 21.1.65.
Other loader versions have not been confirmed through equivalent gameplay tests.

The affected movement check can mistake continued descent for hovering and kick
the player after roughly four seconds. Short falls can end before the timeout.
Active Elytra-style gliding, including the winged Dragon chestplate, is exempt
from this check.

Singleplayer and dedicated servers configured with `allow-flight=true` bypass
this kick path. That server setting also changes general flight enforcement;
it is not a correction to the affected loader's movement check.

When reporting a flight problem, include the Minecraft and loader versions,
server flight setting, equipped items, other installed mods, and server log.
