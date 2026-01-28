# Legendary items

## Legendary crafting system

- Legendary crafts use `legendary.craftSeconds` for the timer, `legendary.craftMaxPerItem` for the total per-item limit, and `legendary.craftMaxActivePerItem` for concurrent crafts (default 1).
- Crafting progress is shown to all players via a boss bar with coordinates; on completion the item drops at the crafting table.

## Legendary inscriptions

- Legendary items can be customized with inscription-style augments.
- Legendary item upgrades should work on any legendary item.
- Each legendary item has up to 2 inscription slots by default (configurable).
- Slot counts, rarity tiers, roll weights, and magnitudes are configurable.
- Inscriptions roll a rarity and magnitude on craft, then grant passive buffs while the legendary item is equipped or held.
- Apply inscriptions by holding the legendary item in your other hand and right-clicking the inscription.
- Use the augment/inscription screen (default U) while holding a legendary item to view or remove inscriptions.

## Items

- Blood Oath Blade: gains +1 Sharpness per unique player kill, capped at Sharpness X.
- Challenger's Gauntlet: right-click a player to challenge them to a duel; both players are teleported to a small arena, winner gets energy; loser loses energy.
  - Requires both players to be in the same dimension and not already in a gauntlet duel; on duel end, both players are returned to where they were challenged.
- Chrono Charm: while carried, all gem and bonus ability cooldowns are reduced by 50%.
- Demolition Blade: right-click to arm a demolition charge on a targeted block or entity; cooldown.
- Duelist's Rapier: parry window on right-click; successful parry grants guaranteed crit on next hit.
- Earthsplitter Pick: netherite-tier silk touch pick; right-click toggles 9x9x3 vs 20x2x3 tunnel; respects unbreakable/blacklisted blocks.
- Experience Blade: consume XP to gain Sharpness (10 levels -> Sharpness II, 20 -> IV, 30 -> VI, max Sharpness XX at 100 levels). Enchantment persists until death.
- Gem Seer: right-click to open a selection screen of all known players (online or offline); choose one to view their active gem, energy level, and owned gems with copy counts. No cooldown and not consumed.
- Gladiator's Mark: brand a player; both you and the marked player deal 50% more damage to each other for 60s.
- Hunter's Sight Bow: aim assist toward your last hit target (player or mob) within 50 blocks and line of sight; if none, aims toward the closest target in your line of sight.
- Hunter's Trophy Necklace: right-click a player (or kill a player while wearing it) to open a passive-steal menu; steal multiple passives (up to 10) and keep them across death/logout and gem swaps (the victim can recover stolen passives by killing you, even if they were unselected).
- Hypno Staff: hold a beam on a mob for 3s to convert; works on all mobs except the universal mob blacklist (shared with Summoner/Astra); control is temporary and does not persist; hypnotized mobs follow the summon AI priority.
- Recall Relic: mark current coords; reuse to teleport back (consumes mark); item persists; has cooldown; forceload while marked and released after teleport/no mark.
- Reversal Mirror: right-click to activate for 5s; all incoming damage is reflected back to the attacker.
- Soul Shackle: link yourself to an enemy; damage you take is split between you both for 10s.
- Supreme set: helmet (Night Vision + Water Breathing), chestplate (Strength I), leggings (Fire Resistance), boots (Speed I), full set (Resistance III); stronger effects override temporarily.
- Third-Strike Blade: every 3rd crit within a 5s chain window deals bonus damage.
- Tracker Compass: right-click to pick a player (including offline); shows current/last-known coords, respawn coords, and points toward them.
- Vampiric Edge: each crit heals 1 heart.

Notes:
- Universal mob blacklist (config): applies to Hypno Staff, Summoner summons, and Astra Soul Capture.
