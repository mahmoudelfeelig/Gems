# Balance config

Balancing is controlled by `config/gems/balance.json` (generated on first run).

- Apply changes without restart: `/gems reloadBalance` (requires op permission level 2+).
- If the JSON is invalid, the reload fails and the current balance stays active (the file is not overwritten).
- Export the currently-applied (sanitized) values: `/gems dumpBalance` (writes `config/gems/balance.effective.json`).

## Units

- `*CooldownSeconds`, `*DurationSeconds`, `*WindowSeconds`: seconds (converted to ticks internally)
- `*RadiusBlocks`, `*RangeBlocks`, `*SpreadBlocks`, `*HeightBlocks`: blocks
- `*Damage`, `*Heal`: Minecraft health points (2.0 = 1 heart)
- `*Amplifier`: potion amplifier (0 = level I, 1 = level II, etc)

## Sanity clamps

Values are clamped on load to prevent extreme configs from tanking performance or breaking gameplay. The file is not rewritten; the runtime values are sanitized.

Common clamps:
- Cooldowns: `0..3600s` (most); `richRushCooldownSeconds` up to `24h`
- Durations: usually `0..120s` (some up to `10m`)
- Most AOE radii: `0..32 blocks`
- Long ranges (beam/tag): `0..128–256 blocks` depending on the ability
- Damages: generally clamped to `0..40` (20 hearts) unless noted

## High-impact knobs (recommended)

- Astra: `dimensionalVoidRadiusBlocks`, `dimensionalVoidDurationSeconds`, `unboundedDurationSeconds`
- Fire: `heatHazeRadiusBlocks`, `meteorShowerCount`, `meteorShowerSpreadBlocks`
- Flux: `fluxBeamArmorDamageAt100`, `overchargeSelfDamagePerSecond`, `overchargePerSecond`
- Life: `lifeCircleMaxHealthDelta`, `vitalityVortexScanRadiusBlocks`
- Puff: `dashVelocity`, `breezyBashImpactDamage`, `groupBashRadiusBlocks`
- Speed: `speedStormRadiusBlocks`, `speedStormEnemySlownessAmplifier`
- Strength: `chadEveryHits`, `chadBonusDamage`
- Wealth: `fumbleRadiusBlocks`, `hotbarLockDurationSeconds`

## Visual budget knobs (server)

These are global caps/switches for particles/sounds emitted by abilities (useful for large servers).

- `visual.enableParticles`: master switch for all server-sent particles.
- `visual.enableSounds`: master switch for ability sounds.
- `visual.particleScalePercent`: scales all particle counts (0..200).
- `visual.maxParticlesPerCall`: hard cap for a single particle spawn call (0..2048).
- `visual.maxBeamSteps`: hard cap for beam “steps” (0..2048).
- `visual.maxRingPoints`: hard cap for ring/aura “points” (0..2048).
