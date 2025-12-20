package com.feel.gems.config;

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

    @Test
    void clampsExtremeValues() {
        GemsBalanceConfig cfg = new GemsBalanceConfig();
        cfg.fire.heatHazeRadiusBlocks = 999;
        cfg.life.vitalityVortexScanRadiusBlocks = 999;
        cfg.astra.astralDaggersDamage = -100.0F;
        cfg.fire.fireballMaxDistanceBlocks = 1;
        cfg.visual.particleScalePercent = 999;
        cfg.visual.maxParticlesPerCall = 99999;

        GemsBalance.Values v = GemsBalance.Values.from(cfg);
        assertEquals(32, v.fire().heatHazeRadiusBlocks());
        assertEquals(6, v.life().vitalityVortexScanRadiusBlocks());
        assertEquals(0.0F, v.astra().astralDaggersDamage());
        assertEquals(10, v.fire().fireballMaxDistanceBlocks());
        assertEquals(200, v.visual().particleScalePercent());
        assertEquals(2048, v.visual().maxParticlesPerCall());
    }

    @Test
    void missingConfigSectionsDoNotCrash() {
        GemsBalanceConfig cfg = new GemsBalanceConfig();
        cfg.visual = null;
        cfg.fire = null;

        GemsBalance.Values v = GemsBalance.Values.from(cfg);
        assertNotNull(v.visual());
        assertNotNull(v.fire());
    }
}
