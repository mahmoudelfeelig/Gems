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
- Long ranges (beam/tag): `0..128-256 blocks` depending on the ability
- Damages: generally clamped to `0..40` (20 hearts) unless noted

## Visual budget knobs (server)

These are global caps/switches for particles/sounds emitted by abilities (useful for large servers).

- `visual.enableParticles`: master switch for all server-sent particles.
- `visual.enableSounds`: master switch for ability sounds.
- `visual.particleScalePercent`: scales all particle counts (0..200).
- `visual.maxParticlesPerCall`: hard cap for a single particle spawn call (0..2048).
- `visual.maxBeamSteps`: hard cap for beam "steps" (0..2048).
- `visual.maxRingPoints`: hard cap for ring/aura "points" (0..2048).

## Ability defaults

These are the default values generated in `config/gems/balance.json`.

### Astra Gem
- Shadow Anchor: `shadowAnchorWindowSeconds=10`
- Dimensional Void: `dimensionalVoidCooldownSeconds=60`, `dimensionalVoidDurationSeconds=8`, `dimensionalVoidRadiusBlocks=10`
- Astral Daggers: `astralDaggersCooldownSeconds=8`, `astralDaggersCount=5`, `astralDaggersDamage=4.0`, `astralDaggersVelocity=3.5`, `astralDaggersSpread=0.05`
- Unbounded: `unboundedCooldownSeconds=60`, `unboundedDurationSeconds=3`
- Astral Camera: `astralCameraCooldownSeconds=60`, `astralCameraDurationSeconds=8`
- Spook: `spookCooldownSeconds=30`, `spookRadiusBlocks=10`, `spookDurationSeconds=6`
- Tag: `tagCooldownSeconds=20`, `tagRangeBlocks=30`, `tagDurationSeconds=12`

### Fire Gem
- Cosy Campfire: `cosyCampfireCooldownSeconds=45`, `cosyCampfireDurationSeconds=10`, `cosyCampfireRadiusBlocks=8`, `cosyCampfireRegenAmplifier=3`
- Heat Haze Zone: `heatHazeCooldownSeconds=90`, `heatHazeDurationSeconds=10`, `heatHazeRadiusBlocks=10`, `heatHazeEnemyMiningFatigueAmplifier=0`, `heatHazeEnemyWeaknessAmplifier=0`
- Fireball: `fireballChargeUpSeconds=3`, `fireballChargeDownSeconds=3`, `fireballInternalCooldownSeconds=4`, `fireballMaxDistanceBlocks=60`
- Meteor Shower: `meteorShowerCooldownSeconds=120`, `meteorShowerCount=10`, `meteorShowerSpreadBlocks=10`, `meteorShowerHeightBlocks=25`, `meteorShowerVelocity=1.5`, `meteorShowerExplosionPower=2`

### Flux Gem
- Flux Beam: `fluxBeamCooldownSeconds=4`, `fluxBeamRangeBlocks=60`, `fluxBeamMinDamage=6.0`, `fluxBeamMaxDamageAt100=12.0`, `fluxBeamMaxDamageAt200=24.0`, `fluxBeamArmorDamageAt100=200`, `fluxBeamArmorDamagePerPercent=2`
- Static Burst: `staticBurstCooldownSeconds=30`, `staticBurstRadiusBlocks=8`, `staticBurstMaxDamage=20.0`, `staticBurstStoreWindowSeconds=120`
- Charge item values: `chargeDiamondBlock=25`, `chargeGoldBlock=15`, `chargeCopperBlock=5`, `chargeEmeraldBlock=20`, `chargeAmethystBlock=10`, `chargeNetheriteScrap=35`, `chargeEnchantedDiamondItem=20`
- Overcharge: `overchargeDelaySeconds=5`, `overchargePerSecond=5`, `overchargeSelfDamagePerSecond=1.0`

### Life Gem
- Vitality Vortex: `vitalityVortexCooldownSeconds=30`, `vitalityVortexRadiusBlocks=8`, `vitalityVortexDurationSeconds=8`, `vitalityVortexScanRadiusBlocks=3`, `vitalityVortexVerdantThreshold=10`, `vitalityVortexAllyHeal=2.0`
- Health Drain: `healthDrainCooldownSeconds=12`, `healthDrainRangeBlocks=20`, `healthDrainAmount=6.0`
- Life Circle: `lifeCircleCooldownSeconds=60`, `lifeCircleDurationSeconds=12`, `lifeCircleRadiusBlocks=8`, `lifeCircleMaxHealthDelta=8.0`
- Heart Lock: `heartLockCooldownSeconds=45`, `heartLockDurationSeconds=6`, `heartLockRangeBlocks=20`

### Puff Gem
- Double Jump: `doubleJumpCooldownSeconds=2`, `doubleJumpVelocityY=0.85`
- Dash: `dashCooldownSeconds=6`, `dashVelocity=1.8`, `dashDamage=6.0`, `dashHitRangeBlocks=4.0`
- Breezy Bash: `breezyBashCooldownSeconds=20`, `breezyBashRangeBlocks=10`, `breezyBashUpVelocityY=1.2`, `breezyBashKnockback=0.6`, `breezyBashInitialDamage=4.0`, `breezyBashImpactDamage=6.0`, `breezyBashImpactWindowSeconds=6`
- Group Breezy Bash: `groupBashCooldownSeconds=45`, `groupBashRadiusBlocks=10`, `groupBashKnockback=1.2`, `groupBashUpVelocityY=0.8`

### Speed Gem
- Momentum: `momentumMinSpeed=0.10`, `momentumMaxSpeed=0.60`, `momentumMinMultiplier=0.90`, `momentumMaxMultiplier=1.30`
- Frictionless Steps: `frictionlessSpeedAmplifier=1`
- Arc Shot: `arcShotCooldownSeconds=20`, `arcShotRangeBlocks=40`, `arcShotRadiusBlocks=2.0`, `arcShotMaxTargets=3`, `arcShotDamage=5.0`
- Speed Storm: `speedStormCooldownSeconds=60`, `speedStormDurationSeconds=8`, `speedStormRadiusBlocks=10`, `speedStormAllySpeedAmplifier=1`, `speedStormAllyHasteAmplifier=1`, `speedStormEnemySlownessAmplifier=6`, `speedStormEnemyMiningFatigueAmplifier=2`
- Terminal Velocity: `terminalVelocityCooldownSeconds=30`, `terminalVelocityDurationSeconds=10`, `terminalVelocitySpeedAmplifier=2`, `terminalVelocityHasteAmplifier=1`
- Slipstream: `slipstreamCooldownSeconds=45`, `slipstreamDurationSeconds=8`, `slipstreamLengthBlocks=18`, `slipstreamRadiusBlocks=3`, `slipstreamAllySpeedAmplifier=1`, `slipstreamEnemySlownessAmplifier=1`, `slipstreamEnemyKnockback=0.35`
- Afterimage: `afterimageCooldownSeconds=30`, `afterimageDurationSeconds=6`, `afterimageSpeedAmplifier=1`

### Strength Gem
- Nullify: `nullifyCooldownSeconds=20`, `nullifyRadiusBlocks=10`
- Frailer: `frailerCooldownSeconds=20`, `frailerRangeBlocks=20`, `frailerDurationSeconds=8`
- Bounty Hunting: `bountyCooldownSeconds=60`, `bountyDurationSeconds=60`
- Chad Strength: `chadCooldownSeconds=90`, `chadDurationSeconds=45`, `chadEveryHits=4`, `chadBonusDamage=7.0`

### Wealth Gem
- Fumble: `fumbleCooldownSeconds=30`, `fumbleDurationSeconds=8`, `fumbleRadiusBlocks=10`
- Hotbar Lock: `hotbarLockCooldownSeconds=30`, `hotbarLockDurationSeconds=6`, `hotbarLockRangeBlocks=20`
- Amplification: `amplificationCooldownSeconds=180`, `amplificationDurationSeconds=45`
- Rich Rush: `richRushCooldownSeconds=540`, `richRushDurationSeconds=180`

### Terror Gem
- Dread Aura: `dreadAuraRadiusBlocks=10`, `dreadAuraAmplifier=0`
- Blood Price: `bloodPriceDurationSeconds=6`, `bloodPriceStrengthAmplifier=1`, `bloodPriceResistanceAmplifier=0`
- Terror Trade: `terrorTradeCooldownSeconds=180`, `terrorTradeRangeBlocks=30`
- Panic Ring: `panicRingCooldownSeconds=60`, `panicRingTntCount=5`, `panicRingFuseTicks=50`, `panicRingRadiusBlocks=1.6`

### Summoner Gem
- Commander's Mark: `commandersMarkDurationSeconds=3`, `commandersMarkStrengthAmplifier=0`, `commandRangeBlocks=32`
- Familiar's Blessing: `summonBonusHealth=4.0`
- Summon slots: `maxPoints=50`, `maxActiveSummons=20`, `summonLifetimeSeconds=120`, `summonSlotCooldownSeconds=30`
- Recall: `recallCooldownSeconds=20`

### Space Gem
- Lunar Scaling: `lunarMinMultiplier=0.85`, `lunarMaxMultiplier=1.20`
- Starshield: `starshieldProjectileDamageMultiplier=0.80`
- Orbital Laser: `orbitalLaserCooldownSeconds=60`, `orbitalLaserRangeBlocks=64`, `orbitalLaserDelaySeconds=1`, `orbitalLaserRadiusBlocks=4`, `orbitalLaserDamage=10.0`
- Gravity Field: `gravityFieldCooldownSeconds=45`, `gravityFieldDurationSeconds=10`, `gravityFieldRadiusBlocks=10`, `gravityFieldAllyGravityMultiplier=0.75`, `gravityFieldEnemyGravityMultiplier=1.25`
- Black Hole: `blackHoleCooldownSeconds=60`, `blackHoleDurationSeconds=6`, `blackHoleRadiusBlocks=8`, `blackHolePullStrength=0.10`, `blackHoleDamagePerSecond=2.0`
- White Hole: `whiteHoleCooldownSeconds=60`, `whiteHoleDurationSeconds=6`, `whiteHoleRadiusBlocks=8`, `whiteHolePushStrength=0.12`, `whiteHoleDamagePerSecond=1.0`
- Orbital Laser (mining): `orbitalLaserMiningRadiusBlocks=2`, `orbitalLaserMiningHardnessCap=60.0`, `orbitalLaserMiningMaxBlocks=64`

### Reaper Gem
- Undead Ward: `undeadWardDamageMultiplier=0.80`
- Harvest: `harvestRegenDurationSeconds=4`, `harvestRegenAmplifier=0`
- Grave Steed: `graveSteedCooldownSeconds=60`, `graveSteedDurationSeconds=30`, `graveSteedDecayDamagePerSecond=1.0`
- Withering Strikes: `witheringStrikesCooldownSeconds=45`, `witheringStrikesDurationSeconds=10`, `witheringStrikesWitherDurationSeconds=4`, `witheringStrikesWitherAmplifier=0`
- Death Oath: `deathOathCooldownSeconds=60`, `deathOathDurationSeconds=12`, `deathOathRangeBlocks=48`, `deathOathSelfDamagePerSecond=1.0`
- Scythe Sweep: `scytheSweepCooldownSeconds=20`, `scytheSweepRangeBlocks=5`, `scytheSweepArcDegrees=110`, `scytheSweepDamage=7.0`, `scytheSweepKnockback=0.55`
- Blood Charge: `bloodChargeCooldownSeconds=60`, `bloodChargeMaxChargeSeconds=8`, `bloodChargeSelfDamagePerSecond=1.0`, `bloodChargeMaxMultiplier=1.60`, `bloodChargeBuffDurationSeconds=8`
- Shade Clone: `shadeCloneCooldownSeconds=90`, `shadeCloneDurationSeconds=12`, `shadeCloneMaxHealth=20.0`

### Pillager Gem
- Raider's Training: `raidersTrainingProjectileVelocityMultiplier=1.15`
- Shieldbreaker: `shieldbreakerDisableCooldownTicks=80`
- Illager Discipline: `illagerDisciplineThresholdHearts=4.0`, `illagerDisciplineResistanceDurationSeconds=4`, `illagerDisciplineResistanceAmplifier=0`, `illagerDisciplineCooldownSeconds=45`
- Fangs: `fangsCooldownSeconds=25`, `fangsRangeBlocks=24`, `fangsCount=8`, `fangsSpacingBlocks=1.25`, `fangsWarmupStepTicks=2`
- Ravage: `ravageCooldownSeconds=20`, `ravageRangeBlocks=6`, `ravageDamage=6.0`, `ravageKnockback=1.25`
- Vindicator Break: `vindicatorBreakCooldownSeconds=35`, `vindicatorBreakDurationSeconds=8`, `vindicatorBreakStrengthAmplifier=0`, `vindicatorBreakShieldDisableCooldownTicks=100`
- Volley: `volleyCooldownSeconds=45`, `volleyDurationSeconds=3`, `volleyPeriodTicks=10`, `volleyArrowsPerShot=1`, `volleyArrowDamage=4.0`, `volleyArrowVelocity=3.0`, `volleyArrowInaccuracy=1.0`

### Spy / Mimic Gem
- Stillness Cloak: `stillnessSeconds=5`, `stillnessMoveEpsilonBlocks=0.05`, `stillnessInvisRefreshSeconds=2`
- Mimic Form: `mimicFormCooldownSeconds=60`, `mimicFormDurationSeconds=12`, `mimicFormBonusMaxHealth=4.0`, `mimicFormSpeedMultiplier=1.10`
- Echo: `echoCooldownSeconds=25`, `echoWindowSeconds=8`
- Steal: `stealCooldownSeconds=60`, `stealRequiredWitnessCount=4`, `maxStolenAbilities=3`
- Smoke Bomb: `smokeBombCooldownSeconds=30`, `smokeBombRadiusBlocks=8`, `smokeBombDurationSeconds=6`, `smokeBombBlindnessAmplifier=0`, `smokeBombSlownessAmplifier=0`
- Stolen Cast: `stolenCastCooldownSeconds=20`
- Echo observation: `observeRangeBlocks=24`, `observeWindowSeconds=600`

### Beacon Gem
- Beacon Core: `coreRadiusBlocks=8`, `corePulsePeriodSeconds=2`, `coreRegenDurationSeconds=3`, `coreRegenAmplifier=0`
- Stabilize: `stabilizeRadiusBlocks=8`, `stabilizeReduceTicksPerSecond=20`
- Rally: `rallyRadiusBlocks=10`, `rallyAbsorptionHearts=4`, `rallyDurationSeconds=8`
- Aura pulses: `auraCooldownSeconds=30`, `auraDurationSeconds=12`, `auraRadiusBlocks=10`, `auraRefreshSeconds=2`
- Aura amplifiers: `auraSpeedAmplifier=1`, `auraHasteAmplifier=1`, `auraResistanceAmplifier=0`, `auraJumpAmplifier=1`, `auraStrengthAmplifier=0`, `auraRegenAmplifier=0`

### Air Gem
- Aerial Guard: `aerialGuardFallDamageMultiplier=0.50`, `aerialGuardKnockbackMultiplier=0.60`
- Skyborn: `skybornDurationSeconds=3`, `skybornCooldownSeconds=20`
- Wind Jump: `windJumpCooldownSeconds=8`, `windJumpVerticalVelocity=1.0`, `windJumpForwardVelocity=0.2`
- Gale Slam: `galeSlamCooldownSeconds=30`, `galeSlamWindowSeconds=8`, `galeSlamRadiusBlocks=4`, `galeSlamBonusDamage=6.0`, `galeSlamKnockback=1.0`
- Updraft Zone: `updraftZoneCooldownSeconds=25`, `updraftZoneRadiusBlocks=8`, `updraftZoneUpVelocity=0.9`, `updraftZoneEnemyDamage=3.0`, `updraftZoneEnemyKnockback=0.6`
- Air Dash: `dashCooldownSeconds=6`, `dashVelocity=1.6`, `dashUpVelocity=0.1`, `dashIFrameDurationSeconds=1`, `dashIFrameResistanceAmplifier=4`

### Legendary Items
- Legendary crafting: `craftSeconds=600`
- Tracker Compass: `trackerRefreshSeconds=2`, `trackerMaxDistanceBlocks=0`
- Recall Relic: `recallCooldownSeconds=60`
- Hypno Staff: `hypnoHoldSeconds=3`, `hypnoRangeBlocks=24`, `hypnoMaxControlled=10`, `hypnoDurationSeconds=0`
- Universal mob blacklist (Hypno/Summoner/Astra): `mobBlacklist=[minecraft:ender_dragon,minecraft:wither]`
- Earthsplitter Pick: `earthsplitterRadiusBlocks=1`
- Blood Oath Blade: `bloodOathSharpnessCap=10`
- Demolition Blade: `demolitionCooldownSeconds=5`, `demolitionFuseTicks=80`
- Hunter's Sight Bow: `hunterAimRangeBlocks=50`, `hunterAimTimeoutSeconds=15`, `hunterAimAssistStrength=1.0`
- Third-Strike Blade: `thirdStrikeWindowSeconds=5`, `thirdStrikeBonusDamage=4.0`
- Vampiric Edge: `vampiricHealAmount=2.0`
- Supreme set: `supremeHelmetNightVisionAmplifier=0`, `supremeHelmetWaterBreathingAmplifier=0`, `supremeChestStrengthAmplifier=0`, `supremeLeggingsFireResAmplifier=0`, `supremeBootsSpeedAmplifier=0`, `supremeSetResistanceAmplifier=2`
