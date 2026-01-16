# Gem Mastery & Rivalry Systems

## Gem Mastery (Cosmetic Progression)

Each gem has a mastery track that rewards players with epic cosmetic titles and aura effects based on ability usage.

### How It Works

- **Tracking**: Every time you successfully use a gem ability, your mastery progress for that gem increases
- **Thresholds**: Epic titles and auras unlock at specific usage milestones
- **Mix & Match**: You can combine any unlocked title with any unlocked aura, regardless of which gem they came from
- **Cosmetic Only**: Mastery rewards provide no gameplay advantages

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

### General Titles (Leaderboard-Based)

In addition to gem-specific titles, there are server-wide general titles awarded to the current leader in each category:

| Title | Requirement |
|-------|-------------|
| Immortal | Fewest deaths (must have at least 1 kill) |
| King Slayer | Most player kills |
| Titan | Most max hearts |
| Whale | Maximum energy (10) |

These titles update every minute based on current server statistics.

### Aura Thresholds

| Threshold | Aura Tier |
|-----------|----------|
| 25 uses | Spark (small particles) |
| 75 uses | Glow (medium particles) |
| 150 uses | Radiance (larger particles) |
| 300 uses | Brilliance (full aura) |

### Aura Effects

Each gem has a unique particle color for its aura:
- **Astra**: Purple
- **Fire**: Orange-red
- **Water**: Blue
- **Air**: White-blue
- **Earth**: Brown
- **Life**: Green
- **Void**: Dark purple
- **Flux**: Cyan
- **Space**: Deep blue
- **Speed**: Yellow
- **Reaper**: Dark red
- **Spy**: Gray
- **Beacon**: Gold
- **Hunter**: Olive
- **Duelist**: Crimson
- **Trickster**: Pink
- **Wealth**: Gold
- **Sentinel**: Silver
- **Summoner**: Violet
- **Terror**: Dark crimson
- **Chaos**: Magenta
- **Pillager**: Dark gray
- **Prism**: White

### Configuration

```json
{
  "mastery": {
    "enabled": true,
    "showAuraParticles": true
  }
}
```

---

## Rivalry System

The Rivalry system assigns players random targets for bonus damage, creating dynamic PvP objectives.

### How It Works

1. **Target Assignment**: On spawn/respawn, you are assigned a random online player as your rival target
2. **Bonus Damage**: Attacks against your rival deal 25% bonus damage (configurable)
3. **HUD Display**: Your current rival is shown in your HUD (orange "Rival: PlayerName")
4. **Reroll on Kill**: When you kill your rival, a new target is automatically assigned
5. **Self-Targeting**: You cannot be assigned yourself as a target
6. **Trusted Players**: Trusted players CAN be assigned as rivals (the bonus persists)

### Configuration

```json
{
  "rivalry": {
    "enabled": true,
    "damageMultiplier": 1.25,
    "showInHud": true
  }
}
```

| Setting | Default | Description |
|---------|---------|-------------|
| `enabled` | `true` | Enable/disable the rivalry system |
| `damageMultiplier` | `1.25` | Damage multiplier against rival (1.25 = 25% bonus) |
| `showInHud` | `true` | Show rival target in the HUD |

### Notes

- The rivalry target persists until killed, then rerolls
- The same player can be selected again after a reroll
- With only two players online, your rival is always the other player
- If no other players are online, no rival is assigned
- Rival assignments are stored per-player and persist across sessions
