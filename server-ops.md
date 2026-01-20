# Gems — Server Owner Guide

This guide is written for Minecraft server owners and admins (not mod developers).

## Requirements

- Minecraft: `1.21.11`
- Fabric Loader: `>=0.18.4` (required)
- Fabric API: `>=0.140.2+1.21.11` (required)

Optional:
- `spark` (profiling)

## Install

1. Install Fabric Loader and Fabric API on your server.
2. Put `gems-<version>.jar` in your server `mods/` folder.
3. Start the server once using the command `java -Xmx2G -jar fabric-server-mc.1.21.11-loader.0.18.4-launcher.1.1.1.jar` to generate configs under `config/gems/`.

## Config files

- Main balance file: `config/gems/balance.json`
  - Reload at runtime (op level 2+): `/gems reloadBalance`
  - Dump effective (clamped) values (op level 2+): `/gems dumpBalance` (writes `config/gems/balance.effective.json`)
- Client config: `config/gems/client.json` (per-client, not synced to server)
  - Players can toggle their own passives and choose keybind mode; see `docs/CONTROLS_AND_HUD.md`.

For config keys, units, and safety clamps, see `balance.md`.

## Permissions and commands

### Player commands

- `/gems status`
- `/gems trust`, `/gems untrust`, `/gems trustlist`
- `/gems trade ...`
- `/gems track <player>`

### Admin commands (op level 2+)

- `/gems reloadBalance`, `/gems dumpBalance`
- Player state:
  - `/gems admin status <player>`
  - `/gems admin resync <players>`
  - `/gems admin reset <player>`
  - `/gems admin setGem <player> <gem>`
  - `/gems admin setEnergy <players> <energy>`
  - `/gems admin setHearts <player> <hearts>`
  - `/gems admin giveItem <player> <itemId>`
  - `/gems admin giveAllGems <players>`
  - `/gems admin clearGems <players>`
  - `/gems admin cooldowns <players> <disabled>`
  - `/gems admin legendaryCooldowns <players> <disabled>`
  - `/gems admin stats show <players>`
  - `/gems admin stats reset <players>`
- Testing helpers:
  - `/gems admin cast <player> <slot>`
  - `/gems admin perf reset`
  - `/gems admin perf snapshot [windowTicks]`
  - `/gems admin stress start <players> <seconds> <periodTicks> <mode> <cycleGems> <forceEnergy10>`
  - `/gems admin stress stop <players>`

## Performance tips

- If your server is large, consider lowering particle/sound load via the global visual knobs in `config/gems/balance.json` (see `balance.md`).
- Use `spark` to capture CPU profiles during heavy PvP and adjust the most expensive abilities (radii, durations, particle caps).

## Troubleshooting

- Config reload fails: `balance.json` is invalid JSON, or values were clamped; use `/gems dumpBalance` to see the effective values.
- Clients can’t change keybinds: keybinds are configured in Minecraft options under Controls -> Gems; client config is not synced server-side.
