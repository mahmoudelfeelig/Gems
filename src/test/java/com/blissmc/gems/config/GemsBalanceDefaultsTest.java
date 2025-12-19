package com.blissmc.gems.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GemsBalanceDefaultsTest {
    @Test
    void defaultsMatchExpectedTicks() {
        GemsBalance.Values v = GemsBalance.Values.defaults();
        assertEquals(3 * 20, v.astra().unboundedDurationTicks());
        assertEquals(10 * 20, v.fire().heatHazeDurationTicks());
        assertEquals(9 * 60 * 20, v.wealth().richRushCooldownTicks());
        assertEquals(3 * 60 * 20, v.wealth().richRushDurationTicks());
    }

    @Test
    void defaultBudgetHasNoWarnings() {
        GemsBalance.Values v = GemsBalance.Values.defaults();
        assertTrue(GemsPerformanceBudget.validate(v).isEmpty());
    }
}

