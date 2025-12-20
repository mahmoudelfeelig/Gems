# Gems
a Minecraft mod that adds gems with unique powers for each holder.

- Specs: see `checklist.md` for the exhaustive feature list (energy, leveling, items, recipes, per-gem passives/abilities).
- Server setup/ops: see `server-ops.md`.
- Player docs: `abilities.md` (what each gem does) and `balance.md` (balancing knobs + clamps).
- Balancing: edit `config/gems/balance.json` (generated on first run) and run `/gems reloadBalance` (op level 2+) to apply.
- Upgrading Minecraft/Fabric: see `upgrade.md`.

## Server install

- Build a jar with `./gradlew build` and copy `build/libs/*.jar` into your server’s `mods/` folder (with Fabric Loader + Fabric API installed).

## In-game config UI

- Install Mod Menu, then open the Gems config screen from Mod Menu.

## Testing

- Unit tests: `./gradlew test`
- End-to-end integration (Fabric GameTest): `./gradlew gametest` (writes `build/reports/gametest.xml`, uses `run/gametest/`)

## Admin commands

- `/gems reloadBalance` (op 2+)
- `/gems dumpBalance` (op 2+)
- `/gems admin setEnergy <player> <0..10>` (op 2+)
- `/gems admin setHearts <player> <5..20>` (op 2+)
- `/gems admin setGem <player> <gem>` (op 2+)
- `/gems admin reset <player>` (op 2+)
- `/gems admin giveItem <player> <heart|energy_upgrade|trader>` (op 2+)
- `/gems admin giveGem <player> <gem>` (op 2+)
- `/gems admin resync <player>` (op 2+)
- `/gems admin cast <player> <slot 1..10>` (op 2+)
- `/gems admin stress start <players> <seconds> <periodTicks> <realistic|force> <cycleGems> <forceEnergy10>` (op 2+)
- `/gems admin stress stop <players>` (op 2+)
