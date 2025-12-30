# Abilities & Gems

## Core loop

- You spawn with a random gem at 3 energy.
- Energy tiers: Broken (0), Common (1), Rare (2), Elite (3), Mythical (4), Legendary (5), Legendary +1..+5 (6-10).
- Passives are active at energy 1+ (unless you toggle them off in client config). At energy 0, passives and abilities are disabled.
- Abilities unlock by energy: 2-4 unlock in order, 5+ unlock all remaining abilities.
- Energy levels 6-10 are buffer only (no new powers).
- Player-vs-player kills grant +1 energy. Death (any cause) loses 1 energy.
- Legendary +5 gives a glint. If a Legendary +5 player kills a non-broken player, the victim drops an upgrade item.
- Upgrade items cannot be used by Legendary +5 players.

## Hearts, items, and swapping

- Heart items drop on death and increase max hearts when consumed.
- Minimum max hearts is configurable (default 5); maximum total hearts is 20.
- Heart items cannot be consumed by trusted players (prevents team boosting).
- Energy upgrade items add a level (up to Legendary +5).
- Gem Trader swaps only your active gem and consumes the item.
- Gem Purchase Token adds any gem to your owned set, activates it, and consumes the token.
- All gems recipes unlock automatically on join. Legendary discount recipes (`*_discount`) are cheaper and only craftable while the matching gem is active (`legendary.recipeGemRequirements` uses recipe ids).
- Players drop their heads on death.

## Legendary items

- Legendary crafts use `legendary.craftSeconds` for the timer, `legendary.craftMaxPerItem` for the total per-item limit, and `legendary.craftMaxActivePerItem` for concurrent crafts (default 1).
- Crafting progress is shown to all players via a boss bar with coordinates; on completion the item drops at the crafting table.
- Tracker Compass: right-click to pick a player (including offline); shows current/last-known coords, respawn coords, and points toward them.
- Recall Relic: mark current coords; reuse to teleport back (consumes mark); item persists; has cooldown; forceload while marked and released after teleport/no mark.
- Hypno Staff: hold a beam on a mob for 3s to convert; works on all mobs except the universal mob blacklist (shared with Summoner/Astra); control is temporary and does not persist; hypnotized mobs follow the summon AI priority.
- Earthsplitter Pick: netherite-tier silk touch pick; right-click toggles 3x3x3 vs 9x3x1 tunnel; respects unbreakable/blacklisted blocks.
- Supreme set: helmet (Night Vision + Water Breathing), chestplate (Strength I), leggings (Fire Resistance), boots (Speed I), full set (Resistance III); stronger effects override temporarily.
- Blood Oath Blade: gains +1 Sharpness per unique player kill, capped at Sharpness X.
- Demolition Blade: right-click to arm a demolition charge on a targeted block or entity; cooldown.
- Hunter's Sight Bow: aim assist toward your last hit target (player or mob) within 50 blocks and line of sight.
- Third-Strike Blade: every 3rd crit within a 5s chain window deals bonus damage.
- Vampiric Edge: each crit heals 1 heart.
- Universal mob blacklist (config): applies to Hypno Staff, Summoner summons, and Astra Soul Capture.

## Trust and ownership

- Trust only affects ally-targeted effects. PVP is always allowed.
- Gem items are bound to their owner. You can hold multiple gems, but only one is active.
- Activating another player's gem kills them, skips their heart drop, reduces their max hearts by 2, and caps their energy at 1.

## Controls and HUD

- Cast an ability: hold Gems Modifier (default Left Alt) + press a hotbar number (1-9).
- Controls can be switched to fully custom keybinds via `config/gems/client.json` (`controlMode`: `CHORD` or `CUSTOM`).
- Client config can disable your own gem passives (`passivesEnabled`).
- Astra Soul Release: the slot after Astra's last ability.
- Summoner loadout UI: Gems Modifier + the hotbar key after Recall (default Alt + 7).
- HUD shows current gem, energy tier, cooldowns, and special states (like Flux charge or Astra soul).

## Gems

### Astra Gem

Passives:
- Soul Capture: store the most recently killed mob for later release.
- Soul Healing: heal on capture or release.

Abilities:
- Shadow Anchor: set an anchor and return to it within a short window.
- Dimensional Void: suppress enemy gem abilities and passives in a radius (players only).
- Astral Daggers: fire a ranged dagger volley.
- Unbounded: brief Spectator mode, then return to Survival.
- Astral Camera: scout in Spectator, then return to your start.
- Spook: disorient nearby enemies.
- Tag: mark a target through walls briefly.
- Soul Release: summon the captured mob as a soul summon (no loot/XP), preserving its name/gear/attributes and restoring it to full health; follows the summon AI priority.

### Fire Gem

Passives:
- Fire Resistance.
- Auto-smelt eligible block drops.
- Auto-enchant Fire Aspect on melee weapons.

Abilities:
- Cosy Campfire: regen aura for allies.
- Heat Haze Zone: allies gain Fire Resistance; enemies get Mining Fatigue + Weakness.
- Fireball: charge and launch an explosive fireball (charge decays unless on obsidian).
- Meteor Shower: multiple meteors strike along a line in front of you.

### Flux Gem

Passives:
- Charge Storage: consume valuables to charge up to 200%.
- Ally Inversion: Flux Beam repairs trusted allies' armor instead of damaging them.
- Overcharge Ramp: above 100% charge, ramps to 200% while dealing self-damage.
- Flux Capacitor: at high charge, gain Absorption.
- Flux Conductivity: taking damage converts some of it into charge.
- Flux Insulation: at high charge, incoming damage is reduced.

Abilities:
- Flux Beam: long-range beam; damage and armor shred scale with charge.
- Static Burst: AOE burst from recent damage taken.
- Flux Surge: spend charge for a speed/resistance burst and a close-range shockwave.
- Flux Discharge: dump charge into a damaging knockback shockwave.

### Life Gem

Passives:
- Auto-enchant Unbreaking on eligible gear.
- Double Saturation from food.

Abilities:
- Vitality Vortex: adaptive pulse based on surroundings (Aquatic, Infernal, Sculk, Verdant, End, Default).
- Health Drain: siphon health from a target to heal yourself.
- Life Swap: swap health with a target; requires at least 3 hearts to cast (configurable).
- Life Circle: boost allies while reducing enemy max health (heals to the new max).
- Heart Lock: lock an enemy's max health to their current health briefly.

### Puff Gem

Passives:
- Fall damage immunity.
- Auto-enchant Power and Punch on bows.
- Sculk silence.
- Crop-trample immunity.
- Windborne: while airborne, gain Slow Falling.

Abilities:
- Double Jump: midair jump reset.
- Dash: fast dash that damages enemies you pass through.
- Breezy Bash: uppercut; impact damage if they land soon.
- Group Breezy Bash: knock away nearby untrusted players.
- Gust: shockwave that launches and slows nearby enemies.

### Speed Gem

Passives:
- Speed I and Haste I.
- Momentum: abilities scale with your speed at cast time.
- Frictionless Steps: reduced slowdown from cobweb, honey, and powder snow.

Abilities:
- Arc Shot: lightning arc that can chain multiple targets.
- Speed Storm: field that buffs allies and slows enemies.
- Terminal Velocity: short Speed/Haste burst.
- Slipstream: wind lane that speeds allies and disrupts enemies.
- Afterimage: brief invisibility + speed; breaks on hit.
- Tempo Shift: speeds ally cooldowns and slows enemy cooldowns nearby.

### Strength Gem

Passives:
- Strength I.
- Auto-enchant Sharpness III.
- Adrenaline: gain brief Resistance when critically low.

Abilities:
- Nullify: remove active effects from enemies in a radius.
- Frailer: apply Weakness to a target.
- Bounty Hunting: consume an item to track its owner temporarily.
- Chad Strength: every Nth hit deals bonus damage.

### Wealth Gem

Passives:
- Auto-enchant Mending, Fortune, and Looting.
- Luck and Hero of the Village V.
- Durability chip and armor mend on hit.
- Double Debris from furnaces.

Abilities:
- Pockets: extra inventory (rows configurable in balance).
- Fumble: disable offhand use and eating for enemies.
- Hotbar Lock: lock an enemy to their current hotbar slot.
- Amplification: temporarily boosts enchant effectiveness.
- Rich Rush: boosts mob drops and ore yields.

### Terror Gem

Passives:
- Dread Aura: nearby untrusted players get Darkness.
- Fearless: cleanse Blindness and Darkness on the holder.
- Blood Price: on player kill, gain a short Strength/Resistance burst.

Abilities:
- Rig: trap a block; any use, break, step, or block update triggers an explosion (five primed TNT, fuse configurable).
- Remote Charge: arm a block within 10s, then detonate from anywhere within 1 minute (has a cooldown).
- Panic Ring: spawn primed TNT around you.
- Breach Charge: immediate blast at the targeted block or entity with wither-skull style VFX.
- Terror Trade: sacrifice yourself to attempt to kill a target (totems can save). Costs 2 max hearts and 2 permanent energy, limited to 3 uses per player.

### Summoner Gem

Passives:
- Summoner's Bond: summons do not target you or trusted players.
- Commander's Mark: hits mark a target; summons prioritize and gain temporary Strength.
- Soulbound Minions: summons despawn on death or logout.
- Familiar's Blessing: summons spawn with bonus health.

Abilities:
- Summon 1-5: spawn the configured loadouts while staying under the active summon point cap. Summons drop no loot or XP.
- Recall: despawn all active summons.
- Summon slots share a global cooldown.

Summoner UI:
- Open the loadout editor with Gems Modifier + the hotbar key after Recall (default Alt + 7).

Summoned/controlled AI priority (Summoner, Astra Soul Release, Hypno Staff):
- Summoner only: Commander's Mark target overrides all other priorities.
- Otherwise: target the last entity you attacked (player or mob) within command range.
- If none: target whoever last attacked you or is currently hostile toward you.
- If none: follow you.
- Trust-based targeting protection only applies while passives are active (energy > 0 and not suppressed).

### Space Gem

Passives:
- Lunar Scaling: damage and self-healing scale with moon phase.
- Low Gravity: minor slow falling and no fall damage while active.
- Starshield: reduced projectile damage outdoors at night.

Abilities:
- Orbital Laser: strike the block you're looking at; sneak for mining mode.
- Gravity Field: lighten trusted allies and weigh down enemies.
- Black Hole: pull and damage entities in an area.
- White Hole: push and damage entities in an area.

### Reaper Gem

Passives:
- Rot Eater: no negative effects from rotten flesh/spider eyes.
- Undead Ward: reduced damage from undead mobs.
- Harvest: brief regen on mob kills.

Abilities:
- Grave Steed: summon a saddled skeleton horse that decays over time.
- Withering Strikes: melee hits apply Wither temporarily.
- Death Oath: choose a target; you take damage over time but deal bonus damage to them.
- Retribution: incoming damage reflects to the attacker while the effect lasts.
- Scythe Sweep: wide melee cleave in front of you.
- Blood Charge: sacrifice health to buff your next hit or ability.
- Shadow Clone: summon multiple invulnerable decoys that vanish after a short time.

### Pillager Gem

Passives:
- Raider's Training: faster projectiles.
- Shieldbreaker: melee hits can disable shields without an axe (untrusted players).
- Illager Discipline: resistance burst at low health (cooldown).
- Crossbow Mastery: auto-applies Quick Charge II to crossbows.
- Raider's Stride: minor Speed while active.

Abilities:
- Fangs: evoker-fangs line at a target zone.
- Ravage: heavy knockback hit.
- Vindicator Break: melee buff that also disables shields.
- Volley: short arrow burst without ammo.
- Warhorn: buff nearby allies and slow nearby enemies.
- Snare Shot: mark a target with glowing and slowness.

### Spy / Mimic Gem

Passives:
- Stillness Cloak: stand still to become invisible with no particles.
- Silent Step: no sculk triggers.
- False Signature: harder to track with mark/track abilities.
- Backstab: attacking from behind deals bonus damage.
- Quick Hands: minor Haste.

Abilities:
- Mimic Form: after a recent mob kill, gain invisibility + bonus health + speed.
- Echo: replay the last observed ability in front of you.
- Steal: after enough observation, steal an ability; the victim only recovers it if you swap gems or they kill you.
- Smoke Bomb: blind/slow nearby enemies and briefly cloak you.
- Stolen Cast: cast a stolen ability; sneak to cycle.
- Skinshift: steal a targeted player's appearance and name (including chat display) for a short time; the target cannot chat while the effect is active.

### Beacon Gem

Passives:
- Beacon Core: regen pulses for trusted allies; slows untrusted players.
- Stabilize: reduces negative effects on trusted allies; hinders enemies with fatigue.
- Rally: casting a beacon aura grants trusted allies brief Absorption.

Abilities:
- Moving auras (toggle): Speed, Haste, Resistance, Jump Boost, Strength, Regeneration (only one active).
- Aura pulses buff trusted allies and apply Slowness/Weakness to untrusted players in range.

### Air Gem

Passives:
- Windburst Mace (Breach IV, Wind Burst III, Mending, Unbreaking III, Fire Aspect II).
- Aerial Guard: reduced damage and knockback while holding the mace.
- Wind Shear: mace strikes add extra knockback and a short slow.

Abilities:
- Wind Jump: wind-charge style high jump.
- Gale Slam: empower your next mace slam with a stronger wind burst.
- Crosswind: cutting gust forward that knocks back and slows enemies.
- Air Dash: forward dash with brief i-frames.

## Assassin endgame

- If you die while already at the configured assassin trigger hearts (default 5), you become an Assassin.
- Assassins are highlighted red in the player list.
- Assassins have a configurable max hearts cap (`systems.assassinMaxHearts`), cannot consume heart items, and never drop heart items.
- If killed by another Assassin: configurable heart loss/gain (`systems.assassinVsAssassinVictimHeartsLoss`/`systems.assassinVsAssassinKillerHeartsGain`).
- Reaching the elimination threshold (`systems.assassinEliminationHeartsThreshold`) permanently eliminates the Assassin.
- Scoring (after becoming an Assassin): normal kill = +1, final kill = +3.
- The highest-score Assassin is matched against the last non-Assassin survivor (admin-run duel).
