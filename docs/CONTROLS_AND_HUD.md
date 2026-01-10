# Controls and HUD

## Casting and keybind modes

- Cast an ability: hold Gems Modifier (default Left Alt) + press a hotbar number (1-9).
- Controls can be switched to fully custom keybinds via `config/gems/client.json` (`controlMode`: `CHORD` or `CUSTOM`).
  - **CHORD mode** (default): Hold Gems Modifier + hotbar key. The modifier is customizable in Options -> Controls -> Gems.
  - **CUSTOM mode**: Each ability slot has its own keybind (`Gems Ability 1` through `Gems Ability 9`) in Options -> Controls -> Gems. These are unbound by default.
- Client config can disable your own gem passives (`passivesEnabled`).

## Special controls

- Astra Soul Release: the slot after Astra's last ability.
- Summoner loadout UI: Gems Modifier + the hotbar key after Recall (default Alt + 7).
- Selection screen: press B (default) to open the selection screen (requires energy 10/10).
  - Most gems: bonus ability/passive selection.
  - Prism: Prism selection (choose from normal gem powers + bonus pool).
- Bonus abilities: use the dedicated `Bonus Ability 1` and `Bonus Ability 2` keybinds (default C and V; customizable in Options -> Controls -> Gems).
- Chaos slots: LAlt + 1-4 to roll or use chaos abilities.
- Spy observed menu: press O (default) to choose which observed ability Echo/Steal will use.

## HUD

- HUD shows current gem, energy tier, cooldowns, and special states (like Flux charge, Astra soul, or Chaos slots).
