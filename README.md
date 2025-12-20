# Gems
a Minecraft mod that adds season-inspired gems with unique powers for each holder.

- Target: Fabric on modern Minecraft (1.21.1+; bump to latest Fabric API when available).
- Design: composition-first, low-overhead systems; new gems should be easy to add.
- Specs: see `checklist.md` for the exhaustive feature list (energy, leveling, items, recipes, per-gem passives/abilities).
- Player docs: `abilities.md` (what each gem does) and `balance.md` (balancing knobs + clamps).
- Balancing: edit `config/gems/balance.json` (generated on first run) and run `/gems reloadBalance` (op level 2+) to apply.

## Server install

- Build a jar with `./gradlew build` and copy `build/libs/*.jar` into your server’s `mods/` folder (with Fabric Loader + Fabric API installed).

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
