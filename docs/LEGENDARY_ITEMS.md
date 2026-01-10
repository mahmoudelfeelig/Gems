# Legendary items

## Legendary crafting system

- Legendary crafts use `legendary.craftSeconds` for the timer, `legendary.craftMaxPerItem` for the total per-item limit, and `legendary.craftMaxActivePerItem` for concurrent crafts (default 1).
- Crafting progress is shown to all players via a boss bar with coordinates; on completion the item drops at the crafting table.

## Items

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
- Gem Seer: right-click to open a selection screen of all online players; choose one to view their active gem, energy level, and owned gems. No cooldown and not consumed.
- Challenger's Gauntlet: right-click a player to challenge them to a duel; both players are teleported to a small arena, winner gets energy; loser loses energy.
  - Requires both players to be in the same dimension and not already in a gauntlet duel; on duel end, both players are returned to where they were challenged.
- Duelist's Rapier: parry window on right-click; successful parry grants guaranteed crit on next hit.
- Experience Blade: consume XP to gain Sharpness (10 levels -> Sharpness II, 20 -> IV, 30 -> VI, max Sharpness XX at 100 levels). Enchantment persists until death.
- Reversal Mirror: right-click to activate for 5s; all incoming damage is reflected back to the attacker.
- Hunter's Trophy Necklace: on player kill, permanently gain the victim's highest-level gem passive (persists through death/logout).
- Gladiator's Mark: brand a player; both you and the marked player deal 50% more damage to each other for 60s.
- Soul Shackle: link yourself to an enemy; damage you take is split between you both for 10s.
- Chrono Charm: while carried, all gem and bonus ability cooldowns are reduced by 50%.
- Universal mob blacklist (config): applies to Hypno Staff, Summoner summons, and Astra Soul Capture.

