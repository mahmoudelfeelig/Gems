## Server ops guide (Gems)

### Requirements
- Minecraft: `1.21.1`
- Fabric Loader: `>=0.16.11`
- Fabric API: `>=0.114.0+1.21.1` (required)

Optional (recommended for profiling/testing):
- `spark` (profiling)
- `carpet` (fake players/bots)

### Install
- Put `gems-<version>.jar` in your server `mods/` folder.
- Put `fabric-api-<version>.jar` in `mods/` as well.
- Start the server once to generate configs.

### Config
- Main balance file: `config/gems/balance.json`
- Reload at runtime (op level 2+): `/gems reloadBalance`
- Dump effective (clamped) values (op level 2+): `/gems dumpBalance` -> writes `config/gems/balance.effective.json`
- Client config: `config/gems/client.json` (per-client, not synced)
  - `passivesEnabled`: toggle passives on/off
  - `controlMode`: `CHORD` (modifier + hotbar) or `CUSTOM` (dedicated keybinds)
- ModMenu config edits are op-only in multiplayer; non-ops see a read-only view.
- All keybinds are customizable in Options -> Controls -> Gems.

### Permissions
- Player commands: `/gems status`, `/gems trust`, `/gems untrust`, `/gems trustlist`, `/gems trade ...`, `/gems track <player>`
- Operator-only commands require permission level **2+** under `/gems admin ...`

### Admin commands (op 2+)
- State:
  - `/gems admin status <player>`
  - `/gems admin resync <players>`
  - `/gems admin reset <player>`
  - `/gems admin setGem <player> <gem>`
  - `/gems admin setEnergy <players> <energy>`
  - `/gems admin setHearts <player> <hearts>`
  - `/gems admin giveItem <player> <heart|energy_upgrade|gem_trader|gem_purchase|tracker_compass|recall_relic|hypno_staff|earthsplitter_pick|supreme_helmet|supreme_chestplate|supreme_leggings|supreme_boots|blood_oath_blade|demolition_blade|hunters_sight_bow|third_strike_blade|vampiric_edge>`
- Casting (for testing):
  - `/gems admin cast <player> <slot>`
- Perf/stress (for CI + profiling):
  - `/gems admin perf reset`
  - `/gems admin perf snapshot [windowTicks]`
  - `/gems admin stress start <players> <seconds> <periodTicks> <realistic|force> <cycleGems> <forceEnergy10>`
  - `/gems admin stress stop <players>`

### Recommended JVM settings
Keep these consistent with your host resources; a typical baseline:
- `-Xms4G -Xmx4G` (or higher if you have RAM; keep Xms==Xmx for stable GC)
- Java 21 (required by the project toolchain)

### Automated “ability correctness” tests (optional)
If you want automated end-to-end checks beyond unit tests, consider adding Fabric GameTest:
- Spawn a controlled world, give a test player a gem, and assert cooldowns/state changes after ability activation.
- These tests can run headlessly in CI and catch regressions that unit tests won’t.

This repo includes a small GameTest suite under `src/gametest/`:
- Run locally: `./gradlew gametest`
- Output report: `build/reports/gametest.xml`
- Run directory: `run/gametest/` (EULA is auto-accepted for the dev run)
