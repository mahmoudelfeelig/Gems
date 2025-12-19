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
                    sec(cfg.shadowAnchorWindowSeconds),
                    sec(cfg.dimensionalVoidCooldownSeconds),
                    sec(cfg.dimensionalVoidDurationSeconds),
                    cfg.dimensionalVoidRadiusBlocks,
                    sec(cfg.astralDaggersCooldownSeconds),
                    cfg.astralDaggersCount,
                    cfg.astralDaggersDamage,
                    cfg.astralDaggersVelocity,
                    cfg.astralDaggersSpread,
                    sec(cfg.unboundedCooldownSeconds),
                    sec(cfg.unboundedDurationSeconds),
                    sec(cfg.astralCameraCooldownSeconds),
                    sec(cfg.astralCameraDurationSeconds),
                    sec(cfg.spookCooldownSeconds),
                    cfg.spookRadiusBlocks,
                    sec(cfg.spookDurationSeconds),
                    sec(cfg.tagCooldownSeconds),
                    cfg.tagRangeBlocks,
                    sec(cfg.tagDurationSeconds)
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
            int meteorShowerCooldownTicks,
            int meteorShowerCount,
            int meteorShowerSpreadBlocks,
            int meteorShowerHeightBlocks,
            float meteorShowerVelocity
    ) {
        static Fire from(GemsBalanceConfig.Fire cfg) {
            return new Fire(
                    sec(cfg.cosyCampfireCooldownSeconds),
                    sec(cfg.cosyCampfireDurationSeconds),
                    cfg.cosyCampfireRadiusBlocks,
                    cfg.cosyCampfireRegenAmplifier,
                    sec(cfg.heatHazeCooldownSeconds),
                    sec(cfg.heatHazeDurationSeconds),
                    cfg.heatHazeRadiusBlocks,
                    cfg.heatHazeEnemyMiningFatigueAmplifier,
                    cfg.heatHazeEnemyWeaknessAmplifier,
                    sec(cfg.fireballChargeUpSeconds),
                    sec(cfg.fireballChargeDownSeconds),
                    sec(cfg.fireballInternalCooldownSeconds),
                    sec(cfg.meteorShowerCooldownSeconds),
                    cfg.meteorShowerCount,
                    cfg.meteorShowerSpreadBlocks,
                    cfg.meteorShowerHeightBlocks,
                    cfg.meteorShowerVelocity
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
                    sec(cfg.fluxBeamCooldownSeconds),
                    cfg.fluxBeamRangeBlocks,
                    cfg.fluxBeamMinDamage,
                    cfg.fluxBeamMaxDamageAt100,
                    cfg.fluxBeamMaxDamageAt200,
                    cfg.fluxBeamArmorDamageAt100,
                    cfg.fluxBeamArmorDamagePerPercent,
                    sec(cfg.staticBurstCooldownSeconds),
                    cfg.staticBurstRadiusBlocks,
                    cfg.staticBurstMaxDamage,
                    sec(cfg.staticBurstStoreWindowSeconds),
                    cfg.chargeDiamondBlock,
                    cfg.chargeGoldBlock,
                    cfg.chargeCopperBlock,
                    cfg.chargeEnchantedDiamondItem,
                    sec(cfg.overchargeDelaySeconds),
                    cfg.overchargePerSecond,
                    cfg.overchargeSelfDamagePerSecond
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
                    sec(cfg.vitalityVortexCooldownSeconds),
                    cfg.vitalityVortexRadiusBlocks,
                    sec(cfg.vitalityVortexDurationSeconds),
                    cfg.vitalityVortexScanRadiusBlocks,
                    cfg.vitalityVortexVerdantThreshold,
                    cfg.vitalityVortexAllyHeal,
                    sec(cfg.healthDrainCooldownSeconds),
                    cfg.healthDrainRangeBlocks,
                    cfg.healthDrainAmount,
                    sec(cfg.lifeCircleCooldownSeconds),
                    sec(cfg.lifeCircleDurationSeconds),
                    cfg.lifeCircleRadiusBlocks,
                    cfg.lifeCircleMaxHealthDelta,
                    sec(cfg.heartLockCooldownSeconds),
                    sec(cfg.heartLockDurationSeconds),
                    cfg.heartLockRangeBlocks
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
                    sec(cfg.doubleJumpCooldownSeconds),
                    cfg.doubleJumpVelocityY,
                    sec(cfg.dashCooldownSeconds),
                    cfg.dashVelocity,
                    cfg.dashDamage,
                    cfg.dashHitRangeBlocks,
                    sec(cfg.breezyBashCooldownSeconds),
                    cfg.breezyBashRangeBlocks,
                    cfg.breezyBashUpVelocityY,
                    cfg.breezyBashKnockback,
                    cfg.breezyBashInitialDamage,
                    cfg.breezyBashImpactDamage,
                    sec(cfg.breezyBashImpactWindowSeconds),
                    sec(cfg.groupBashCooldownSeconds),
                    cfg.groupBashRadiusBlocks,
                    cfg.groupBashKnockback,
                    cfg.groupBashUpVelocityY
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
                    sec(cfg.arcShotCooldownSeconds),
                    cfg.arcShotRangeBlocks,
                    cfg.arcShotRadiusBlocks,
                    cfg.arcShotMaxTargets,
                    cfg.arcShotDamage,
                    sec(cfg.speedStormCooldownSeconds),
                    sec(cfg.speedStormDurationSeconds),
                    cfg.speedStormRadiusBlocks,
                    cfg.speedStormAllySpeedAmplifier,
                    cfg.speedStormAllyHasteAmplifier,
                    cfg.speedStormEnemySlownessAmplifier,
                    cfg.speedStormEnemyMiningFatigueAmplifier,
                    sec(cfg.terminalVelocityCooldownSeconds),
                    sec(cfg.terminalVelocityDurationSeconds),
                    cfg.terminalVelocitySpeedAmplifier,
                    cfg.terminalVelocityHasteAmplifier
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
                    sec(cfg.nullifyCooldownSeconds),
                    cfg.nullifyRadiusBlocks,
                    sec(cfg.frailerCooldownSeconds),
                    cfg.frailerRangeBlocks,
                    sec(cfg.frailerDurationSeconds),
                    sec(cfg.bountyCooldownSeconds),
                    sec(cfg.bountyDurationSeconds),
                    sec(cfg.chadCooldownSeconds),
                    sec(cfg.chadDurationSeconds),
                    cfg.chadEveryHits,
                    cfg.chadBonusDamage
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
                    sec(cfg.fumbleCooldownSeconds),
                    sec(cfg.fumbleDurationSeconds),
                    cfg.fumbleRadiusBlocks,
                    sec(cfg.hotbarLockCooldownSeconds),
                    sec(cfg.hotbarLockDurationSeconds),
                    cfg.hotbarLockRangeBlocks,
                    sec(cfg.amplificationCooldownSeconds),
                    sec(cfg.amplificationDurationSeconds),
                    sec(cfg.richRushCooldownSeconds),
                    sec(cfg.richRushDurationSeconds)
            );
        }
    }

    private static int sec(int seconds) {
        if (seconds <= 0) {
            return 0;
        }
        return Math.multiplyExact(seconds, TICKS_PER_SECOND);
    }
}

