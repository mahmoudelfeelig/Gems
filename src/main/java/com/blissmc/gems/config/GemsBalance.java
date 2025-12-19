package com.blissmc.gems.config;

import com.blissmc.gems.GemsMod;

public final class GemsBalance {
    public static final int TICKS_PER_SECOND = 20;

    private static volatile Values VALUES = Values.defaults();

    private GemsBalance() {
    }

    public static Values v() {
        return VALUES;
    }

    public static void init() {
        GemsBalanceConfig cfg = GemsConfigManager.loadOrCreate();
        apply(cfg);
    }

    static void apply(GemsBalanceConfig cfg) {
        Values next = Values.from(cfg);
        var warnings = GemsPerformanceBudget.validate(next);
        for (String warning : warnings) {
            GemsMod.LOGGER.warn("[balance] {}", warning);
        }
        VALUES = next;
    }

    public record Values(
            Astra astra,
            Fire fire,
            Flux flux,
            Life life,
            Puff puff,
            Speed speed,
            Strength strength,
            Wealth wealth
    ) {
        public static Values defaults() {
            return from(new GemsBalanceConfig());
        }

        public static Values from(GemsBalanceConfig cfg) {
            return new Values(
                    Astra.from(cfg.astra),
                    Fire.from(cfg.fire),
                    Flux.from(cfg.flux),
                    Life.from(cfg.life),
                    Puff.from(cfg.puff),
                    Speed.from(cfg.speed),
                    Strength.from(cfg.strength),
                    Wealth.from(cfg.wealth)
            );
        }
    }

    public record Astra(
            int shadowAnchorWindowTicks,
            int dimensionalVoidCooldownTicks,
            int dimensionalVoidDurationTicks,
            int dimensionalVoidRadiusBlocks,
            int astralDaggersCooldownTicks,
            int astralDaggersCount,
            float astralDaggersDamage,
            float astralDaggersVelocity,
            float astralDaggersSpread,
            int unboundedCooldownTicks,
            int unboundedDurationTicks,
            int astralCameraCooldownTicks,
            int astralCameraDurationTicks,
            int spookCooldownTicks,
            int spookRadiusBlocks,
            int spookDurationTicks,
            int tagCooldownTicks,
            int tagRangeBlocks,
            int tagDurationTicks
    ) {
        static Astra from(GemsBalanceConfig.Astra cfg) {
            return new Astra(
                    secClamped(cfg.shadowAnchorWindowSeconds, 1, 60),
                    secClamped(cfg.dimensionalVoidCooldownSeconds, 0, 3600),
                    secClamped(cfg.dimensionalVoidDurationSeconds, 0, 60),
                    clampInt(cfg.dimensionalVoidRadiusBlocks, 0, 32),
                    secClamped(cfg.astralDaggersCooldownSeconds, 0, 3600),
                    clampInt(cfg.astralDaggersCount, 1, 30),
                    clampFloat(cfg.astralDaggersDamage, 0.0F, 40.0F),
                    clampFloat(cfg.astralDaggersVelocity, 0.1F, 8.0F),
                    clampFloat(cfg.astralDaggersSpread, 0.0F, 0.5F),
                    secClamped(cfg.unboundedCooldownSeconds, 0, 3600),
                    secClamped(cfg.unboundedDurationSeconds, 0, 10),
                    secClamped(cfg.astralCameraCooldownSeconds, 0, 3600),
                    secClamped(cfg.astralCameraDurationSeconds, 0, 60),
                    secClamped(cfg.spookCooldownSeconds, 0, 3600),
                    clampInt(cfg.spookRadiusBlocks, 0, 32),
                    secClamped(cfg.spookDurationSeconds, 0, 60),
                    secClamped(cfg.tagCooldownSeconds, 0, 3600),
                    clampInt(cfg.tagRangeBlocks, 0, 128),
                    secClamped(cfg.tagDurationSeconds, 0, 120)
            );
        }
    }

    public record Fire(
            int cosyCampfireCooldownTicks,
            int cosyCampfireDurationTicks,
            int cosyCampfireRadiusBlocks,
            int cosyCampfireRegenAmplifier,
            int heatHazeCooldownTicks,
            int heatHazeDurationTicks,
            int heatHazeRadiusBlocks,
            int heatHazeEnemyMiningFatigueAmplifier,
            int heatHazeEnemyWeaknessAmplifier,
            int fireballChargeUpTicks,
            int fireballChargeDownTicks,
            int fireballInternalCooldownTicks,
            int fireballMaxDistanceBlocks,
            int meteorShowerCooldownTicks,
            int meteorShowerCount,
            int meteorShowerSpreadBlocks,
            int meteorShowerHeightBlocks,
            float meteorShowerVelocity
    ) {
        static Fire from(GemsBalanceConfig.Fire cfg) {
            return new Fire(
                    secClamped(cfg.cosyCampfireCooldownSeconds, 0, 3600),
                    secClamped(cfg.cosyCampfireDurationSeconds, 0, 120),
                    clampInt(cfg.cosyCampfireRadiusBlocks, 0, 32),
                    clampInt(cfg.cosyCampfireRegenAmplifier, 0, 10),
                    secClamped(cfg.heatHazeCooldownSeconds, 0, 3600),
                    secClamped(cfg.heatHazeDurationSeconds, 0, 120),
                    clampInt(cfg.heatHazeRadiusBlocks, 0, 32),
                    clampInt(cfg.heatHazeEnemyMiningFatigueAmplifier, 0, 10),
                    clampInt(cfg.heatHazeEnemyWeaknessAmplifier, 0, 10),
                    secClamped(cfg.fireballChargeUpSeconds, 0, 20),
                    secClamped(cfg.fireballChargeDownSeconds, 0, 20),
                    secClamped(cfg.fireballInternalCooldownSeconds, 0, 3600),
                    clampInt(cfg.fireballMaxDistanceBlocks, 10, 256),
                    secClamped(cfg.meteorShowerCooldownSeconds, 0, 3600),
                    clampInt(cfg.meteorShowerCount, 0, 50),
                    clampInt(cfg.meteorShowerSpreadBlocks, 0, 48),
                    clampInt(cfg.meteorShowerHeightBlocks, 1, 256),
                    clampFloat(cfg.meteorShowerVelocity, 0.1F, 6.0F)
            );
        }
    }

    public record Flux(
            int fluxBeamCooldownTicks,
            int fluxBeamRangeBlocks,
            float fluxBeamMinDamage,
            float fluxBeamMaxDamageAt100,
            float fluxBeamMaxDamageAt200,
            int fluxBeamArmorDamageAt100,
            int fluxBeamArmorDamagePerPercent,
            int staticBurstCooldownTicks,
            int staticBurstRadiusBlocks,
            float staticBurstMaxDamage,
            int staticBurstStoreWindowTicks,
            int chargeDiamondBlock,
            int chargeGoldBlock,
            int chargeCopperBlock,
            int chargeEnchantedDiamondItem,
            int overchargeDelayTicks,
            int overchargePerSecond,
            float overchargeSelfDamagePerSecond
    ) {
        static Flux from(GemsBalanceConfig.Flux cfg) {
            return new Flux(
                    secClamped(cfg.fluxBeamCooldownSeconds, 0, 3600),
                    clampInt(cfg.fluxBeamRangeBlocks, 1, 256),
                    clampFloat(cfg.fluxBeamMinDamage, 0.0F, 40.0F),
                    clampFloat(cfg.fluxBeamMaxDamageAt100, 0.0F, 80.0F),
                    clampFloat(cfg.fluxBeamMaxDamageAt200, 0.0F, 120.0F),
                    clampInt(cfg.fluxBeamArmorDamageAt100, 0, 2000),
                    clampInt(cfg.fluxBeamArmorDamagePerPercent, 0, 100),
                    secClamped(cfg.staticBurstCooldownSeconds, 0, 3600),
                    clampInt(cfg.staticBurstRadiusBlocks, 0, 32),
                    clampFloat(cfg.staticBurstMaxDamage, 0.0F, 80.0F),
                    secClamped(cfg.staticBurstStoreWindowSeconds, 0, 600),
                    clampInt(cfg.chargeDiamondBlock, 0, 200),
                    clampInt(cfg.chargeGoldBlock, 0, 200),
                    clampInt(cfg.chargeCopperBlock, 0, 200),
                    clampInt(cfg.chargeEnchantedDiamondItem, 0, 200),
                    secClamped(cfg.overchargeDelaySeconds, 0, 60),
                    clampInt(cfg.overchargePerSecond, 0, 100),
                    clampFloat(cfg.overchargeSelfDamagePerSecond, 0.0F, 20.0F)
            );
        }
    }

    public record Life(
            int vitalityVortexCooldownTicks,
            int vitalityVortexRadiusBlocks,
            int vitalityVortexDurationTicks,
            int vitalityVortexScanRadiusBlocks,
            int vitalityVortexVerdantThreshold,
            float vitalityVortexAllyHeal,
            int healthDrainCooldownTicks,
            int healthDrainRangeBlocks,
            float healthDrainAmount,
            int lifeCircleCooldownTicks,
            int lifeCircleDurationTicks,
            int lifeCircleRadiusBlocks,
            double lifeCircleMaxHealthDelta,
            int heartLockCooldownTicks,
            int heartLockDurationTicks,
            int heartLockRangeBlocks
    ) {
        static Life from(GemsBalanceConfig.Life cfg) {
            return new Life(
                    secClamped(cfg.vitalityVortexCooldownSeconds, 0, 3600),
                    clampInt(cfg.vitalityVortexRadiusBlocks, 0, 32),
                    secClamped(cfg.vitalityVortexDurationSeconds, 0, 120),
                    clampInt(cfg.vitalityVortexScanRadiusBlocks, 1, 6),
                    clampInt(cfg.vitalityVortexVerdantThreshold, 0, 64),
                    clampFloat(cfg.vitalityVortexAllyHeal, 0.0F, 40.0F),
                    secClamped(cfg.healthDrainCooldownSeconds, 0, 3600),
                    clampInt(cfg.healthDrainRangeBlocks, 0, 128),
                    clampFloat(cfg.healthDrainAmount, 0.0F, 40.0F),
                    secClamped(cfg.lifeCircleCooldownSeconds, 0, 3600),
                    secClamped(cfg.lifeCircleDurationSeconds, 0, 120),
                    clampInt(cfg.lifeCircleRadiusBlocks, 0, 32),
                    clampDouble(cfg.lifeCircleMaxHealthDelta, 0.0D, 40.0D),
                    secClamped(cfg.heartLockCooldownSeconds, 0, 3600),
                    secClamped(cfg.heartLockDurationSeconds, 0, 60),
                    clampInt(cfg.heartLockRangeBlocks, 0, 128)
            );
        }
    }

    public record Puff(
            int doubleJumpCooldownTicks,
            double doubleJumpVelocityY,
            int dashCooldownTicks,
            double dashVelocity,
            float dashDamage,
            double dashHitRangeBlocks,
            int breezyBashCooldownTicks,
            int breezyBashRangeBlocks,
            double breezyBashUpVelocityY,
            double breezyBashKnockback,
            float breezyBashInitialDamage,
            float breezyBashImpactDamage,
            int breezyBashImpactWindowTicks,
            int groupBashCooldownTicks,
            int groupBashRadiusBlocks,
            double groupBashKnockback,
            double groupBashUpVelocityY
    ) {
        static Puff from(GemsBalanceConfig.Puff cfg) {
            return new Puff(
                    secClamped(cfg.doubleJumpCooldownSeconds, 0, 3600),
                    clampDouble(cfg.doubleJumpVelocityY, 0.0D, 3.0D),
                    secClamped(cfg.dashCooldownSeconds, 0, 3600),
                    clampDouble(cfg.dashVelocity, 0.0D, 5.0D),
                    clampFloat(cfg.dashDamage, 0.0F, 40.0F),
                    clampDouble(cfg.dashHitRangeBlocks, 0.5D, 16.0D),
                    secClamped(cfg.breezyBashCooldownSeconds, 0, 3600),
                    clampInt(cfg.breezyBashRangeBlocks, 0, 64),
                    clampDouble(cfg.breezyBashUpVelocityY, 0.0D, 5.0D),
                    clampDouble(cfg.breezyBashKnockback, 0.0D, 5.0D),
                    clampFloat(cfg.breezyBashInitialDamage, 0.0F, 40.0F),
                    clampFloat(cfg.breezyBashImpactDamage, 0.0F, 80.0F),
                    secClamped(cfg.breezyBashImpactWindowSeconds, 0, 60),
                    secClamped(cfg.groupBashCooldownSeconds, 0, 3600),
                    clampInt(cfg.groupBashRadiusBlocks, 0, 64),
                    clampDouble(cfg.groupBashKnockback, 0.0D, 5.0D),
                    clampDouble(cfg.groupBashUpVelocityY, 0.0D, 5.0D)
            );
        }
    }

    public record Speed(
            int arcShotCooldownTicks,
            int arcShotRangeBlocks,
            double arcShotRadiusBlocks,
            int arcShotMaxTargets,
            float arcShotDamage,
            int speedStormCooldownTicks,
            int speedStormDurationTicks,
            int speedStormRadiusBlocks,
            int speedStormAllySpeedAmplifier,
            int speedStormAllyHasteAmplifier,
            int speedStormEnemySlownessAmplifier,
            int speedStormEnemyMiningFatigueAmplifier,
            int terminalVelocityCooldownTicks,
            int terminalVelocityDurationTicks,
            int terminalVelocitySpeedAmplifier,
            int terminalVelocityHasteAmplifier
    ) {
        static Speed from(GemsBalanceConfig.Speed cfg) {
            return new Speed(
                    secClamped(cfg.arcShotCooldownSeconds, 0, 3600),
                    clampInt(cfg.arcShotRangeBlocks, 0, 256),
                    clampDouble(cfg.arcShotRadiusBlocks, 0.0D, 16.0D),
                    clampInt(cfg.arcShotMaxTargets, 1, 10),
                    clampFloat(cfg.arcShotDamage, 0.0F, 40.0F),
                    secClamped(cfg.speedStormCooldownSeconds, 0, 3600),
                    secClamped(cfg.speedStormDurationSeconds, 0, 120),
                    clampInt(cfg.speedStormRadiusBlocks, 0, 32),
                    clampInt(cfg.speedStormAllySpeedAmplifier, 0, 10),
                    clampInt(cfg.speedStormAllyHasteAmplifier, 0, 10),
                    clampInt(cfg.speedStormEnemySlownessAmplifier, 0, 10),
                    clampInt(cfg.speedStormEnemyMiningFatigueAmplifier, 0, 10),
                    secClamped(cfg.terminalVelocityCooldownSeconds, 0, 3600),
                    secClamped(cfg.terminalVelocityDurationSeconds, 0, 120),
                    clampInt(cfg.terminalVelocitySpeedAmplifier, 0, 10),
                    clampInt(cfg.terminalVelocityHasteAmplifier, 0, 10)
            );
        }
    }

    public record Strength(
            int nullifyCooldownTicks,
            int nullifyRadiusBlocks,
            int frailerCooldownTicks,
            int frailerRangeBlocks,
            int frailerDurationTicks,
            int bountyCooldownTicks,
            int bountyDurationTicks,
            int chadCooldownTicks,
            int chadDurationTicks,
            int chadEveryHits,
            float chadBonusDamage
    ) {
        static Strength from(GemsBalanceConfig.Strength cfg) {
            return new Strength(
                    secClamped(cfg.nullifyCooldownSeconds, 0, 3600),
                    clampInt(cfg.nullifyRadiusBlocks, 0, 32),
                    secClamped(cfg.frailerCooldownSeconds, 0, 3600),
                    clampInt(cfg.frailerRangeBlocks, 0, 128),
                    secClamped(cfg.frailerDurationSeconds, 0, 120),
                    secClamped(cfg.bountyCooldownSeconds, 0, 3600),
                    secClamped(cfg.bountyDurationSeconds, 0, 3600),
                    secClamped(cfg.chadCooldownSeconds, 0, 3600),
                    secClamped(cfg.chadDurationSeconds, 0, 3600),
                    clampInt(cfg.chadEveryHits, 1, 20),
                    clampFloat(cfg.chadBonusDamage, 0.0F, 80.0F)
            );
        }
    }

    public record Wealth(
            int fumbleCooldownTicks,
            int fumbleDurationTicks,
            int fumbleRadiusBlocks,
            int hotbarLockCooldownTicks,
            int hotbarLockDurationTicks,
            int hotbarLockRangeBlocks,
            int amplificationCooldownTicks,
            int amplificationDurationTicks,
            int richRushCooldownTicks,
            int richRushDurationTicks
    ) {
        static Wealth from(GemsBalanceConfig.Wealth cfg) {
            return new Wealth(
                    secClamped(cfg.fumbleCooldownSeconds, 0, 3600),
                    secClamped(cfg.fumbleDurationSeconds, 0, 120),
                    clampInt(cfg.fumbleRadiusBlocks, 0, 32),
                    secClamped(cfg.hotbarLockCooldownSeconds, 0, 3600),
                    secClamped(cfg.hotbarLockDurationSeconds, 0, 120),
                    clampInt(cfg.hotbarLockRangeBlocks, 0, 128),
                    secClamped(cfg.amplificationCooldownSeconds, 0, 3600),
                    secClamped(cfg.amplificationDurationSeconds, 0, 600),
                    secClamped(cfg.richRushCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.richRushDurationSeconds, 0, 24 * 3600)
            );
        }
    }

    private static int secClamped(int seconds, int minSeconds, int maxSeconds) {
        if (seconds <= 0) {
            return 0;
        }
        return sec(clampInt(seconds, minSeconds, maxSeconds));
    }

    private static int clampInt(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static float clampFloat(float value, float min, float max) {
        if (Float.isNaN(value)) {
            return min;
        }
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static double clampDouble(double value, double min, double max) {
        if (Double.isNaN(value)) {
            return min;
        }
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static int sec(int seconds) {
        if (seconds <= 0) {
            return 0;
        }
        return Math.multiplyExact(seconds, TICKS_PER_SECOND);
    }
}
