# Changelog

## 22.12.2025: New Gems & Assassin Endgame

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
  - Ability availability tied to energy: 0–1 none; 2–4 first abilities only; 5+ all abilities.
  - Heart items respect the **5-heart floor** and block teammate boosting; expensive heart crafting and energy upgrade crafting enabled; Trader item swaps to any gem of same level.
  - Config-driven balance with runtime clamps (`config/gems/balance.json`), hot reload via `/gems reloadBalance` and export via `/gems dumpBalance`.

- **Testing & tooling**
  - Unit + GameTest suites cover gem unlock order, energy ladder, item flows, trader swap, assassin flows, and ability behaviors; headless `./gradlew test` and `./gradlew gametest` remain the release gate.
  - Lint task available for strict compilation: `./gradlew lint`.
