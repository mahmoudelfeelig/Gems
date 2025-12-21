# Abilities & Passives

This doc is player-facing and lists what each gem does, plus the default balancing values from `config/gems/balance.json`.

## Trust & ownership

- Trust is one-way: being on another player’s trust list does not imply they are on yours. Trusted players can still damage or debuff you (PVP always allowed); trust only controls ally-targeted effects.

## Gem ownership

- Each gem item is tagged to its owner. If that owner dies, all of their gem items (even if dropped or stashed) are reclaimed and the owner respawns with the same gem at **-1 energy** (min 0).
- If someone else activates your gem item, you die, lose 2 max hearts (no heart item drops), and your gem resets to level 1 (or stays at 0/1); the activator keeps the gem but gains no hearts or energy.
- You can hold multiple gem items (even ones owned by others) but can only activate one gem at a time.
- Gem trading items only trade your currently active gem; other held gems stay untouched.

## Keybinds (default)

- **Ability cast**: hold `Gems Modifier` (default: Left Alt) + press a hotbar number key (`1..9`).
- **Astra Soul Release**: the slot **after** Astra’s last ability (ex: 7 abilities → Soul Release is `Alt + Ability Slot 8`).
- All of these can be changed in `Options → Controls → Gems` (and hotbar number keys are suppressed while the modifier is held, so hotbar usage stays normal).

## Profiling helpers (admin)

For automated profiling (spark/JFR), ops can drive abilities without key presses:
- `/gems admin cast <player> <slot>`
- `/gems admin stress start <players> <seconds> <periodTicks> <realistic|force> <cycleGems> <forceEnergy10>`
- MSPT snapshot (server tick timing): `/gems admin perf snapshot [windowTicks]` and reset: `/gems admin perf reset`

If you want a single command-block command, use the built-in functions:
- `/function gems:perf_force_60s`
- `/function gems:perf_realistic_60s`
- `/function gems:perf_stop`

With Carpet installed, you can also spawn 20 fake players and run the same tests:
- `/function gems:perf_force_60s_bots_20`
- `/function gems:perf_realistic_60s_bots_20`

## HUD

- Shows gem, energy tier, ability list (locked/cooldown), Flux charge %, Astra soul captured state, and highlights your last pressed ability slot.

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
  - Shows a charge bar in the action bar while charging.
  - Defaults: `fireballChargeUpSeconds=3`, `fireballChargeDownSeconds=3`, `fireballInternalCooldownSeconds=4`, `fireballMaxDistanceBlocks=60`
- Meteor Shower: spawns multiple falling “meteors” around a target zone.
  - Defaults: `meteorShowerCooldownSeconds=120`, `meteorShowerCount=10`, `meteorShowerSpreadBlocks=10`, `meteorShowerHeightBlocks=25`, `meteorShowerVelocity=1.5`, `meteorShowerExplosionPower=2`

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
- Defaults: `chargeDiamondBlock=25`, `chargeGoldBlock=15`, `chargeCopperBlock=5`, `chargeEmeraldBlock=20`, `chargeAmethystBlock=10`, `chargeNetheriteScrap=35`, `chargeEnchantedDiamondItem=20`
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
- Haste I: permanent Haste I effect.
- Momentum: abilities scale with your movement speed at cast time.
  - Defaults: `momentumMinSpeed=0.10`, `momentumMaxSpeed=0.60`, `momentumMinMultiplier=0.90`, `momentumMaxMultiplier=1.30`
- Frictionless Steps: reduced slowdown from cobweb, honey, and powder snow.
  - Defaults: `frictionlessSpeedAmplifier=1`

**Abilities**
- Arc Shot: lightning arc that can strike multiple targets in a line.
  - Defaults: `arcShotCooldownSeconds=20`, `arcShotRangeBlocks=40`, `arcShotRadiusBlocks=2.0`, `arcShotMaxTargets=3`, `arcShotDamage=5.0`
- Speed Storm: AOE field that slows enemies and buffs allies.
  - Defaults: `speedStormCooldownSeconds=60`, `speedStormDurationSeconds=8`, `speedStormRadiusBlocks=10`, `speedStormAllySpeedAmplifier=1`, `speedStormAllyHasteAmplifier=1`, `speedStormEnemySlownessAmplifier=6`, `speedStormEnemyMiningFatigueAmplifier=2`
- Terminal Velocity: burst of Speed III + Haste II.
  - Defaults: `terminalVelocityCooldownSeconds=30`, `terminalVelocityDurationSeconds=10`, `terminalVelocitySpeedAmplifier=2`, `terminalVelocityHasteAmplifier=1`
- Slipstream: a forward wind lane that speeds allies and disrupts enemies.
  - Defaults: `slipstreamCooldownSeconds=45`, `slipstreamDurationSeconds=8`, `slipstreamLengthBlocks=18`, `slipstreamRadiusBlocks=3`, `slipstreamAllySpeedAmplifier=1`, `slipstreamEnemySlownessAmplifier=1`, `slipstreamEnemyKnockback=0.35`
- Afterimage: brief invisibility + speed burst; breaks on first hit.
  - Defaults: `afterimageCooldownSeconds=30`, `afterimageDurationSeconds=6`, `afterimageSpeedAmplifier=1`

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

## Terror Gem

**Passives**
- Dread Aura: nearby untrusted players get Darkness.
  - Defaults: `dreadAuraRadiusBlocks=10`, `dreadAuraAmplifier=0`
- Fearless: cleanses Blindness and Darkness from the holder.
- Blood Price: on killing a player, gain a short Strength/Resistance burst.
  - Defaults: `bloodPriceDurationSeconds=6`, `bloodPriceStrengthAmplifier=1`, `bloodPriceResistanceAmplifier=0`

**Abilities**
- Terror Trade: target a player and sacrifice yourself to attempt to kill them.
  - Totems will still save the target (totem pops).
  - Costs: `terrorTradeHeartsCost=2` hearts, and `terrorTradePermanentEnergyPenalty=2` permanent max-energy (cap reduction).
  - Limited uses: `terrorTradeMaxUses=3` uses per player total (persists across deaths and gem swaps).
  - Defaults: `terrorTradeCooldownSeconds=180`, `terrorTradeRangeBlocks=30`
- Panic Ring: spawns primed TNT around you.
  - Defaults: `panicRingCooldownSeconds=60`, `panicRingTntCount=5`, `panicRingFuseTicks=50`, `panicRingRadiusBlocks=1.6`

## Summoner Gem

**Passives**
- Summoner’s Bond: summons will not target you or your trusted players.
- Commander’s Mark: sword hits “mark” the target; summons are commanded toward them and gain temporary Strength.
  - Defaults: `commandersMarkDurationSeconds=3`, `commandersMarkStrengthAmplifier=0`, `commandRangeBlocks=32`
- Soulbound Minions: summons despawn when you die or log out.
- Familiar’s Blessing: summons spawn with bonus max health.
  - Defaults: `summonBonusHealth=4.0`
- Loadout UI: press your Gems modifier + the next hotbar key after the last loadout slot (default: Left Alt + hotbar 6) while using the Summoner gem to open the loadout editor. Each slot can hold multiple rows of mobs, and edits are validated against the point budget.

**Abilities**
- Summon 1–5: spawns the configured minions for that slot.
  - Budget: total loadout cost (all 5 slots combined) must be `<= maxPoints` and active summons are capped.
  - Defaults: `maxPoints=50`, `maxActiveSummons=20`, `summonLifetimeSeconds=120`, `summonSlotCooldownSeconds=30`
  - Default slot loadout:
    - Slot 1: 2× zombie
    - Slot 2: 2× skeleton
    - Slot 3: 1× creeper
  - Default point costs (extendable via config/UI): zombie 5, skeleton 5, stray 6, husk 6, spider 6, cave spider 8, drowned 8, wolf 8, piglin 8, slime 8, magma cube 10, creeper 10, witch 14, blaze 14, pillager 12, vindicator 18, wither skeleton 18, guardian 18, enderman 20, hoglin 16, zoglin 22, piglin brute 25, iron golem 40, ravager 45.
- Recall: despawns all of your active summons.
  - Defaults: `recallCooldownSeconds=20`

## Space Gem

**Passives**
- Lunar Scaling: your outgoing damage and self-healing scale with the moon phase (full moon strongest).
  - Defaults: `lunarMinMultiplier=0.85`, `lunarMaxMultiplier=1.20`
- Low Gravity: minor Slow Falling and no fall damage while the gem is active.
- Starshield: reduces projectile damage while outdoors at night.
  - Defaults: `starshieldProjectileDamageMultiplier=0.80`

**Abilities**
- Orbital Laser: strikes the block you're looking at; `Sneak + cast` turns it into mining mode.
  - Defaults: `orbitalLaserCooldownSeconds=60`, `orbitalLaserRangeBlocks=64`, `orbitalLaserDelaySeconds=1`, `orbitalLaserRadiusBlocks=4`, `orbitalLaserDamage=10.0`
  - Mining defaults: `orbitalLaserMiningRadiusBlocks=2`, `orbitalLaserMiningHardnessCap=60.0`, `orbitalLaserMiningMaxBlocks=64`
- Gravity Field: AOE around you that makes trusted players lighter and untrusted players heavier (movement feel changes via gravity).
  - Defaults: `gravityFieldCooldownSeconds=45`, `gravityFieldDurationSeconds=10`, `gravityFieldRadiusBlocks=10`, `gravityFieldAllyGravityMultiplier=0.75`, `gravityFieldEnemyGravityMultiplier=1.25`
- Black Hole: pulls entities inward and deals periodic damage.
  - Defaults: `blackHoleCooldownSeconds=60`, `blackHoleDurationSeconds=6`, `blackHoleRadiusBlocks=8`, `blackHolePullStrength=0.10`, `blackHoleDamagePerSecond=2.0`
- White Hole: pushes entities outward and deals periodic damage.
  - Defaults: `whiteHoleCooldownSeconds=60`, `whiteHoleDurationSeconds=6`, `whiteHoleRadiusBlocks=8`, `whiteHolePushStrength=0.12`, `whiteHoleDamagePerSecond=1.0`

## Reaper Gem

**Passives**
- Rot Eater: eating rotten flesh/spider eyes does not apply negative effects.
- Undead Ward: reduced damage taken from undead mobs.
  - Defaults: `undeadWardDamageMultiplier=0.80`
- Harvest: killing mobs grants brief Regeneration.
  - Defaults: `harvestRegenDurationSeconds=4`, `harvestRegenAmplifier=0`

**Abilities**
- Grave Steed: summons a saddled skeleton horse mount that decays over time.
  - Defaults: `graveSteedCooldownSeconds=60`, `graveSteedDurationSeconds=30`, `graveSteedDecayDamagePerSecond=1.0`
- Withering Strikes: temporarily makes your melee hits apply Wither.
  - Defaults: `witheringStrikesCooldownSeconds=45`, `witheringStrikesDurationSeconds=10`, `witheringStrikesWitherDurationSeconds=4`, `witheringStrikesWitherAmplifier=0`
- Death Oath: choose a target; you take damage over time until you hit them (or it expires).
  - Defaults: `deathOathCooldownSeconds=60`, `deathOathDurationSeconds=12`, `deathOathRangeBlocks=48`, `deathOathSelfDamagePerSecond=1.0`
- Scythe Sweep: wide melee cleave in front of you (5 block reach).
  - Defaults: `scytheSweepCooldownSeconds=20`, `scytheSweepRangeBlocks=5`, `scytheSweepArcDegrees=110`, `scytheSweepDamage=7.0`, `scytheSweepKnockback=0.55`
- Blood Charge: hold to sacrifice health; your next hit/ability is amplified for a short window.
  - Defaults: `bloodChargeCooldownSeconds=60`, `bloodChargeMaxChargeSeconds=8`, `bloodChargeSelfDamagePerSecond=1.0`, `bloodChargeMaxMultiplier=1.60`, `bloodChargeBuffDurationSeconds=8`
- Shade Clone: spawns a short-lived clone that looks real (no damage).
  - Defaults: `shadeCloneCooldownSeconds=90`, `shadeCloneDurationSeconds=12`, `shadeCloneMaxHealth=20.0`

## Pillager Gem

**Passives**
- Raider’s Training: your fired projectiles travel faster.
  - Defaults: `raidersTrainingProjectileVelocityMultiplier=1.15`
- Shieldbreaker: your melee hits can disable shields even without an axe (untrusted players only).
  - Defaults: `shieldbreakerDisableCooldownTicks=80`
- Illager Discipline: when you drop below a health threshold, gain a burst of Resistance (cooldown).
  - Defaults: `illagerDisciplineThresholdHearts=4.0`, `illagerDisciplineResistanceDurationSeconds=4`, `illagerDisciplineResistanceAmplifier=0`, `illagerDisciplineCooldownSeconds=45`

**Abilities**
- Fangs: conjures an evoker-fangs line at the target zone (entity or block).
  - Defaults: `fangsCooldownSeconds=25`, `fangsRangeBlocks=24`, `fangsCount=8`, `fangsSpacingBlocks=1.25`, `fangsWarmupStepTicks=2`
- Ravage: bashes a target with heavy knockback (untrusted players only).
  - Defaults: `ravageCooldownSeconds=20`, `ravageRangeBlocks=6`, `ravageDamage=6.0`, `ravageKnockback=1.25`
- Vindicator Break: temporary buff that empowers melee hits and disables shields on contact (untrusted players only).
  - Defaults: `vindicatorBreakCooldownSeconds=35`, `vindicatorBreakDurationSeconds=8`, `vindicatorBreakStrengthAmplifier=0`, `vindicatorBreakShieldDisableCooldownTicks=100`
- Volley: automatically fires a short burst of arrows (does not require ammo).
  - Defaults: `volleyCooldownSeconds=45`, `volleyDurationSeconds=3`, `volleyPeriodTicks=10`, `volleyArrowsPerShot=1`, `volleyArrowDamage=4.0`, `volleyArrowVelocity=3.0`, `volleyArrowInaccuracy=1.0`

## Spy / Mimic Gem

**Passives**
- Stillness Cloak: stand still for a few seconds to become invisible with no particles (refreshes while still).
  - Defaults: `stillnessSeconds=5`, `stillnessMoveEpsilonBlocks=0.05`, `stillnessInvisRefreshSeconds=2`
- Silent Step: prevents sculk sensor/shrieker activations caused by you.
- False Signature: makes you harder to track with “mark/track” style gem abilities.
- Quick Hands: permanent minor Haste while active.

**Abilities**
- Mimic Form: requires a recent mob kill; temporarily grants invisibility + bonus max health + speed.
  - Defaults: `mimicFormCooldownSeconds=60`, `mimicFormDurationSeconds=12`, `mimicFormBonusMaxHealth=4.0`, `mimicFormSpeedMultiplier=1.10`
- Echo: replays the last observed ability used in front of you within a time window.
  - Observation defaults: `observeRangeBlocks=24`, `observeWindowSeconds=600`
  - Defaults: `echoCooldownSeconds=25`, `echoWindowSeconds=8`
- Steal: after witnessing the same ability enough times (without dying), steal it into a limited list; the victim temporarily loses that ability until they swap gems.
  - Defaults: `stealCooldownSeconds=60`, `stealRequiredWitnessCount=4`, `maxStolenAbilities=3`
- Smoke Bomb: blinds/slows nearby untrusted players and briefly cloaks you.
  - Defaults: `smokeBombCooldownSeconds=30`, `smokeBombRadiusBlocks=8`, `smokeBombDurationSeconds=6`, `smokeBombBlindnessAmplifier=0`, `smokeBombSlownessAmplifier=0`
- Stolen Cast: casts your selected stolen ability; `Sneak + cast` cycles selection.
  - Defaults: `stolenCastCooldownSeconds=20`

## Beacon Gem

**Passives**
- Beacon Core: trusted allies near you receive periodic Regeneration pulses; nearby untrusted players are slowed.
  - Defaults: `coreRadiusBlocks=8`, `corePulsePeriodSeconds=2`, `coreRegenDurationSeconds=3`, `coreRegenAmplifier=0`
- Stabilize: reduces harmful effect durations on trusted allies nearby and hinders untrusted players with brief fatigue.
  - Defaults: `stabilizeRadiusBlocks=8`, `stabilizeReduceTicksPerSecond=20`
- Rally: casting a beacon aura grants trusted allies brief Absorption and weakens untrusted players.
  - Defaults: `rallyRadiusBlocks=10`, `rallyAbsorptionHearts=4`, `rallyDurationSeconds=8`

**Abilities (moving beacon auras)**
- Aura: Speed
- Aura: Haste
- Aura: Resistance
- Aura: Jump Boost
- Aura: Strength
- Aura: Regeneration
- Each pulse: buffs trusted players with the chosen aura and applies Slowness/Weakness to untrusted players in range.
- Defaults: `auraCooldownSeconds=30`, `auraDurationSeconds=12`, `auraRadiusBlocks=10`, `auraRefreshSeconds=2`
- Aura amplifiers: `auraSpeedAmplifier=1`, `auraHasteAmplifier=1`, `auraResistanceAmplifier=0`, `auraJumpAmplifier=1`, `auraStrengthAmplifier=0`, `auraRegenAmplifier=0`

## Air Gem

**Passives**
- Windburst Mace: grants a maxed-out mace (Breach IV, Wind Burst III, Mending, Unbreaking III, Fire Aspect II).
- Aerial Guard: reduced fall damage and knockback while holding the mace.
  - Defaults: `aerialGuardFallDamageMultiplier=0.50`, `aerialGuardKnockbackMultiplier=0.60`
- Skyborn: taking damage midair grants brief Slow Falling (cooldown).
  - Defaults: `skybornDurationSeconds=3`, `skybornCooldownSeconds=20`

**Abilities**
- Wind Jump: wind-charge style high jump.
  - Defaults: `windJumpCooldownSeconds=8`, `windJumpVerticalVelocity=1.0`, `windJumpForwardVelocity=0.2`
- Gale Slam: empowers your next mace hit to create a stronger wind slam.
  - Defaults: `galeSlamCooldownSeconds=30`, `galeSlamWindowSeconds=8`, `galeSlamRadiusBlocks=4`, `galeSlamBonusDamage=6.0`, `galeSlamKnockback=1.0`
- Updraft Zone: updraft that lifts allies and disrupts enemies.
  - Defaults: `updraftZoneCooldownSeconds=25`, `updraftZoneRadiusBlocks=8`, `updraftZoneUpVelocity=0.9`, `updraftZoneEnemyDamage=3.0`, `updraftZoneEnemyKnockback=0.6`
- Air Dash: forward dash with brief i-frames.
  - Defaults: `dashCooldownSeconds=6`, `dashVelocity=1.6`, `dashUpVelocity=0.1`, `dashIFrameDurationSeconds=1`, `dashIFrameResistanceAmplifier=4`

---

# Assassin Endgame

- Everyone spawns normally with a random gem.
- If a player dies while already at **5 max hearts**, they become an **Assassin**.
- Assassins are highlighted **red** in the player list/tab UI.
- Assassins:
  - Have a **static max of 10 hearts**.
  - **Never drop heart items** and **cannot consume heart items**.
  - If killed by another Assassin, they lose **2 max hearts**.
  - Can only regain those lost hearts by **killing other Assassins**.
  - If an Assassin reaches **0 max hearts**, they are eliminated permanently.
  - Cannot exceed **10 hearts**, even when killing another Assassin at 10 hearts.
- Scoring (counts only after becoming an Assassin):
  - **Normal kills**: +1 point
  - **Final kills**: +3 points
    - “Final kill” = killing a player who was at 5 hearts and thereby turning them into an Assassin.
- The Assassin with the most points is matched against the last non-Assassin survivor (admin-run duel).

## Heart Items: teammate restriction

- Heart items can be consumed by anyone *except* players trusted by the heart’s owner (prevents teammate boosting).