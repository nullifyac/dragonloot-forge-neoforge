# Withdrawn 1.1.17 prototype - October 7, 2026

The experimental 1.1.17 build patched an independent Forge/NeoForge movement
defect affecting long falls. That change is outside Dragon Loot's scope and has
been removed from the maintained source on all five supported Minecraft versions.
The upstream-only regression suites were also removed.

**1.1.17 was never uploaded to CurseForge and must not be published.**
Dragon Loot's current mod version remains 1.1.16. Its existing published files
are unchanged; see [the release record](RELEASE-1.1.16.md).

The existing `v1.1.17` Git tag is retained as a historical prototype rather than
rewritten. Local prototype artifacts are archived under the ignored
`release/withdrawn/1.1.17/` directory, outside the active release staging area.
Earlier test evidence remains unchanged under `release/test-results/`.

The loader problem is documented only as a
[known issue with its tested scope](NETWORK-CONTROLS.md).

The source-JAR license inclusion and the Minecraft 1.16.5 safeguard against
incomplete Mixin mappings remain because they concern Dragon Loot's own builds.

All five restored ports passed clean assemblies and resource/package checks.
Every decompressed production-JAR entry is byte-identical to the corresponding
published 1.1.16 artifact; source archives differ only by the added module
license. The withdrawn hooks and test classes are absent. Published and archived
prototype JARs retain their original checksums. Verification is recorded locally
under `release/test-results/2026-10-07/scope-correction/`; these rebuild checks do
not claim a new gameplay test run.
