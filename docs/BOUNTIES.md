# Bounties

The bounty system lets players place heart/energy rewards on other players. Rewards are paid out when the target is killed.

## Placement Rules

- Only non-assassins can place bounties.
- Targets must be online when the bounty is placed.
- Bounty costs are deducted immediately.
- Heart bounties cannot drop you below the minimum max hearts (default 5).
- Energy bounties cannot drop you below 0 and are clamped to your current energy (up to max energy).
- You can raise an existing bounty on the same target only if you do not reduce hearts or energy. At least one value must increase.
- Multiple players can place bounties on the same target. Rewards stack.

## Claim Rules

- If any player kills the target, they receive all bounties on that target.
- If the target kills a placer, the target receives that placer's bounty.
- Heart rewards are given as Heart items. If inventory is full, they drop at your feet.
- Energy rewards are given as Energy Upgrade items.
- Bounty rewards are in addition to normal kill drops (the usual heart + energy upgrade).

## Assassin Void

- If a target becomes an assassin, all bounties on them are removed and a server notification is sent.

## Commands

- `/gems bounty` opens the Bounty Board screen.
- `/gems bounty place <player> <hearts> <energy>` places or raises a bounty.
- `/gems bounty list` shows all active bounties.
