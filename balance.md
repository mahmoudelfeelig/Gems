# Balance config

Balancing is controlled by `config/gems/balance.json` (generated on first run).

- Apply changes without restart: `/gems reloadBalance` (requires op permission level 2+).
- If the JSON is invalid, the reload fails and the current balance stays active (the file is not overwritten).
- Export the currently-applied (sanitized) values: `/gems dumpBalance` (writes `config/gems/balance.effective.json`).

Player-facing behavior docs live under `docs/` (start at `docs/README.md`). This file focuses on configuration keys, units, and safety constraints.

## Units

- `*CooldownSeconds`, `*DurationSeconds`, `*WindowSeconds`: seconds (converted to ticks internally)
- `*RadiusBlocks`, `*RangeBlocks`, `*SpreadBlocks`, `*HeightBlocks`: blocks
- `*Damage`, `*Heal`: Minecraft health points (2.0 = 1 heart)
- `*Amplifier`: potion amplifier (0 = level I, 1 = level II, etc)
- `*ScalePercent`: percentage scaling (100 = no change)

## Sanity clamps

Values are clamped on load to prevent extreme configs from tanking performance or breaking gameplay. The file is not rewritten; the runtime values are sanitized.

Typical clamps:
- Cooldowns: `0..3600s` (most); some long systems allow longer windows.
- Durations: usually `0..120s` (some allow longer).
- Most AOE radii: `0..32 blocks`

## Visual budget knobs (server)

Global caps/switches for particles/sounds emitted by abilities (useful for large servers).

- `visual.enableParticles`: master switch for all server-sent particles.
- `visual.enableSounds`: master switch for ability sounds.
- `visual.particleScalePercent`: scales all particle counts (0..200).
- `visual.maxParticlesPerCall`: hard cap for a single particle spawn call (default 128).
- `visual.maxBeamSteps`: hard cap for beam particle steps (default 256).
- `visual.maxRingPoints`: hard cap for ring/aura particle points (default 128).

## Config layout (top-level keys)

The balance file mirrors the in-code schema in `src/main/java/com/feel/gems/config/GemsBalanceConfig.java`.

- `visual`: global particles/sounds caps.
- `systems`: hearts + assassin tuning, controlled mob follow behavior.
- Per-gem sections: `astra`, `fire`, `flux`, `life`, `puff`, `speed`, `strength`, `wealth`, `terror`, `summoner`, `space`, `reaper`, `pillager`, `spyMimic`, `beacon`, `air`, `duelist`, `hunter`, `sentinel`, `trickster`.
- `legendary`: legendary crafting limits, tracker/recall/hypno/earthsplitter tuning, supreme set effects, and `recipeGemRequirements` for discount recipes.
- `bonusPool`: per-bonus ability/passive tuning (cooldowns, damage, ranges, effect multipliers, etc).
- `mobBlacklist`: universal mob blacklist used by Hypno Staff, Summoner summons, and Astra Soul Capture.

## Recipe gem requirements (discount recipes)

`legendary.recipeGemRequirements` is a map of `recipe_id -> required_active_gem_id` for discount recipes (e.g. `gems:tracker_compass_discount` -> `spy_mimic`).

## Client config note

Client-side settings are stored in `config/gems/client.json` (per-client, not synced to server). For keybind modes and HUD controls, see `docs/CONTROLS_AND_HUD.md`.

