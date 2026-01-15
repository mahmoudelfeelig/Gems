# Mechanics reference (developers)

This file is the developer-facing source-of-truth for implemented gameplay mechanics and invariants.

- Player-facing docs live under `docs/` (start at `gameplay.md` and `docs/README.md`).
- Server-owner docs: `server-ops.md` and `balance.md`.

## Progression and leveling

### Energy tiers and overflow

- Players start at **3 energy**.
- Energy tiers map to numeric levels:
  - Broken (0), Common (1), Rare (2), Elite (3), Mythical (4), Legendary (5), Legendary +1..+5 (6-10).
- **Kills (player-vs-player)** grant **+1 energy** up to Legendary +5 (10).
- **Death (any cause)** loses **-1 energy**.
- Overflow behavior:
  - Above Legendary (5), the `+n` suffix (6-10) is reduced first on death.
  - Only after reaching 5 does the tier drop below Legendary.

### Unlock rules (abilities and passives)

- **Level 1** unlocks all passives.
- **Levels 2-4** unlock abilities sequentially (gem-defined order).
- **Level 5** unlocks all remaining abilities at once (if any remain).
- **Levels 6-10** are **buffer only** (no new powers).

### Ability gating by energy

- At energy **0-1**: **no abilities**.
- At energy **2-4**: keep only the **first (energy - 1)** abilities (e.g. energy 3 -> first two abilities remain available).
- At energy **5+**: **all abilities** are available.
- Passives are active at energy **1+**, unless client-side passives are disabled (see “Client config”).

## Hearts (max health) system

- On death, players drop one consumable **Heart** item (unless already at the minimum max hearts).
- Consuming a Heart increases max hearts up to the configured cap (default: 20 total hearts).
- Minimum max hearts is configurable (default: 5).
- Trusted players cannot consume Heart items (prevents team boosting).
- Players also drop their head on death (cosmetic + used by some recipes).
- A Test Dummy spawn egg exists for testing abilities/items (`gems:test_dummy_spawn_egg`).

## Gem ownership, swapping, and penalties

- Gems are **bound to an owner**. Players can own multiple gems, but only one is active at a time.
- Gem swapping:
  - **Gem Trader** swaps only the active gem.
  - **Gem Purchase Token** adds a selected gem to the owned set and activates it.
- Activating another player’s gem applies the ownership penalty:
  - Kills the victim.
  - Skips their heart drop.
  - Reduces their max hearts by 2.
  - Caps their energy at 1.

## Trust rules

- Trust only affects **ally-targeted** effects (e.g. “buff allies” vs “harm enemies”).
- PVP is always allowed; trust does not block direct combat.

## Upgrade items and Legendary +5

- Legendary +5 gives the player an enchant glint.
- If a Legendary +5 player kills a non-broken player, the victim drops an **upgrade item**.
- Upgrade items cannot be used by Legendary +5 players.

## Recipes and unlocks

- All recipes unlock automatically on player join.
- Recipe sources:
  - Core items + legendary items: `src/main/resources/data/gems/recipe/*.json`
  - Discount recipes: `*_discount` variants
- Legendary discount recipes are gem-gated via `legendary.recipeGemRequirements` (map of recipe id -> required active gem id).

## Legendary item system

- Legendary crafts:
  - Use `legendary.craftSeconds`, `legendary.craftMaxPerItem`, and `legendary.craftMaxActivePerItem`.
  - Show a server-wide boss bar with coordinates during crafting.
  - Drop the item at the crafting table on completion.
- Universal mob blacklist:
  - A single config list (`mobBlacklist`) applies to Hypno Staff, Summoner summons, and Astra Soul Capture.

## Bonus pool system (energy 10)

- At energy 10/10, any gem holder can claim up to:
  - 2 bonus abilities
  - 2 bonus passives
- Claims are **unique** per player:
  - Once claimed, no other player can claim/use that power until it is released.
  - Claims are released when a player drops below energy 10.
- Bonus casting:
  - Bonus abilities are cast via the dedicated keybinds (default C and V).
- Balance:
  - Bonus pool tuning lives under `bonusPool.*` in `config/gems/balance.json`.

## Special gems

### Void

- Void provides “Void Immunity”: immune to all gem abilities and passives from other players.

### Chaos

- Chaos provides configurable independent random ability slots (tied to the Chaos Agent passive).

### Prism

- At energy 10/10, Prism players select:
  - Up to 3 normal gem abilities + 2 bonus abilities
  - Up to 3 normal gem passives + 2 bonus passives
- Blacklisted powers cannot be selected.

## Assassin endgame

- If a player dies while already at the assassin trigger hearts, they become an Assassin.
- Assassins:
  - Are highlighted red in tab.
  - Have a max-hearts cap (`systems.assassinMaxHearts`).
  - Cannot consume Heart items and never drop Heart items.
  - Are permanently eliminated when hearts <= `systems.assassinEliminationHeartsThreshold`.
- Assassin-vs-Assassin kill rules:
  - On being killed by an Assassin, the victim loses `systems.assassinVsAssassinVictimHeartsLoss` hearts.
  - The killer gains `systems.assassinVsAssassinKillerHeartsGain` hearts (up to the cap).
  - The killer also takes all of the victim’s accumulated Assassin points.
- Scoring (after becoming an Assassin):
  - Normal kill: +1 point
  - Final kill: +3 points (killing a player at the trigger hearts, turning them into an Assassin)
- The highest-score Assassin is matched against the last non-Assassin survivor (admin-run duel).

## Client config and controls

- Client config lives in `config/gems/client.json` and is not synced server-side.
- `passivesEnabled`: toggles the client’s own passives.
- `controlMode`:
  - `CHORD`: modifier + hotbar (default)
  - `CUSTOM`: dedicated ability keybinds

## Data-driven definitions and architecture notes

- Gem definitions are data-driven via `data/gems/gem_definitions.json` (in-repo path: `src/main/resources/data/gems/gem_definitions.json`).
- Abilities/passives are intended to be composable components; avoid deep inheritance.
- Performance constraints:
  - Avoid per-tick world scans when possible.
  - Cache player state and cleanly register/unregister listeners.
  - Keep abilities server-authoritative; client should be UI only.

## Planned feature scope

### Loadout presets

- Presets unlock at **energy 6** (no new power; UI/quality-of-life unlock).
- Presets are per-gem saved ability order + passive toggles + HUD layout.
- Preset swaps are server-authoritative and must respect current unlocks/energy gating.

### Gem augments

- Augments are craftable items for both gems and legendary items.
- Recipes must require **player heads** and otherwise use **unique ingredients**.
- Augments apply configurable modifiers to abilities/passives with caps and conflicts.
- All slot counts, rarity tiers, roll weights, and magnitude ranges are **configurable**.
- Gem augments:
  - Max slots (default **4**).
  - Rarity tiers (default **3**).
- Draft recipe direction (subject to balance tuning):
  - Gem augment core: `player_head` + `echo_shard` + `nether_star` + gem-aligned ingredient.
  - Legendary inscription core: `player_head` + `ancient_debris` + `dragon_breath` + legendary-aligned ingredient.
  - Each augment type adds a unique catalyst (e.g., wind charge for mobility, blaze rod for fire, phantom membrane for evasion).
  - Output is an augment item tagged with its target (gem/legendary) and modifier id.

### Legendary item customization

- Legendary items accept augment-style customizations (inscriptions/sockets).
- Customizations are data-driven and capped per item.
- All slot counts, rarity tiers, roll weights, and magnitude ranges are **configurable**.
- Legendary customizations:
  - Max slots (default **2**).
  - Rarity tiers (default **3**).
  - Two categories: **universal** (works on all legendary items) and **item-specific** (bound to a single legendary item).

### Team synergies

- Synergies trigger when two different gem abilities are cast within a short window.
- Must work for multi-gem players casting their own abilities and across trusted allies.
- Synergy triggers are event-driven (no heavy per-tick scans).

### Gem mastery tracks (cosmetic)

- Per-gem mastery tracks unlock **titles** and **aura particles** only.
- Players can customize and mix unlocked titles/aura cosmetics.

### Rivalry system

- On spawn, assign **one target** you deal extra damage to (no “take more damage” rival).
- Target can be trusted; cannot be yourself.
- The bonus persists until you **kill the target**, then reroll (target may repeat).
- Works with only 2 players online (always picks the other player).
- Damage bonus is configurable (per-rarity or flat).

## Testing coverage gaps (to reach full coverage)

### Abilities (beyond smoke tests)

- Summoner: slots 2-5 activation/cooldowns and point cap behavior.
- Beacon: aura types Speed/Haste/Jump/Regeneration activation and toggling.
- Chaos: slot roll, ability use cooldown, expiry, and passive apply/remove per slot.
- Astra: Soul Release (capture -> release flow, blacklist, no loot/XP).
- Duelist: Mirror Match barrier confinement behavior + skin/name copy.
- Hunter: Origin Tracking (first-owner tracking, offline owner message).
- Spy: Skinshift (name/skin/chat lock behavior).
- Sentinel: Lockdown zone blocks movement abilities.
- Trickster: Puppet Master and Mind Games effect enforcement.

### Non-bonus passives (missing explicit tests)

- Astra: Soul Capture, Soul Healing.
- Fire: Fire Resistance, Auto Smelt, Auto-enchant Fire Aspect.
- Flux: Ally Inversion, Overcharge Ramp, Capacitor, Conductivity, Insulation.
- Life: Auto-enchant Unbreaking, Double Saturation.
- Puff: Fall Damage Immunity, Auto-enchant Power, Auto-enchant Punch, Sculk Silence, Crop-trample Immunity, Windborne.
- Speed: Speed I, Haste I, Frictionless Steps.
- Strength: Strength I, Auto-enchant Sharpness, Adrenaline.
- Wealth: Auto-enchant Mending, Auto-enchant Fortune, Auto-enchant Looting, Luck, Hero of the Village, Durability Chip, Armor Mend on Hit, Double Debris.
- Terror: Blood Price.
- Summoner: Bond, Commander’s Mark, Soulbound Minions, Familiar’s Blessing.
- Space: Low Gravity, Starshield.
- Reaper: Rot Eater, Undead Ward, Harvest.
- Pillager: Raider’s Training, Shieldbreaker, Crossbow Mastery, Raider’s Stride.
- Spy: Stillness Cloak, Silent Step, False Signature, Backstab, Backstep, Quick Hands.
- Beacon: Beacon Core, Stabilize, Rally (status pulses).
- Air: Windburst Mace, Aerial Guard, Wind Shear.
- Chaos: Random Rotation passive.
- Duelist: Combat Stance.
- Hunter: Tracker’s Eye.
- Sentinel: Fortress, Retribution Thorns.
- Trickster: Slippery.

### Legendary items (missing gametests)

- Tracker Compass (tracking UI + last-known coords).
- Recall Relic (mark/teleport/forceload lifecycle).
- Hypno Staff (conversion window, blacklist, AI priority).
- Earthsplitter Pick (mode toggle and block blacklist).
- Supreme armor set (piece + full-set effects).
- Blood Oath Blade (kill tracking, Sharpness cap).
- Demolition Blade (charge placement + detonation).
- Hunter’s Sight Bow (aim assist behavior).
- Third-Strike Blade (third-crit bonus within window).
- Vampiric Edge (crit heal).
- Gem Seer (player selection + info display).
- Duelist’s Rapier (parry window + guaranteed crit).
- Experience Blade (XP -> Sharpness scaling and persistence).
- Reversal Mirror (reflect duration + attacker damage).
- Hunter’s Trophy Necklace (passive-steal UI + persistence).
- Gladiator’s Mark (mutual damage amp).
- Soul Shackle (damage split).
- Chrono Charm (cooldown reduction + stacking + HUD sync).
