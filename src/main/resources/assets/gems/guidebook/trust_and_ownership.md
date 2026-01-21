# Trust and ownership

This doc describes the rules around trusting other players and owning gems.

## Trust

- Trust only affects ally-targeted effects (buffs, heals, shared cooldowns, etc.).
- PVP is always allowed regardless of trust status.
- Trust is managed via commands: `/gems trust`, `/gems untrust`, `/gems trustlist`.

## Gem ownership

### Owning multiple gems

- Gems are **bound to an owner** when first picked up or crafted.
- A player can own multiple gems, but only **one gem is active** at a time.
- Switching your active gem:
  - **Gem Trader**: swaps only the active gem.
  - **Gem Purchase Token**: adds a selected gem to your owned set and activates it.
- When you switch gems, any gem-specific state is cleared.

### When someone uses your gem

If another player activates a gem that belongs to you (ownership theft), severe penalties apply to **you** (the victim):

1. **Instant death** – you are killed immediately.
2. **No heart drop** – your heart does not drop for anyone to claim.
3. **Max hearts reduced by 2** – your maximum health permanently decreases.

This makes guarding your gem critical. Gems in item frames, chests, or dropped on the ground can still be stolen and activated by enemies.

## Item ownership tags and stacking

- Items of the same type can stack even if they have different ownership tags.
- Moving stacks between inventories preserves ownership without creating ownerless items.
