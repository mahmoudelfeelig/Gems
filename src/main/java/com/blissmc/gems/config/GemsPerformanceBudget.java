package com.blissmc.gems.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic safeguards that keep default balancing values within sane bounds.
 * This is not a runtime profiler; it's a "prevent accidental O(n^3) radii" check.
 */
public final class GemsPerformanceBudget {
    private GemsPerformanceBudget() {
    }

    public static List<String> validate(GemsBalance.Values v) {
        List<String> warnings = new ArrayList<>();

        if (!v.visual().enableParticles()) {
            warnings.add("Visual.enableParticles is disabled; ability VFX will be minimal.");
        }
        if (!v.visual().enableSounds()) {
            warnings.add("Visual.enableSounds is disabled; ability audio feedback will be minimal.");
        }
        if (v.visual().particleScalePercent() > 150) {
            warnings.add("Visual.particleScalePercent is high (" + v.visual().particleScalePercent() + "%); particles can become expensive on large servers.");
        }
        if (v.visual().maxParticlesPerCall() > 512) {
            warnings.add("Visual.maxParticlesPerCall is high (" + v.visual().maxParticlesPerCall() + "); large particle packets can be expensive.");
        }
        if (v.visual().maxRingPoints() > 512) {
            warnings.add("Visual.maxRingPoints is high (" + v.visual().maxRingPoints() + "); aura rings can become expensive.");
        }

        if (v.life().vitalityVortexScanRadiusBlocks() > 6) {
            warnings.add("Life.vitalityVortexScanRadiusBlocks is very high (" + v.life().vitalityVortexScanRadiusBlocks() + "); scanning is O(r^3) on activation.");
        }
        if (v.fire().meteorShowerCount() > 25) {
            warnings.add("Fire.meteorShowerCount is very high (" + v.fire().meteorShowerCount() + "); spawns many entities.");
        }
        if (v.fire().heatHazeRadiusBlocks() > 24) {
            warnings.add("Fire.heatHazeRadiusBlocks is very high (" + v.fire().heatHazeRadiusBlocks() + "); applies effects to many players per second.");
        }
        if (v.speed().speedStormRadiusBlocks() > 24) {
            warnings.add("Speed.speedStormRadiusBlocks is very high (" + v.speed().speedStormRadiusBlocks() + "); applies effects to many players per second.");
        }
        if (v.astra().dimensionalVoidRadiusBlocks() > 24) {
            warnings.add("Astra.dimensionalVoidRadiusBlocks is very high (" + v.astra().dimensionalVoidRadiusBlocks() + ").");
        }
        if (v.wealth().fumbleRadiusBlocks() > 24) {
            warnings.add("Wealth.fumbleRadiusBlocks is very high (" + v.wealth().fumbleRadiusBlocks() + ").");
        }

        return warnings;
    }
}
