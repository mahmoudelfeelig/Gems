# Gems
a Minecraft mod that adds season-inspired gems with unique powers for each holder.

- Target: Fabric on modern Minecraft (1.21.1+; bump to latest Fabric API when available).
- Design: composition-first, low-overhead systems; new gems should be easy to add.
- Specs: see `checklist.md` for the exhaustive feature list (energy, leveling, items, recipes, per-gem passives/abilities).
- Player docs: `abilities.md` (what each gem does) and `balance.md` (balancing knobs + clamps).
- Balancing: edit `config/gems/balance.json` (generated on first run) and restart to apply changes.

## Server install

- Build a jar with `./gradlew build` and copy `build/libs/*.jar` into your server’s `mods/` folder (with Fabric Loader + Fabric API installed).
