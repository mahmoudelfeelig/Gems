# Balance config

Balancing is controlled by `config/gems/balance.json` (generated on first run). Restart the server/client after editing.

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

