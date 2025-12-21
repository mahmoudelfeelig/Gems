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

---

# Planned / Design Notes (not implemented yet)

This section is a design spec for the next iteration. Nothing below this header is guaranteed to exist in the current build.

## Server Gameplay Loop: “Assassin” Endgame

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

## Heart Items: teammate restriction (implemented)

- Heart items can be consumed by anyone *except* players trusted by the heart’s owner (prevents teammate boosting).

## Speed Gem: scaling + expansion (planned)

- Add **Haste** as a passive.
- Add “acceleration” scaling: the faster the Speed user is moving, the stronger Speed abilities are *at cast time*.
- Add additional passives/abilities to make Speed more complete.

**Proposed Speed passives (level 1)**
- Speed I: permanent Speed I.
- Haste I: permanent Haste I.
- Momentum: abilities snapshot your movement speed at cast time and scale their effects (damage/radius/duration/targets) based on that “momentum”.
- Frictionless Steps: reduces movement slowdown from cobweb/honey/slow powder while the gem is active.

**Proposed Speed abilities**
- Arc Shot (existing): lightning arc that can strike multiple targets in a line.
- Speed Storm (existing): AOE field that slows enemies and buffs allies.
- Terminal Velocity (existing): burst of Speed III + Haste II.
- Slipstream: creates a forward wind lane; trusted allies inside gain Speed, enemies get minor knockback/Slowness.
- Afterimage: short-duration “blur” that grants brief invisibility + speed burst; first hit taken ends it early.

**Proposed acceleration scaling (cast-time snapshot)**
- Define `momentum` from horizontal velocity at cast time, clamped to `[minSpeed, maxSpeed]` and remapped to `[0..1]`.
- Examples:
  - Arc Shot: `damage`, `maxTargets`, and `knockback` scale with momentum.
  - Speed Storm: `radius` and `enemySlownessAmplifier` scale with momentum.
  - Terminal Velocity: `duration` scales with momentum (cap to avoid over-long buffs).

## New Gems (planned)

### Terror Gem
- Abilities:
  - “Terror Trade”: kill yourself to kill a target (totem pops instead of dying if held). Costs **-2 hearts** and **-2 permanent energy** (even if Assassin). Hard cap: **3 total uses per player** (persistent across gem changes and deaths).
  - “Panic Ring”: spawn **5 primed TNT** around you.

**Proposed Terror passives (level 1)**
- Dread Aura: untrusted players within a radius get short Darkness pulses (pulse every second; no per-tick spam).
- Fearless: immune to Darkness/Blindness from gem sources (including your own Dread Aura).
- Blood Price: on killing a player, gain a short Strength burst (refreshes, stacks).

### Summoner Gem

**Proposed Summoner passives (level 1)**
- Summon Mastery: summoned mobs never target you or your trusted list.
- Commander’s Mark: if you hit a player with a sword, they become “marked” for 3s; your summons prioritize them and deal bonus damage to them.
- Pack Tactics: summons gain a small damage bonus when multiple summons from the same owner are nearby.
- Soulbound: your summons despawn cleanly on your death/logoff to prevent server clutter.

- Core system:
  - You have a configurable pool of “points/coins/mana” (ex: 50) and per-mob costs (ex: Zombie 5, Skeleton 5, Creeper 10, Piglin Brute 25).
  - Ability slots 1–5 each spawn the configured summon for that slot.
  - Summons are never hostile to you or your trust list.
  - Summons prioritize a target you hit with a sword within the last 3 seconds and deal increased damage to that target.
  - No summoned mobs drop loot or XP.
  - Ender Dragon / Wither excluded.

**Proposed Summoner abilities**
- Summon Slot 1: spawns the configured summon set for slot 1.
- Summon Slot 2: spawns the configured summon set for slot 2.
- Summon Slot 3: spawns the configured summon set for slot 3.
- Summon Slot 4: spawns the configured summon set for slot 4.
- Summon Slot 5: spawns the configured summon set for slot 5.
- Recall: teleports your active summons to you (short cooldown; fails if too far / in other dimension).

### Space Gem
- Passives:
  - Damage/healing scaling based on moon phase.

**Proposed Space passives (level 1)**
- Lunar Scaling: damage/healing multipliers based on moon phase (new moon weakest → full moon strongest).
- Low Gravity: permanent minor Slow Falling and no fall damage while gem is active.
- Starshield: reduced projectile damage while outdoors at night (server-side check; no tick scanning).

- Abilities:
  - Orbital laser strike (area selection; “shift to mine” mode; can mine hard blocks like obsidian).
  - Gravity control field (per-player control within range: lighter vs heavier).
  - Black hole that pulls/damages mobs/players.
  - White hole that pushes away mobs/players.

**Proposed Space abilities**
- Orbital Laser:
  - Cast selects a target block position within range.
  - Mode toggles between **Damage** and **Mining** (Mining breaks blocks with a whitelist + hardness cap).
  - Telegraph: visible beam + warning particles before impact.
- Gravity Field:
  - Creates a radius around the caster.
  - Second cast cycles targeting mode: **Allies Up / Enemies Down / Manual**.
  - Manual mode lets you “tag” one player at a time for Up or Down within the field.
- Black Hole:
  - Spawns at your position (or targeted position) and pulls entities toward the center, dealing periodic damage.
  - Pull strength and duration are configurable and clamped.
- White Hole:
  - Spawns at your position (or targeted position) and pushes entities away from the center, dealing periodic damage.
  - Push strength and duration are configurable and clamped.

### Reaper Gem
- Passives:
  - Reduced damage from undead 
  - immunity to rotten flesh/spider eye debuffs.

**Proposed Reaper passives (level 1)**
- Rot Eater: eating rotten flesh/spider eyes does not apply negative effects.
- Undead Ward: reduced damage from undead mobs.
- Harvest: killing mobs grants brief Regeneration (small, capped).

- Abilities:
  - Summon a skeleton horse mount (saddled) that loses health over time.
  - Attacks apply Wither temporarily.
  - Track a player’s distance in blocks; you sacrifice health until you hit them.
  - Scythe arc attack (5-block reach).
  - Hold-to-charge health sacrifice that boosts next attack/abilities damage.
  - Summon a clone illusion that looks real and attacks but deals no damage.

**Proposed Reaper abilities (order)**
- Grave Steed: summon saddled skeleton horse (decays over time).
- Withering Strikes: timed buff where your hits apply Wither.
- Death Oath: pick a target; while active you slowly lose health until you hit them (cancel on hit or time-out).
- Scythe Sweep: cone/arc cleave attack (5 block reach) with knockback.
- Blood Charge: hold to sacrifice health; next hit/ability is amplified.
- Shade Clone: spawn a convincing clone that pathfinds/attacks but deals 0 damage.

### Pillager Gem

**Proposed Pillager passives (level 1)**
- Raider’s Training: improved bow/crossbow handling (reduced draw time and increased projectile velocity).
- Shieldbreaker: your attacks apply extra shield disable time (no axe required).
- Illager Discipline: brief Resistance burst when you drop below a health threshold (cooldown).

- Abilities:
  - Evoker fangs (“teeth”) attack.
  - Ravager-style knockback/charge hit.
  - “Vindicator Break”: increase damage and disable shields like an axe hit (without using axe cooldown).
  - Auto-arrow burst: shoots arrows every 0.5s for 3 seconds.

**Proposed Pillager abilities (order)**
- Fangs: evoker-fangs line attack.
- Ravage: short charge that knocks back and damages.
- Vindicator Break: temporarily grants bonus melee damage and disables shields on hit.
- Volley: auto-shoot a burst for 3 seconds.

### Spy / Mimic Gem
- Passives:
  - If you stand still for 5 seconds, become invisible (no particles).

**Proposed Spy/Mimic passives (level 1)**
- Stillness Cloak: stand still for 5s → invisibility with no particles; moving cancels.
- Silent Step: muffles footsteps and prevents sculk sensor/shrieker triggers.
- False Signature: hides your gem particles/ability indicators from enemies (your own HUD still shows everything).
- Quick Hands: reduced item use slowdown while invisible.

- Abilities:
  - Transform into the last mob you killed (model + health/hitbox/speed).
  - Copy an ability used in front of you.
  - “Observe Theft”: if you witness an ability used 4 times within 10 minutes and stay alive, you can permanently steal it (until you switch gems; max 3 stolen abilities; you choose which one).

**Proposed Spy/Mimic abilities (order)**
- Mimic Form: transform into last mob you killed (model + movement + hitbox; clamp extremes).
- Mirror Cast: if you saw an ability used recently in front of you, cast it once (copy is weaker and has its own cooldown).
- Observe Theft: persistent steal system (max 3 stolen at once; choose which; resets on gem switch).
- Decoy: spawn a fake player decoy that looks real and draws attention (no damage).
- Wiretap: highlights nearby players’ last ability used for a short window (no exact cooldown numbers).

### Beacon Gem (support/utility)

**Proposed Beacon passives (level 1)**
- Beacon Core: trusted allies in a small radius get minor Regeneration pulses.
- Stabilize: reduces negative status effect durations on trusted allies near you.
- Rally: when you cast a beacon ability, trusted allies get a short absorption shield.

**Proposed Beacon abilities (1–6 are beacon effects)**
- Aura: Speed
- Aura: Haste
- Aura: Resistance
- Aura: Jump Boost
- Aura: Strength
- Aura: Regeneration

Each aura sets your “active beacon effect” for a duration (moving beacon), affecting you + trusted allies within radius.

### Air Gem (mace-focused)
- Passives:
  - Grants a maxed-out mace (Breach IV, Wind Burst III, Mending, Unbreaking III, Fire Aspect II).

**Proposed Air passives (level 1)**
- Windburst Mace: grants the maxed-out mace.
- Aerial Guard: reduced fall damage and knockback while holding the mace.
- Skyborn: brief Slow Falling after taking damage while airborne (cooldown).

- Abilities:
  - Wind-charge style high jump.

**Proposed Air abilities (order)**
- Wind Jump: wind-charge style high jump (no block grief).
- Gale Slam: empowers your next mace slam to create a stronger wind explosion (knockback + damage).
- Updraft Zone: short-lived updraft pillar that lifts trusted allies and disrupts enemies.
- Air Dash: mid-air dash with a brief i-frame window (cooldown).
