# Gems

## Overview

Gems is a PvP-first RPG progression mod built around gem archetypes. Each gem grants a unique set of passives and active abilities, unlocked through a level and energy system that rewards kills, punishes deaths, and creates constant power swings. Progress carries across swaps, so you can change playstyles without starting over.

## Core Loop: Energy, Levels, and Unlocks

- You spawn with a random gem at 3 energy.
- Kills grant +1 energy and death removes -1 energy.
- Energy tiers: Broken (0) -> Common (1) -> Rare (2) -> Elite (3) -> Mythical (4) -> Legendary (5) -> Legendary +1 to +5 (6 to 10).
- Ability access scales with energy:
  - 0 to 1: passives and abilities disabled
  - 2 to 4: keep the first (energy - 1) abilities active
  - 5 plus: full kit online
- Leveling (max level 10):
  - Level 1: all passives
  - Levels 2 to 4: unlock abilities in gem-defined order
  - Level 5: unlock any remaining abilities
  - Levels 6 to 10: buffer levels (no new powers)

## Hearts, Death, and the Assassin Endgame

- On death, players drop one Heart item that increases max health when consumed.
- Max health limits: cannot drop below 5 hearts and cannot exceed 20 total hearts.
- If you die while already at 5 max hearts, you become an Assassin:
  - Highlighted red in the player list and tab UI
  - Fixed 10-heart cap
  - Never drops hearts and cannot consume heart items
  - If killed by another Assassin: -2 max hearts
  - Can only regain those hearts by killing other Assassins
  - Reaching 0 max hearts permanently eliminates the Assassin
- Scoring (only after becoming an Assassin):
  - Normal kill: +1 point
  - Final kill (turning someone into an Assassin): +3 points
  - Winner selection uses points (admin-run duel afterwards): highest-score Assassin vs last surviving non-Assassin

## Gem Swapping, Ownership, and Upgrades

- Gems are bound to their owner. You can own multiple gems, but only one is active.
- Gem Trader swaps only your active gem (owned gems stay owned).
- Gem Purchase Token lets you pick any gem to add to your owned set and activate.
- Craft Energy Upgrades to increase gem energy (up to Legendary +5).
- At Legendary +5, players gain an enchant glint. If a Legendary +5 player kills a non-broken player, the victim drops an upgrade item that anyone can consume for a level (not usable by Legendary +5).

## Legendary Items

Legendary items are unique, server-wide crafts designed to create objectives, conflict, and endgame moments.

- Each legendary craft takes 10 minutes.
- When crafting starts, the crafter’s location is announced globally.
- Crafting progress is shown to all players via a boss bar with coordinates.
- When complete, the item drops at the crafting table.
- Some recipes require a player head that is not your own.

### Legendary item list

- Gem Seer: view any known player's gem info and status.
- Tracker Compass: pick a player (including offline) and track current or last-known coords, respawn coords, and direction.
- Recall Relic: mark your location, then teleport back later (consumes the mark). Persists through use and has a cooldown; chunks are forceloaded while marked.
- Hypno Staff: hold a beam on a mob to temporarily convert it (bosses excluded). Controlled mobs follow summon-style AI priorities.
- Earthsplitter Pick: netherite-tier Silk Touch pick with a toggleable tunneling mode (3x3x3 vs 9x3x1), respecting protected or unbreakable blocks.
- Supreme Set: legendary armor set with strong utility effects; full set grants Resistance.
- Blood Oath Blade: gains +1 Sharpness per unique player kill (capped).
- Demolition Blade: arm a demolition charge on a targeted block or entity (cooldown).
- Hunter’s Sight Bow: aim assist toward your last hit target within range and line of sight.
- Third Strike Blade: every 3rd crit within a short chain window deals bonus damage.
- Vampiric Edge: critical hits heal you.
- Chrono Charm: cooldowns tick faster while carried.
- Reversal Mirror: reflect incoming damage for a short window.
- Experience Blade: consume XP to upgrade Sharpness levels.
- Duelist’s Rapier: parry window; successful parry guarantees a crit.
- Hunter’s Trophy Necklace: steal passives from defeated players.
- Gladiator’s Mark: brand a player; both deal more damage to each other for a short time.
- Soul Shackle: link to an enemy; damage you take is split between you.
- Challenger’s Gauntlet: challenge a player to a duel arena; winner gains energy.

## Gem Roster Highlights

- Astra: soul capture and release, anchor return, ability suppression void, ranged daggers, scouting tools, tracking tag.
- Fire: fire immunity, auto-smelt and enchant, healing campfire aura, debuff zones, chargeable fireball, meteor shower.
- Flux: charge-based scaling up to 200 percent, beam with armor damage, damage to charge conversion, overcharge ramp with self-damage.
- Life: sustain and max-health control, adaptive vitality vortex, health drain, life circle aura pressure, heart lock, and timed life swap reswap.
- Puff: fall immunity and mobility kit, double jump, dash damage, knock-ups, peel tools.
- Speed: speed and haste core with momentum scaling, lightning arc shot, storm field control, terminal burst, frictionless steps, slipstream and afterimage mobility.
- Strength: raw melee power, sharpness auto-enchant, nullify effects, weaken enemies, bounty tracking.
- Wealth: loot and durability economy, pockets inventory, disables eating or offhand use, hotbar locking, enchant amplification, rich-rush drops.
- Terror: traps and explosives, rigged blocks, remote detonation, panic ring, breach charges.
- Summoner: configurable minions per slot with costs and budget, Commander’s Mark focus, recall safety.
- Space: lunar scaling, low gravity, projectile shield at night, orbital laser, gravity control zones, black hole pull, white hole push.
- Reaper: undead resistance and regen on kills, withering hits, death oath targeting, retribution reflect, scythe arcs, skeletal mount.
- Pillager: faster projectiles, shieldbreaking, resist at low HP, evoker fangs zoning, ammo-free volleys.
- Spy: stillness invisibility, silent sculk, signature masking, backstab bonus, mimic form, echo replay, ability steal, smoke bomb, stolen cast cycling, skinshift disguise.
- Beacon: mobile beacon core with pulses for trusted allies, stabilize support, absorption rally, toggleable moving auras.
- Air: mace-focused wind kit, reduced knockback and fall, wind-charge jump, gale slam, crosswind gust, air dash.
- Chaos: random rolls on abilities and passives from the gem roster with reduced cooldowns.

## Server-Friendly Balancing

- Balance values live in `config/gems/balance.json` (generated on first run).
- Reload without restarting: `/gems reloadBalance` (op level 2 plus).
- Export current sanitized values: `/gems dumpBalance`.
- Includes sane clamps and optional particle and sound caps for large servers.

## Credits

This project uses (or is inspired by) the following third-party assets:

### Logo
- https://magory.itch.io/ultimate-gem-collections (logo + background + gem icons)

### Icons / Items

- https://kanomwan.itch.io/kw-16x16-gems-icon?download (gems)
- https://karsiori.itch.io/free-pixel-art-gem-pack (gem trader + energy upgrade + gem purchase)
- https://skristi.itch.io/heart-and-health-bars (heart item)
- https://snoopethduckduck.itch.io/things (compass + augments + inscriptions)

### Weapons

- https://season-penguin.itch.io/free-fantasy-weapon-pack (weapons)
- https://runicpixels.itch.io/swordtember-2023-collection (swords)
