# Gems
a Minecraft mod that adds gems with unique powers for each holder.

- Specs: see `checklist.md` for the feature list.
- Server setup/ops: see `server-ops.md`.
- Player docs: `gameplay.md` (what each gem does) and `balance.md` (balancing knobs + clamps).
- Balancing: edit `config/gems/balance.json` (generated on first run) and run `/gems reloadBalance` (op level 2+) to apply.
- Upgrading Minecraft/Fabric: see `upgrade.md`.

## Server install

- Build a jar with `./gradlew build` and copy `build/libs/*.jar` into your server’s `mods/` folder (with Fabric Loader + Fabric API installed).

## In-game config UI

- Install Mod Menu, then open the Gems config screen from Mod Menu.

## Testing

- Unit tests: `./gradlew test`
- End-to-end integration (Fabric GameTest): `./gradlew gametest` (writes `build/reports/gametest.xml`, uses `run/gametest/`)
