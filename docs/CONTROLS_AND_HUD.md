# Controls and HUD

## Casting and keybind modes

- Cast an ability: hold Gems Modifier (default Left Alt) + press a hotbar number (1-9).
- Controls can be switched to fully custom keybinds via `config/gems/client.json` (`controlMode`: `CHORD` or `CUSTOM`).
  - **CHORD mode** (default): Hold Gems Modifier + hotbar key. The modifier is customizable in Options -> Controls -> Gems.
  - **CUSTOM mode**: Each ability slot has its own keybind (`Gems Ability 1` through `Gems Ability 9`) in Options -> Controls -> Gems. These are unbound by default.
- Client config can disable your own gem passives (`passivesEnabled`).

## Special controls

- Astra Soul Release: the slot after Astra's last ability.
- Summoner loadout UI: Gems Modifier + the hotbar key after the last ability slot (default Alt + 7) while using Summoner.
- Loadout presets manager: press L (default) to open the loadout presets screen.
- Guidebook: press ~ (tilde) to open the in-game docs viewer.
- Augment/inscription screen: press U (default) while holding a gem or legendary item to manage augments or inscriptions for that specific item.
- Title selection: press M (default) to pick your active title and view progress across unlocked titles.
- Selection screen: press B (default) to open the selection screen (requires energy 10/10).
  - Most gems: bonus ability/passive selection.
  - Prism: Prism selection (choose from normal gem powers + bonus pool).
- Bonus abilities: use the dedicated `Bonus Ability 1` and `Bonus Ability 2` keybinds (default C and V; customizable in Options -> Controls -> Gems).
- Toggle control mode: optional keybind (unbound by default) to switch between CHORD and CUSTOM at runtime.
- Chaos slots: LAlt + 1-4 to roll or use chaos abilities.
- Spy observed menu: press O (default) to choose which observed ability Echo/Steal will use; observations persist until used (Echo consumes 1, Steal consumes 4).

## HUD

- HUD shows current gem, energy tier, cooldowns, and special states (like Flux charge, Astra soul, or Chaos slots).

## Loadout Presets (Energy 6+)

Players with energy level 6 or higher can save and load loadout presets for each gem.

**Preset manager UI:** Open the loadout presets screen with the `Loadout Presets` keybind (default L).

### Features

- **Per-Gem Storage**: Save up to 5 presets per gem
- **Saved Settings**:
  - Ability order preferences
  - Passive toggles (enabled/disabled)
  - HUD layout (position, show cooldowns, show energy, compact mode)
- **Energy Gating**: Switching presets respects your current ability unlocks and energy level

### HUD Layout Options

Each preset can store custom HUD layout preferences:
- **Position**: TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
- **Show Cooldowns**: Toggle cooldown display
- **Show Energy**: Toggle energy display
- **Compact Mode**: Smaller HUD footprint

### Configuration

```json
{
  "loadouts": {
    "enabled": true,
    "unlockEnergy": 6,
    "maxPresetsPerGem": 5
  }
}
```

| Setting | Default | Description |
|---------|---------|-------------|
| `enabled` | `true` | Enable/disable loadout presets |
| `unlockEnergy` | `6` | Minimum energy level to use presets |
| `maxPresetsPerGem` | `5` | Maximum presets per gem |
