# Gem Mastery & Rivalry Systems

## Gem Mastery (Cosmetic Progression)

Each gem has a mastery track that rewards players with epic cosmetic titles based on ability usage.

### How It Works

- **Tracking**: Every time you successfully use a gem ability, your mastery progress for that gem increases
- **Thresholds**: Epic titles unlock at specific usage milestones
- **Cosmetic Only**: Mastery rewards provide no gameplay advantages
- **Auto-display**: If you haven't selected a title, a leaderboard title shows first (if you hold one), otherwise your highest unlocked title for your active gem is shown

### Title Thresholds

Each gem has 2 unique epic titles:

| Threshold | Tier | Examples |
|-----------|------|----------|
| 100 uses | Epic 1 | "Starweaver" (Astra), "Pyroclast" (Fire), "Windrunner" (Speed) |
| 500 uses | Epic 2 | "Voidwalker" (Astra), "Inferno Incarnate" (Fire), "The Blur" (Speed) |

### Epic Titles by Gem

| Gem | Tier 1 (100 uses) | Tier 2 (500 uses) |
|-----|-------------------|-------------------|
| Astra | Starweaver | Voidwalker |
| Fire | Pyroclast | Inferno Incarnate |
| Flux | Stormcaller | Tempest Eternal |
| Life | Lifebinder | The Undying |
| Puff | Cloudstrider | Skyborne |
| Speed | Windrunner | The Blur |
| Strength | Ironwrought | The Unbreakable |
| Wealth | Goldhand | The Midas Touch |
| Terror | Dreadlord | Nightmare Incarnate |
| Summoner | Beastmaster | Legion Commander |
| Space | Riftwalker | Dimension Breaker |
| Reaper | Soulreaver | Death's Hand |
| Pillager | Warlord | The Conqueror |
| Spy | Shadowblade | The Unseen |
| Beacon | Lightwarden | The Radiant |
| Air | Galeweaver | Hurricane |
| Void | Abyssal | The Hollow |
| Chaos | Anarchist | Entropy Incarnate |
| Prism | Chromatic | The Kaleidoscope |
| Duelist | Bladedancer | The Champion |
| Hunter | Apex Predator | The Huntsman |
| Sentinel | Bulwark | The Immovable |
| Trickster | Illusionist | Master of Deception |

### Title Selection Screen

- Open the title selection screen with the `Title Selection` keybind (default M).
- The screen lists every title with your current progress (`uses / threshold`).
- Click an unlocked title to equip it or use Clear to remove your selection.
- Selected titles display in chat and the tab list with their title colors.
- Your title color also tints your chat message text; if no aura is selected, your aura color follows your title.
- Skinshifted chat uses the target's title and color.
- Admin-forced titles are labeled in the list.

### General Titles (Leaderboard-Based)

In addition to gem-specific titles, there are server-wide general titles awarded to the current leader in each category:

| Title | Requirement |
|-------|-------------|
| Immortal | Fewest deaths (must have at least 1 kill) |
| King Slayer | Most player kills |
| Titan | Most max hearts |
| Whale | Maximum energy (10) |
| Synergy Master | Most synergy triggers |
| Spell Slinger | Most total ability casts |
| Worldbreaker | Most total damage dealt |
| <Gem> Slayer | Most player kills while that gem is active (one leaderboard per gem) |

These titles update every minute based on current server statistics.
Per-gem leaderboards count player kills only.
Damage dealt is tracked in health points (1.0 = half-heart).

## Rivalry System

The Rivalry system assigns players random targets for bonus damage, creating dynamic PvP objectives.

### How It Works

1. **Target Assignment**: On spawn/respawn, you are assigned a random online player as your rival target
2. **Bonus Damage**: Attacks against your rival deal 25% bonus damage (configurable)
3. **HUD Display**: Your current rival is shown in your HUD (orange "Rival: PlayerName")
4. **Reroll on Kill**: When you kill your rival, a new target is automatically assigned
5. **Self-Targeting**: You cannot be assigned yourself as a target
6. **Trusted Players**: Trusted players CAN be assigned as rivals (the bonus persists)

### Settings

Rivalry damage and HUD display can be tuned by the server.

### Notes

- The rivalry target persists until killed, then rerolls
- The same player can be selected again after a reroll
- With only two players online, your rival is always the other player
- If no other players are online, no rival is assigned
- Rival assignments are stored per-player and persist across sessions
