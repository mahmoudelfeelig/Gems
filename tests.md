# Tests

Source of truth: specs live in checklist.md and abilities.md. This document maps existing and planned tests to those specs.

## Existing tests

- **GameTests**
  - GemsGameTests (src/gametest/java/com/feel/gems/gametest/GemsGameTests.java)
    - Astral camera returns to start
    - Flux beam consumes charge and resets to 0
      - Beacon aura applies only to trusted allies
      - Pillager volley stops when energy hits zero; discipline triggers under threshold
    - Death keeps active gem only (GemKeepOnDeath stash/restore)
    - Trader consumes item and keeps only new gem
      - Unlock order follows energy spec (per gem definitions)
      - Energy ladder gates abilities and loses 1 on death
      - Trader requires trader item and consumes exactly one
      - Spy steal records required observations even on cold cache
      - Summoner recall cleans tracked summons
      - Glint applies only to the active gem at energy cap and clears on switch/low energy
         - Hearts drop above the 5-heart floor and clamp; at the floor no drop and assassin conversion
         - Energy upgrade raises energy by one and caps; heart item respects max cap
         - Pockets inventory persists through save/load
    - (Many other ability- and item-focused cases inside this suite)

- **Unit/Integration (JUnit)**
  - GemsBalanceDefaultsTest (balance defaults match config)
  - GemsPerformanceBudgetTest (performance budget thresholds)
  - GemRegistryIntegrityTest (registry integrity)
  - GemEnergyTierTest (energy tier logic)
  - GemEnergyStateTest (energy state transitions)
   - GemEnergySpecTest (energy unlock spec compliance)
   - DataSpecRegressionTest (all gem definitions present with passives/abilities)
  - AssassinStateTest (assassin state logic)
  - HotbarLockTest (hotbar lock enforcement)
  - SummonerBudgetTest (summoner budget math)
   - TimeSourceConsistencyTest (forbids per-world time in power/net/state/trade/trust packages)
   - TelemetryParsingTest (perf snapshot regex guard)
   - GemItemGlintTest (glint flag toggles component)
   - HeartRecipeTest (expensive heart crafting recipe stays stable)
  - WealthFumbleTest (wealth fumble behavior)
  - MinecraftBootstrap (test bootstrap helper)

## Notes

- Always align expected values and unlock/energy rules with checklist.md and abilities.md (single source of truth).
- Prefer GameTests for integration/stateful flows; JUnit for pure logic/format guards.
- When adding tests, include brief comments referencing the relevant spec section/row for traceability.
