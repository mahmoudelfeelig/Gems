# Abilities & Passives

This doc is player-facing and lists what each gem does, plus the default balancing values from `config/gems/balance.json`.

## Keybinds (default)

- **Ability cast**: hold `Gems Modifier` (default: Left Alt) + press `Ability Slot N` (defaults: `1..9` and `0` for slot 10).
- **Astra Soul Release**: the slot **after** Astra’s last ability (ex: 7 abilities → Soul Release is `Alt + Ability Slot 8`).
- All of these can be changed in `Options → Controls → Gems` (and hotbar number keys are suppressed while the modifier is held, so hotbar usage stays normal).

## Profiling helpers (admin)

For automated profiling (spark/JFR), ops can drive abilities without key presses:
- `/gems admin cast <player> <slot>`
- `/gems admin stress start <players> <seconds> <periodTicks> <realistic|force> <cycleGems> <forceEnergy10>`

## HUD

- Shows gem, energy tier, max hearts, ability list (locked/cooldown), Flux charge %, Astra soul captured state, and highlights your last pressed ability slot.

## Unlock rules (energy/levels)

- Level/energy 1: passives only
- Level/energy 2–4: abilities unlock in order (one per level)
- Level/energy 5: all remaining abilities unlock
- Level/energy 6–10: buffer only (no new powers)

## Astra Gem

**Passives**
- Soul Capture: stores the most recently killed mob so it can be released later.
- Soul Healing: heals the holder when Soul Capture stores or releases a mob.

**Abilities**
- Shadow Anchor: press once to set an anchor; press again within the window to return.
  - Tip: `Sneak + cast` while anchored clears the anchor without returning.
  - Defaults: `shadowAnchorWindowSeconds=10`
- Dimensional Void: suppresses enemy gem abilities within a radius for a duration.
  - Defaults: `dimensionalVoidCooldownSeconds=60`, `dimensionalVoidDurationSeconds=8`, `dimensionalVoidRadiusBlocks=10`
- Astral Daggers: fires multiple ranged daggers.
  - Defaults: `astralDaggersCooldownSeconds=8`, `astralDaggersCount=5`, `astralDaggersDamage=4.0`, `astralDaggersVelocity=3.5`, `astralDaggersSpread=0.05`
- Unbounded: enter Spectator mode briefly, then return to Survival automatically.
  - Defaults: `unboundedCooldownSeconds=60`, `unboundedDurationSeconds=3`
- Astral Camera: enter Spectator mode for scouting, then return to your original position.
  - Defaults: `astralCameraCooldownSeconds=60`, `astralCameraDurationSeconds=8`
- Spook: applies a short fear/disorient effect to nearby enemies.
  - Defaults: `spookCooldownSeconds=30`, `spookRadiusBlocks=10`, `spookDurationSeconds=6`
- Tag: marks a target so they remain tracked/visible through walls for a short time.
  - Defaults: `tagCooldownSeconds=20`, `tagRangeBlocks=30`, `tagDurationSeconds=12`

**Soul Release (Astra-only)**
- Summons the captured mob type (if any) as a **soul summon**.
  - Soul summons drop **no loot and no XP**, and killing them does **not** re-capture their soul.
  - Hostile soul summons will target untrusted players, and will not target the owner or their trusted players.

## Fire Gem

**Passives**
- Fire Resistance: permanent Fire Resistance.
- Auto-smelt: smelts eligible block drops.
- Auto-enchant Fire Aspect: applies Fire Aspect I to eligible melee weapons.

**Abilities**
- Cosy Campfire: creates an aura that grants allies Regeneration IV in range.
  - Defaults: `cosyCampfireCooldownSeconds=45`, `cosyCampfireDurationSeconds=10`, `cosyCampfireRadiusBlocks=8`, `cosyCampfireRegenAmplifier=3`
- Heat Haze Zone: allies gain Fire Resistance; enemies gain Mining Fatigue + Weakness while in the radius.
  - Defaults: `heatHazeCooldownSeconds=90`, `heatHazeDurationSeconds=10`, `heatHazeRadiusBlocks=10`, `heatHazeEnemyMiningFatigueAmplifier=0`, `heatHazeEnemyWeaknessAmplifier=0`
- Fireball: press once to start charging; press again to launch. Charge decays unless standing on obsidian.
  - Defaults: `fireballChargeUpSeconds=3`, `fireballChargeDownSeconds=3`, `fireballInternalCooldownSeconds=4`, `fireballMaxDistanceBlocks=60`
- Meteor Shower: spawns multiple falling “meteors” around a target zone.
  - Defaults: `meteorShowerCooldownSeconds=120`, `meteorShowerCount=10`, `meteorShowerSpreadBlocks=10`, `meteorShowerHeightBlocks=25`, `meteorShowerVelocity=1.5`

## Flux Gem

**Passives**
- Charge Storage: consumes valuables to increase stored Flux charge (0–200%).
- Ally Inversion: if you Flux Beam a trusted ally, it repairs their armor instead of damaging them.
- Overcharge Ramp: at 100% charge, begins overcharging toward 200% after a delay, dealing self-damage each second.

**Abilities**
- Flux Beam: raycast beam; damage and armor durability damage scale with stored charge.
  - Defaults: `fluxBeamCooldownSeconds=4`, `fluxBeamRangeBlocks=60`, `fluxBeamMinDamage=6.0`, `fluxBeamMaxDamageAt100=12.0`, `fluxBeamMaxDamageAt200=24.0`, `fluxBeamArmorDamageAt100=200`, `fluxBeamArmorDamagePerPercent=2`
- Static Burst: releases stored recent damage as an AOE burst.
  - Defaults: `staticBurstCooldownSeconds=30`, `staticBurstRadiusBlocks=8`, `staticBurstMaxDamage=20.0`, `staticBurstStoreWindowSeconds=120`

**Charge item values**
- Defaults: `chargeDiamondBlock=25`, `chargeGoldBlock=15`, `chargeCopperBlock=5`, `chargeEnchantedDiamondItem=20`
- Overcharge defaults: `overchargeDelaySeconds=5`, `overchargePerSecond=5`, `overchargeSelfDamagePerSecond=1.0`

## Life Gem

**Passives**
- Auto-enchant Unbreaking: applies Unbreaking III to eligible gear.
- Double Saturation: doubles saturation gained from food.

**Abilities**
- Vitality Vortex: area pulse that adapts to surroundings (Aquatic/Infernal/Sculk/Verdant/End/Default).
  - Defaults: `vitalityVortexCooldownSeconds=30`, `vitalityVortexRadiusBlocks=8`, `vitalityVortexDurationSeconds=8`, `vitalityVortexScanRadiusBlocks=3`, `vitalityVortexVerdantThreshold=10`, `vitalityVortexAllyHeal=2.0`
- Health Drain: siphons health from a target to heal the user.
  - Defaults: `healthDrainCooldownSeconds=12`, `healthDrainRangeBlocks=20`, `healthDrainAmount=6.0`
- Life Circle: aura that lowers enemy max health while boosting trusted allies and the holder.
  - Defaults: `lifeCircleCooldownSeconds=60`, `lifeCircleDurationSeconds=12`, `lifeCircleRadiusBlocks=8`, `lifeCircleMaxHealthDelta=8.0`
- Heart Lock: temporarily locks an enemy’s max health to their health at cast time.
  - Defaults: `heartLockCooldownSeconds=45`, `heartLockDurationSeconds=6`, `heartLockRangeBlocks=20`

## Puff Gem

**Passives**
- Fall Damage Immunity: negates fall damage.
- Auto-enchant Power: applies Power III to bows.
- Auto-enchant Punch: applies Punch I to bows.
- Sculk Silence: does not trigger sculk shriekers.
- Crop-Trample Immunity: cannot trample farmland.

**Abilities**
- Double Jump: midair jump reset.
  - Defaults: `doubleJumpCooldownSeconds=2`, `doubleJumpVelocityY=0.85`
- Dash: fast dash that damages enemies you dash through.
  - Defaults: `dashCooldownSeconds=6`, `dashVelocity=1.8`, `dashDamage=6.0`, `dashHitRangeBlocks=4.0`
- Breezy Bash: uppercut a target; if they land soon, they take impact damage.
  - Defaults: `breezyBashCooldownSeconds=20`, `breezyBashRangeBlocks=10`, `breezyBashUpVelocityY=1.2`, `breezyBashKnockback=0.6`, `breezyBashInitialDamage=4.0`, `breezyBashImpactDamage=6.0`, `breezyBashImpactWindowSeconds=6`
- Group Breezy Bash: knocks away all untrusted players nearby.
  - Defaults: `groupBashCooldownSeconds=45`, `groupBashRadiusBlocks=10`, `groupBashKnockback=1.2`, `groupBashUpVelocityY=0.8`

## Speed Gem

**Passives**
- Speed I: permanent Speed I effect.

**Abilities**
- Arc Shot: lightning arc that can strike multiple targets in a line.
  - Defaults: `arcShotCooldownSeconds=20`, `arcShotRangeBlocks=40`, `arcShotRadiusBlocks=2.0`, `arcShotMaxTargets=3`, `arcShotDamage=5.0`
- Speed Storm: AOE field that slows enemies and buffs allies.
  - Defaults: `speedStormCooldownSeconds=60`, `speedStormDurationSeconds=8`, `speedStormRadiusBlocks=10`, `speedStormAllySpeedAmplifier=1`, `speedStormAllyHasteAmplifier=1`, `speedStormEnemySlownessAmplifier=6`, `speedStormEnemyMiningFatigueAmplifier=2`
- Terminal Velocity: burst of Speed III + Haste II.
  - Defaults: `terminalVelocityCooldownSeconds=30`, `terminalVelocityDurationSeconds=10`, `terminalVelocitySpeedAmplifier=2`, `terminalVelocityHasteAmplifier=1`

## Strength Gem

**Passives**
- Strength I: permanent Strength I effect.
- Auto-enchant Sharpness: applies Sharpness III (and does not downgrade better Sharpness).

**Abilities**
- Nullify: removes active potion/status effects from enemies in a radius.
  - Defaults: `nullifyCooldownSeconds=20`, `nullifyRadiusBlocks=10`
- Frailer: applies Weakness to a target.
  - Defaults: `frailerCooldownSeconds=20`, `frailerRangeBlocks=20`, `frailerDurationSeconds=8`
- Bounty Hunting: consumes an item and tracks that item’s original owner temporarily.
  - Defaults: `bountyCooldownSeconds=60`, `bountyDurationSeconds=60`
- Chad Strength: for a duration, every Nth hit deals bonus damage.
  - Defaults: `chadCooldownSeconds=90`, `chadDurationSeconds=45`, `chadEveryHits=4`, `chadBonusDamage=7.0`

## Wealth Gem

**Passives**
- Auto-enchant Mending: applies Mending.
- Auto-enchant Fortune: applies Fortune III.
- Auto-enchant Looting: applies Looting III.
- Luck: permanent Luck effect.
- Hero of the Village: permanent Hero of the Village effect.
- Durability chip: extra armor durability damage per strike.
- Armor mend on hit: slowly repairs the holder’s armor on successful hits.
- Double Debris: furnaces output double netherite scrap.

**Abilities**
- Pockets: opens a 9-slot extra inventory.
- Fumble: disables offhand use and eating for enemies in a radius.
  - Defaults: `fumbleCooldownSeconds=30`, `fumbleDurationSeconds=8`, `fumbleRadiusBlocks=10`
- Hotbar Lock: locks an enemy to their current hotbar slot temporarily.
  - Defaults: `hotbarLockCooldownSeconds=30`, `hotbarLockDurationSeconds=6`, `hotbarLockRangeBlocks=20`
- Amplification: temporarily boosts enchant effectiveness on the holder’s gear.
  - Defaults: `amplificationCooldownSeconds=180`, `amplificationDurationSeconds=45`
- Rich Rush: temporarily boosts mob drops and ore yields.
  - Defaults: `richRushCooldownSeconds=540`, `richRushDurationSeconds=180`
