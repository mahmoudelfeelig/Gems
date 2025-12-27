# Mod spec checklist

- Gems: see `gameplay.md` for abilities, passives, and gem lists.
- Leveling: level 1 unlocks all passives. Levels 2-4 unlock abilities sequentially. Level 5 unlocks all remaining abilities (so if a gem has 5+ abilities, the extras all arrive at level 5; if it has fewer, you stop as soon as all are unlocked). Levels 6-10 are buffer levels with no new powers; max level 10.
- Energy/life: start at 3 energy. Death (any cause) loses 1 energy. The tiers are Legendary 5 -> Mythical 4 -> Elite 3 -> Rare 2 -> Common 1 -> Broken 0; overflow uses Legendary +1...+5 (6-10). Ability availability scales with energy: at 0-1 energy you have no abilities; at 2-4 energy you keep the first (energy-1) abilities (e.g., energy 3 -> first two abilities stay active); at 5+ energy you have all abilities. Kills grant +1 energy up to Legendary +5 (10). Legendary +5 adds an enchant glint; if a Legendary +5 player kills a non-broken player, that victim also drops an upgrade item that anyone can consume to gain a level (not usable by a Legendary +5 player).
- Energy gain overflow: above Legendary uses +1...+5 suffix; death reduces suffix before lowering tiers.
- Gem swapping: a Gem Trader item swaps only your active gem for another gem of your choice; other owned gems stay owned, and the selected gem becomes active.
- Gem purchase token: a consumable item that lets you pick any gem to add to your owned set and activate.
- Level/energy upgrade items: craftable upgrade item -> increases gem energy; crafting hearts adds a heart (up to 20 total hearts max).
- Hearts on death: on death drop one consumable heart item (nether star texture). Right-click to gain max health. Cannot drop below 5 max hearts; if at 5, no heart drops and cannot increase past 20 max total hearts (including the original 10 you spawn as).
- Textures: shared gem base texture with palette swaps per gem; custom textures for upgrade items and heart drops.
- Performance/architecture: composition-first modular system; keep per-tick load minimal; abilities and passives should register/unregister cleanly; add tests/benchmarks as we go.
- Gem definitions: data-driven via `data/gems/gem_definitions.json`.

## Recipes
- Heart (`gems:heart`): shaped `NIN / IGI / NIN` where `N=netherite_scrap`, `I=iron_block`, `G=gold_block`.
- Energy Upgrade (`gems:energy_upgrade`): shaped `NEN / EDE / NEN` where `N=netherite_scrap`, `E=emerald_block`, `D=diamond_block`.
- Gem Trader (`gems:gem_trader`): shaped `DBD / BWB / DBD` where `D=diamond_block`, `B=dragon_breath`, `W=wither_skeleton_skull`.
  - Use: opens a GUI to pick a new gem; consumes 1 Gem Trader and replaces only your active gem with the selected gem (which becomes active).
- Gem Purchase Token (`gems:gem_purchase`): shaped `EBE / DND / EBE` where `N=netherite_block`, `B=beacon`, `D=diamond_block`, `E=end_crystal`.
  - Use: opens a GUI to pick any gem; consumes 1 token, adds the gem to your owned set, and sets it active.

## Level unlock mapping (per gem)
- Level 1: all passives.
- Levels 2-4: unlock abilities in gem-defined order.
- Level 5: unlock any remaining abilities (if more than one remains, unlock them together here).
- Levels 6-10: no new powers; buffer so deaths do not immediately remove abilities.

---

# Systems

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
