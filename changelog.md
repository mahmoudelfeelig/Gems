# Changelog

## V1.4.1 22.01.2026: Bug fixes and minor improvements
- Fixed bugs that would essentially let you dupe your gems
- Changed persistence augments to extend the duration of abilities instead of reducing the cooldowns
- You no longer respawn with your active gem (only the first join grants a starting gem)


## V1.4 21.01.2026: Balance, Spy, Mimic, Loadouts, Synergies, Wealth, Tracking, Trophy Necklace, and Docs Updates

- Balance: Life Health Drain now respects armor and enchantments; Heart Lock duration lowered to 4 seconds by default; Life Swap reswaps after 15 seconds if both players survive (configurable).
- Spy: observed abilities no longer expire; separate Echo, Steal, and Stolen Cast selections; stolen ability selection persists; stolen passives and abilities show in gem tooltips.
- Mimic Form: bonus stats now scale from the last killed mob's attributes instead of a flat bonus.
- Mirage: spawns a single clone at your aim point that mirrors your movement, breaks on hit or timeout, and grants a short buff.
- Loadouts: preset saves apply immediately; active preset tracking updated; ability order and HUD layout revert to defaults when loadouts are locked by low energy.
- Synergies: per synergy config entries for enable, window, and cooldown; docs list synergies per gem with cooldowns; guidebook updated to match.
- Leaderboards: added per-gem kill leaders plus synergy triggers, total ability casts, and total damage dealt tracking for general titles.
- Titles: title colors show in chat and tab; chat text uses the title color; title auras follow the selected title color if no aura is selected; skinshifted chat uses the target's title and color; titles refresh live; title screen now has Unlocked/All tabs and lists general titles.
- Wealth: Cured Prices trade discounts apply only to active Wealth users, with all trade inputs reduced to one item or one emerald.
- Tracking: Gem Seer and Tracker Compass support offline player data; bounty tracking falls back to the previous owner if you are the last owner.
- Trophy Necklace: per kill passive selection with persistent steals; stolen passives return to the victim if they kill you; victims lose stolen passives while they remain stolen; trophy UI shows stolen passives and lets you enable or disable them.
- Bounties: new bounty board UI and commands; heart and energy bounties with replacement rules and immediate costs; rewards stack on top of normal kill drops; assassin targets void bounties with notifications.
- Ownership: item owner tags no longer prevent stacks from merging; inventory moves preserve ownership.
- Docs: gems and legendary items reordered alphabetically; guidebook gem, synergy, bounties, trust, and titles pages updated with current mechanics.

## V1.3 10.01.2026: New Gems, New Legendary Items, and Combat/UI Fixes

- **New gems**: Void, Chaos, Prism, Duelist, Hunter, Sentinel, and Trickster.
- **New legendary items**:
  - **Gem Seer**: view any online player's active/owned gem info.
  - **Chrono Charm**: cooldowns tick twice as fast while carried (gem + bonus abilities).
  - **Reversal Mirror**: reflect incoming damage back for 5 seconds.
  - **Experience Blade**: consume XP to upgrade Sharpness (up to XX at 100 levels).
  - **Duelist’s Rapier**: parry window; successful parry guarantees a crit.
  - **Hunter’s Trophy Necklace**: right-click (or kill) a player to open a passive-steal menu; steal multiple passives (up to 10) and keep them permanently (persists across gem swaps).
  - **Gladiator’s Mark**: brand a player; both deal 50% more damage to each other for 60s.
  - **Soul Shackle**: link to an enemy; damage you take is split between you for 10s.
  - **Challenger’s Gauntlet**: challenge a player to a duel arena; winner gains energy.
- **New test tool**: Test Dummy entity + spawn egg for quickly testing abilities and items.

## V1.2 28.12.2025: Legendary Items, Crafting, and System Updates

- **Legendary items**: one-of-a-kind crafts with a 10-minute crafting timer, global location announcement, boss-bar progress, and drop-at-table completion.
- **Legendary arsenal**: Tracker Compass, Recall Relic, Hypno Staff, Earthsplitter Pick, Supreme armor set, Blood Oath Blade, Demolition Blade, Hunter’s Sight Bow, Third-Strike Blade, and Vampiric Edge.
- **Recipe system**: legendary discount recipes gated by active gem, auto-unlocked recipes on join, and recipe IDs tied to `legendary.recipeGemRequirements`.
- **Summons AI**: unified AI priority across Summoner, Astra Soul Release, and Hypno (last target -> attacker/hostile -> follow), with summon targeting protections.
- **Terror updates**: rigged blocks detonate on use/step/break/update; breach charge is immediate with wither-style VFX.


## V1.1 22.12.2025: New Gems & Assassin Endgame

- Added **eight new gems** with full passives, ability unlock ordering, config knobs, and GameTest coverage:
  - **Terror**: Darkness aura + self-sacrifice execute (totem-respect), TNT panic ring, limited-use heart/energy costing assassination.
  - **Summoner**: Point-budgeted loadouts (5 slots), commander’s mark focus, recall-all, despawn on death/logoff, no-loot summons.
  - **Space**: Lunar damage scaling, low-gravity mobility, orbital laser (mining mode), gravity field, black/white hole control.
  - **Reaper**: Undead mitigation, regen on harvest, withering strikes, grave steed, scythe sweep, blood-charge amp, illusion clone.
  - **Pillager**: Faster projectiles, shield break on hits, low-HP resistance, evoker fangs, ravage knockback, vindicator break, arrow volley.
  - **Spy/Mimic**: Stillness invisibility, sculk silence, ability observation/echo/steal, smoke bomb escape, stolen-cast cycling.
  - **Beacon**: Core regen + slow aura, stabilize/rally pulses, six moving beacon auras (Speed/Haste/Resistance/Jump/Strength/Regen).
  - **Air**: Windburst mace loadout, aerial guard/skyborn safety, wind jump, gale slam, updraft zone, air dash i-frames.

- **Assassin endgame mode**
  - Dying at **5 max hearts** converts you into an Assassin with a fixed **10-heart cap** and red tablist highlight.
  - Assassins drop/consume **no heart items**; only Assassin kills can restore Assassin heart losses; death to another Assassin costs **-2 max hearts**.
  - Elimination at 0 hearts; kills scored: **+1** normal, **+3** final-kill (the death that triggers Assassin conversion). Highest-scoring Assassin duels the last non-Assassin survivor.

- **Systems & economy changes**
  - Energy ladder enforced: start 3 energy; deaths -1; kills +1 up to Legendary +5 (10) with glint + upgrade drop at cap.
  - Ability availability tied to energy: 0-1 none; 2-4 first abilities only; 5+ all abilities.
  - Heart items respect the **5-heart floor** and block teammate boosting; expensive heart crafting and energy upgrade crafting enabled; Trader item swaps to any gem of same level.
  - Config-driven balance with runtime clamps (`config/gems/balance.json`), hot reload via `/gems reloadBalance` and export via `/gems dumpBalance`.

- **Testing & tooling**
  - Unit + GameTest suites cover gem unlock order, energy ladder, item flows, trader swap, assassin flows, and ability behaviors; headless `./gradlew test` and `./gradlew gametest` remain the release gate.
  - Lint task available for strict compilation: `./gradlew lint`.
