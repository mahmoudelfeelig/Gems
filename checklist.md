# Mod spec checklist

- Core gems: Astra, Fire, Flux, Life, Puff, Speed, Strength, Wealth; default gem given to players.
- Leveling: level 1 unlocks all passives. Levels 2-4 unlock abilities sequentially. Level 5 unlocks all remaining abilities (so if a gem has 5+ abilities, the extras all arrive at level 5; if it has fewer, you stop as soon as all are unlocked). Levels 6-10 are buffer levels with no new powers; max level 10.
- Energy/life: start at 3 energy. Death (any cause) loses 1 energy. The tiers are Legendary 5 → Mythical 4 → Elite 3 → Rare 2 → Common 1 → Broken 0; overflow uses Legendary +1…+5 (6-10). Ability availability scales with energy: at 0-1 energy you have no abilities; at 2-4 energy you keep the first (energy-1) abilities (e.g., energy 3 → first two abilities stay active); at 5+ energy you have all abilities. Kills grant +1 energy up to Legendary +5 (10). Legendary +5 adds an enchant glint; if a Legendary +5 player kills a non-broken player, that victim also drops an upgrade item that anyone can consume to gain a level (not usable by a Legendary +5 player).
- Energy gain overflow: above Legendary uses +1…+5 suffix; death reduces suffix before lowering tiers.
- Gem swapping: a Trader item swaps your gem for another gem of your choice of the same level (you keep both gems but only one is active).
- Level/energy upgrade items: craftable upgrade item -> increases gem energy; crafting hearts adds a heart (up to 20 total hearts max).
- Hearts on death: on death drop one consumable heart item (nether star texture). Right-click to gain max health. Cannot drop below 5 max hearts; if at 5, no heart drops and cannot increase past 20 max total hearts (including the original 10 you spawn as).
- Textures: shared gem base texture with palette swaps per gem; custom textures for upgrade items and heart drops.
- Performance/architecture: composition-first modular system; keep per-tick load minimal; abilities and passives should register/unregister cleanly; add tests/benchmarks as we go.

## Recipes
- Heart (`gems:heart`): shaped `NIN / IGI / NIN` where `N=netherite_scrap`, `I=iron_block`, `G=gold_block`.
- Energy Upgrade (`gems:energy_upgrade`): shaped `NEN / EDE / NEN` where `N=netherite_scrap`, `E=emerald_block`, `D=diamond_block`.
- Trader (`gems:trader`): shaped `DBD / BWB / DBD` where `D=diamond_block`, `B=dragon_breath`, `W=wither_skeleton_skull`.
  - Use: opens a GUI to pick a new gem; consumes 1 Trader and replaces your owned gems with the selected gem (which becomes active).

## Gem abilities and passives
- Astra
  - Passives:
    - Soul Capture: stores the most recently killed mob; can be released later as a summon or resource.
    - Soul Healing: heals the holder on successful soul capture or release; minor regen pulse.
  - Abilities:
    - Shadow Anchor: first activation places an anchor; second activation within a short window returns you to the anchor location.
    - Dimensional Void: suppress gem abilities of enemies in a radius for a brief duration.
    - Astral Daggers: fire rapid, accurate daggers that deal ranged damage.
    - Unbounded: briefly enter Spectator mode, then return to normal gameplay automatically.
    - Astral Camera: enter Spectator mode for scouting, then return to your original position automatically.
    - Spook: apply a brief fear/disorient effect to nearby enemies.
    - Tag: mark a target so they remain tracked/visible through walls for a short time.
- Fire
  - Passives:
    - Fire Resistance: permanent immunity to fire/lava damage.
    - Auto-smelt: smelts broken blocks on drop (ores to ingots, etc.).
    - Auto-enchant Fire Aspect: applies Fire Aspect to held melee weapons.
  - Abilities:
    - Cosy Campfire: place a campfire aura granting allies Regeneration IV in range.
    - Heat Haze Zone: for a short duration, allies in the radius gain Fire Resistance while enemies gain Mining Fatigue and Weakness (no block replacement).
    - Fireball: charge-and-release explosive fireball; charge decays unless standing on obsidian.
    - Meteor Shower: call multiple meteors that explode on impact around a target zone.
- Flux
  - Passives:
    - Charge Storage: consume valuables (diamond/gold/copper blocks, enchanted diamond gear/tools) to charge the beam up to 100%.
    - Ally Inversion: offensive beam effects on trusted players repair their armor durability instead of dealing damage.
    - Overcharge Ramp: once at 100%, after 5s begins charging toward 200% while dealing self-damage each second.
  - Abilities:
    - Flux Beam: long-range beam whose damage/durability shred scales with stored charge (up to 200% one-shot potential).
    - Static Burst: burst built from recent damage taken (archival ability; can be disabled if unused).
- Life
  - Passives:
    - Auto-enchant Unbreaking: applies Unbreaking to held gear.
    - Double Saturation: food restores twice the normal saturation.
  - Abilities:
    - Vitality Vortex: area pulse that provides buffs/heals allies and debuffs enemies based on surroundings (Aquatic near water, Infernal in Nether/near lava, Sculk near sculk blocks, Verdant near plants/leaves, End in the End, otherwise Default).
    - Health Drain: siphon health from a target to heal the user.
    - Life Circle: aura that lowers enemy max health while boosting the user's and trusted allies' max health.
    - Heart Lock: temporarily locks an enemy's max health to their health at cast time.
- Puff
  - Passives:
    - Fall Damage Immunity: negates fall damage entirely.
    - Auto-enchant Power: auto-applies Power to bows.
    - Auto-enchant Punch: auto-applies Punch to bows.
    - Sculk Silence: immune to triggering sculk shriekers.
    - Crop-Trample Immunity: cannot trample farmland.
  - Abilities:
    - Double Jump: midair jump reset with short cooldown.
    - Dash: rapid dash that damages/knocks back targets passed through.
    - Breezy Bash (Uppercut + Impact): launch a target upward; if they land within a short window, they take bonus impact damage.
    - Group Breezy Bash: radial knock-up/knockback on all untrusted players nearby.
- Speed
  - Passives:
    - Speed I: permanent movement speed bonus (tuneable; previously Speed II).
  - Abilities:
    - Arc Shot: fire a lightning arc down a line that strikes up to several enemies along the path, dealing damage and knockback.
    - Speed Storm: field that freezes enemies while granting speed/haste to allies.
    - Terminal Velocity: short burst of Speed III + Haste II.
- Strength
  - Passives:
    - Strength I (intended II): flat damage buff.
    - Auto-enchant Sharpness: Sharpness III at tier 1.
  - Abilities:
    - Nullify: strip active potion/status effects from enemies.
    - Frailer: apply Weakness to enemies.
    - Bounty Hunting: track the owner of an input item for a limited time; item consumed.
    - Chad Strength: every fourth hit deals bonus (~3.5 hearts) damage.
- Wealth
  - Passives:
    - Auto-enchant Mending: applies Mending to tools/armor.
    - Auto-enchant Fortune: applies Fortune to tools.
    - Auto-enchant Looting: applies Looting to weapons.
    - Luck: permanent Luck effect.
    - Hero of the Village: permanent hero status.
    - Durability chip: extra armor damage dealt to enemies per strike.
    - Armor mend on hit: slowly repairs the holder's armor when hitting enemies.
    - Double Debris: furnace outputs double netherite scrap.
  - Abilities:
    - Pockets: opens 9-slot extra inventory UI.
    - Fumble: for a short duration, enemies cannot use their offhand and cannot eat (no other action-cancels).
    - Hotbar Lock: lock an enemy to their current hotbar slot for a short duration (they can still act, but cannot switch away).
    - Amplification: boosts all enchants on tools/armor for ~45 seconds (3-minute cooldown).
    - Rich Rush: boosts mob drops and ore yields for ~3 minutes (9-minute cooldown).

## Level unlock mapping (per gem)
- Level 1: all passives.
- Levels 2-4: unlock abilities in gem-defined order.
- Level 5: unlock any remaining abilities (if more than one remains, unlock them together here).
- Levels 6-10: no new powers; buffer so deaths do not immediately remove abilities.

---

# Next iteration (design / not implemented yet)

## Server gameplay loop: Assassin endgame

- If a player dies while already at **5 max hearts**, they become an **Assassin**.
- Assassins are highlighted **red** in the player list/tab UI.
- Assassins:
  - Static **10 hearts** max.
  - Never drop heart items and cannot consume heart items.
  - If killed by another Assassin: **-2 max hearts**.
  - Can only regain those lost hearts by **killing other Assassins**.
  - If they reach **0 hearts**: eliminated permanently.
  - Cannot exceed **10 hearts** even when killing another Assassin at 10 hearts.
- Scoring (only after becoming an Assassin):
  - Normal kill: **+1 point**
  - Final kill: **+3 points**
    - Final kill = killing a player who was at 5 hearts and thereby turning them into an Assassin.
- Winner selection uses points (admin-run duel afterwards):
  - Highest-point Assassin vs last surviving non-Assassin.

## Heart items: team restriction

- Heart items should not be consumable by teammates (trusted players), to prevent team boosting.

## Speed gem: scaling + expansion

- Add Haste as a passive.
- Add “acceleration” scaling so Speed abilities scale with the caster’s current movement speed at cast time.
- Add more passives/abilities for Speed.

Proposed Speed passives:
- Speed I, Haste I
- Momentum scaling snapshot at cast time (horizontal speed → `[0..1]` momentum).
- Frictionless Steps: reduced slowdown from cobweb/honey/slow powder.

Proposed Speed abilities to add:
- Slipstream: forward wind lane that buffs trusted allies and disrupts enemies.
- Afterimage: short “blur” invis/speed that breaks on first hit.

## New gems to add (docs only for now)

### 1) Terror gem

- Ability: kill yourself to kill a target player; if they have a Totem their Totem pops instead.
- Cost: **-2 hearts and -2 permanent energy** (even for Assassins).
- Hard cap: **3 uses per player total**, persistent across gem changes and deaths.
- Ability: spawn 5 primed TNT around you.
- Passives (proposed):
  - Dread Aura (Darkness pulses on untrusted nearby)
  - Fearless (immune to Darkness/Blindness from gem sources)
  - Blood Price (short Strength burst on player kill)

### 2) Summoner gem

- Passives (proposed):
  - Summon Mastery (never targets owner/trusted)
  - Commander’s Mark (sword-hit mark → summons prioritize + bonus damage)
  - Pack Tactics (small bonus when multiple summons nearby)
  - Soulbound (despawn on owner death/logoff)
- “Mana/coin” point system:
  - You have a total point budget and per-mob costs.
  - Ability slots 1–5 each spawn the configured summon for that slot.
  - Summons are never hostile to you or your trust list.
  - Summons prioritize targets you hit with a sword within the last 3 seconds, with increased damage.
  - All costs + max points configurable; excludes Wither/Ender Dragon.
  - Summons drop no loot or XP.
  - Extra ability (proposed): Recall (teleport active summons to you; limited range).

### 3) Space gem

- Passives (proposed):
  - Lunar Scaling (moon phase multiplier)
  - Low Gravity (minor slow falling / reduced fall damage)
  - Starshield (reduced projectile damage outdoors at night)
- Ability: orbital laser strike in a selected area (shift mining mode; can mine hard blocks like obsidian).
- Ability: gravity control in an area (per-player “lighter/heavier” control).
- Ability: black hole on your location that pulls/damages mobs and players.

### 4) Reaper gem

- Passives (proposed):
  - Rot Eater (no negative effects from rotten flesh/spider eyes)
  - Undead Ward (reduced damage from undead mobs)
  - Harvest (brief regen on mob kills, capped)
- Ability: summon a skeleton horse mount (saddled) that loses health over time.
- Ability: apply Wither on hit temporarily.
- Ability: reveal distance to a player; sacrifice health until you hit them.
- Ability: scythe arc attack (5-block reach).
- Ability: hold-to-charge health sacrifice that buffs next attacks/abilities.
- Ability: summon a clone illusion (looks real, attacks, but deals no damage).

### 5) Pillager gem

- Passives (proposed):
  - Raider’s Training (better bow/crossbow handling)
  - Shieldbreaker (extra shield disable time on hits)
  - Illager Discipline (brief Resistance at low HP, cooldown)
- Ability: evoker fangs attack.
- Ability: ravager-style knockback hit.
- Ability: “vindicator break”: add damage and disable shields like an axe hit (without axe cooldown).
- Ability: auto-arrow burst (every 0.5s for 3 seconds).

### 6) Spy / mimic gem

- Passive: if you stay still for 5 seconds, become invisible (no particles).
- Passives (proposed):
  - Silent Step (no footsteps + no sculk triggers)
  - False Signature (hide gem indicators from enemies)
  - Quick Hands (reduced item-use slowdown while invisible)
- Ability: transform into the last mob you killed (model + health/hitbox/speed).
- Ability: copy an ability used in front of you.
- Ability: “observe theft”: if you witness an ability used 4 times in 10 minutes and stay alive, permanently steal it (until you switch gems; max 3 stolen abilities; choose which ability).
- Spy abilities (proposed):
  - Decoy (fake player decoy, no damage)
  - Wiretap (briefly see nearby players’ last ability used)

### 7) Beacon gem (support/utility)

- Passives (proposed):
  - Beacon Core (minor regen pulses for trusted allies nearby)
  - Stabilize (reduces negative effect durations on trusted allies nearby)
  - Rally (absorption burst for trusted allies when you cast a beacon aura)
- Abilities 1–6: moving maxed-out beacon effects for you + trusted allies.
  - Proposed effects: Speed, Haste, Resistance, Jump Boost, Strength, Regeneration.

### 8) Air gem (mace-focused)

- Passive: grants a maxed-out mace (Breach IV, Wind Burst III, Mending, Unbreaking III, Fire Aspect II).
- Passives (proposed):
  - Aerial Guard (reduced fall damage/knockback while holding the mace)
  - Skyborn (brief slow falling after taking damage while airborne, cooldown)
- Ability: wind-charge style high jump.
- Air abilities (proposed):
  - Gale Slam (empower next mace slam: stronger wind burst)
  - Updraft Zone (pillar that lifts trusted allies, disrupts enemies)
  - Air Dash (mid-air dash with brief i-frames)
