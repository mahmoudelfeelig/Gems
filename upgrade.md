# Upgrading Minecraft / Fabric versions

This mod is set up for Fabric + modern Minecraft. When updating to a newer Minecraft version, expect small churn in Yarn names, mixin targets, and Fabric API.

## 1: Bump versions

Edit `gradle.properties`:

- `minecraft_version` (ex: `1.21.2`)
- `yarn_mappings` (ex: `1.21.2+build.X`)
- `loader_version`
- `fabric_version`
- `modmenu_version` (optional; only needed if you want Mod Menu integration at that version)

If you update Minecraft, update `fabric.mod.json` `depends.minecraft` as well.

## 2: Update build tooling

- Keep using a supported Gradle + Loom combo (Loom releases track MC/Fabric changes).
- Run `./gradlew build` and fix compilation errors first (these are usually straightforward Yarn renames).

## 3: Fix mixins and mappings drift

When MC updates, the most common breakages are:

- Mixin targets/method descriptors changed (update inject points or remap names).
- Client-only classes moved/renamed (keep client mixins only in `gems.client.mixins.json`).
- Screen/GUI method signatures changed (e.g., `HandledScreen` rendering methods).

Recommendation:

- Run `./gradlew runClient` and `./gradlew runServer` once after bumping versions.
- If a crash mentions a mixin apply error, start with the exact method signature in the crash report.

## 4: Re-run tests and perf CI

- Unit tests: `./gradlew test`
- GameTests: `./gradlew gametest`
- Perf workflow: validate that your server can start headless and that the RCON handshake still works.

## 5: Resource pack metadata

If Minecraft bumps the pack format, update `src/main/resources/pack.mcmeta` accordingly.

