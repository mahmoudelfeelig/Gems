# Tests

Source of truth: specs live in checklist.md and gameplay.md. This document maps existing and planned tests to those specs.

## Existing tests

- **GameTests**
  - Core (src/gametest/java/com/feel/gems/gametest/core/GemsCoreGameTests.java)
    - Death keeps active gem only (GemKeepOnDeath stash/restore)
    - Unlock order follows energy spec (per gem definitions)
    - Energy ladder gates abilities and loses 1 on death
    - Passives apply/stop when disables are toggled
    - Ability disables clear + cooldown snapshot persists
  - Trader (src/gametest/java/com/feel/gems/gametest/trade/GemsTradeGameTests.java)
    - Trader consumes item and keeps only new gem
    - Gem Trader fails without gem_trader item
    - Gem Trader requires gem_trader item and consumes exactly one
  - Flux (src/gametest/java/com/feel/gems/gametest/power/GemsFluxGameTests.java)
    - Flux beam consumes charge and resets to 0
    - Flux charge consumes exactly one item
  - Items (src/gametest/java/com/feel/gems/gametest/item/GemsItemGameTests.java)
    - Recipes registered
    - Glint applies only to the active gem at energy cap and clears on switch/low energy
    - Energy upgrade raises energy by one and caps
    - Heart item respects max cap
    - Pockets inventory persists through save/load
  - Hearts/Assassin (src/gametest/java/com/feel/gems/gametest/item/GemsHeartAndAssassinGameTests.java)
    - Assassin conversion and hearts applied
    - Hearts drop above the 5-heart floor and clamp; at the floor no drop and assassin conversion
  - Abilities (src/gametest/java/com/feel/gems/gametest/ability/GemsAbilityGameTests.java)
    - (All other end-to-end ability-focused cases)
  - Space (src/gametest/java/com/feel/gems/gametest/space/GemsSpaceMiningGameTests.java)
    - Orbital laser mining breaks normal blocks (regression for mining-mode filtering)
  - Air (src/gametest/java/com/feel/gems/gametest/air/GemsAirMaceGameTests.java)
    - Air mace does not respawn when dropped; re-grants after passive re-enabled
  - Terror (src/gametest/java/com/feel/gems/gametest/terror/GemsTerrorTradeGameTests.java)
    - Normal-player Terror Trade kills both, bypasses totems, and applies 2-heart penalty to target

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
   - TimeSourceConsistencyTest (forbids per-world time in power/net/state/trade/trust packages)
   - TelemetryParsingTest (perf snapshot regex guard)
   - GemItemGlintTest (glint flag toggles component)
   - HeartRecipeTest (expensive heart crafting recipe stays stable)
  - WealthFumbleTest (wealth fumble behavior)
  - MinecraftBootstrap (test bootstrap helper)

## Notes

- Always align expected values and unlock/energy rules with checklist.md and gameplay.md (single source of truth).
- Prefer GameTests for integration/stateful flows; JUnit for pure logic/format guards.
- When adding tests, include brief comments referencing the relevant spec section/row for traceability.
