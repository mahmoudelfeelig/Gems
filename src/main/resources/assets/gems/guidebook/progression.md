# Progression (Energy, Hearts, Swapping)

This doc describes the core progression loop: energy, hearts, and gem swapping.

## Core loop

- You spawn with a random gem at 3 energy.
- Energy tiers: Broken (0), Common (1), Rare (2), Elite (3), Mythical (4), Legendary (5), Legendary +1..+5 (6-10).
- Passives are active at energy 1+ (unless you toggle them off in client config). At energy 0, passives and abilities are disabled.
- Abilities unlock by energy: 2-4 unlock in order, 5+ unlock all remaining abilities.
- Energy levels 6-10 are buffer only (no new powers).
- Player-vs-player kills grant +1 energy. Death (any cause) loses 1 energy.
- Legendary +5 gives a glint. If a Legendary +5 player kills a non-broken player, the victim drops an upgrade item.
- Upgrade items cannot be used by Legendary +5 players.

## Hearts, items, and swapping

- Heart items drop on death and increase max hearts when consumed.
- Minimum max hearts is configurable (default 5); maximum total hearts is 20.
- Heart items cannot be consumed by trusted players (prevents team boosting).
- Energy upgrade items add a level (up to Legendary +5).
- Gem Trader swaps only your active gem and consumes the item.
- Gem Purchase Token adds any gem to your owned set, activates it, and consumes the token.
- All gem and legendary recipes unlock automatically when you join.
- Players drop their heads on death.
- Test Dummy spawn egg: spawns a player-sized training dummy for item/ability testing.
