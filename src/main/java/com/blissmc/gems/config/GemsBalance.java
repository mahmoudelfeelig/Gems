package com.feel.gems.config;

import com.feel.gems.GemsMod;

public final class GemsBalance {
    public static final int TICKS_PER_SECOND = 20;

    private static volatile Values VALUES = Values.defaults();

    private GemsBalance() {
    }

    public static Values v() {
        return VALUES;
    }

    public static void init() {
        GemsConfigManager.LoadResult load = GemsConfigManager.loadOrCreateWithFallback();
        apply(load.config());
    }

    static void apply(GemsBalanceConfig cfg) {
        Values next = Values.from(cfg);
        var warnings = GemsPerformanceBudget.validate(next);
        for (String warning : warnings) {
            GemsMod.LOGGER.warn("[balance] {}", warning);
        }
        VALUES = next;
    }

    public record ReloadResult(boolean applied, GemsConfigManager.LoadResult loadResult) {
    }

    public static ReloadResult reloadFromDisk() {
        GemsConfigManager.LoadResult load = GemsConfigManager.loadOrCreateStrict();
        if (load.status() == GemsConfigManager.LoadStatus.ERROR || load.config() == null) {
            return new ReloadResult(false, load);
        }
        apply(load.config());
        return new ReloadResult(true, load);
    }

    public static java.nio.file.Path dumpEffectiveBalance() {
        GemsBalanceConfig cfg = toConfig(VALUES);
        java.nio.file.Path out = GemsConfigManager.resolveInConfigDir("balance.effective.json");
        GemsConfigManager.write(out, cfg);
        return out;
    }

    private static GemsBalanceConfig toConfig(Values v) {
        GemsBalanceConfig cfg = new GemsBalanceConfig();

        cfg.visual.enableParticles = v.visual().enableParticles();
        cfg.visual.enableSounds = v.visual().enableSounds();
        cfg.visual.particleScalePercent = v.visual().particleScalePercent();
        cfg.visual.maxParticlesPerCall = v.visual().maxParticlesPerCall();
        cfg.visual.maxBeamSteps = v.visual().maxBeamSteps();
        cfg.visual.maxRingPoints = v.visual().maxRingPoints();

        cfg.astra.shadowAnchorWindowSeconds = ticksToSeconds(v.astra().shadowAnchorWindowTicks());
        cfg.astra.dimensionalVoidCooldownSeconds = ticksToSeconds(v.astra().dimensionalVoidCooldownTicks());
        cfg.astra.dimensionalVoidDurationSeconds = ticksToSeconds(v.astra().dimensionalVoidDurationTicks());
        cfg.astra.dimensionalVoidRadiusBlocks = v.astra().dimensionalVoidRadiusBlocks();
        cfg.astra.astralDaggersCooldownSeconds = ticksToSeconds(v.astra().astralDaggersCooldownTicks());
        cfg.astra.astralDaggersCount = v.astra().astralDaggersCount();
        cfg.astra.astralDaggersDamage = v.astra().astralDaggersDamage();
        cfg.astra.astralDaggersVelocity = v.astra().astralDaggersVelocity();
        cfg.astra.astralDaggersSpread = v.astra().astralDaggersSpread();
        cfg.astra.unboundedCooldownSeconds = ticksToSeconds(v.astra().unboundedCooldownTicks());
        cfg.astra.unboundedDurationSeconds = ticksToSeconds(v.astra().unboundedDurationTicks());
        cfg.astra.astralCameraCooldownSeconds = ticksToSeconds(v.astra().astralCameraCooldownTicks());
        cfg.astra.astralCameraDurationSeconds = ticksToSeconds(v.astra().astralCameraDurationTicks());
        cfg.astra.spookCooldownSeconds = ticksToSeconds(v.astra().spookCooldownTicks());
        cfg.astra.spookRadiusBlocks = v.astra().spookRadiusBlocks();
        cfg.astra.spookDurationSeconds = ticksToSeconds(v.astra().spookDurationTicks());
        cfg.astra.tagCooldownSeconds = ticksToSeconds(v.astra().tagCooldownTicks());
        cfg.astra.tagRangeBlocks = v.astra().tagRangeBlocks();
        cfg.astra.tagDurationSeconds = ticksToSeconds(v.astra().tagDurationTicks());

        cfg.fire.cosyCampfireCooldownSeconds = ticksToSeconds(v.fire().cosyCampfireCooldownTicks());
        cfg.fire.cosyCampfireDurationSeconds = ticksToSeconds(v.fire().cosyCampfireDurationTicks());
        cfg.fire.cosyCampfireRadiusBlocks = v.fire().cosyCampfireRadiusBlocks();
        cfg.fire.cosyCampfireRegenAmplifier = v.fire().cosyCampfireRegenAmplifier();
        cfg.fire.heatHazeCooldownSeconds = ticksToSeconds(v.fire().heatHazeCooldownTicks());
        cfg.fire.heatHazeDurationSeconds = ticksToSeconds(v.fire().heatHazeDurationTicks());
        cfg.fire.heatHazeRadiusBlocks = v.fire().heatHazeRadiusBlocks();
        cfg.fire.heatHazeEnemyMiningFatigueAmplifier = v.fire().heatHazeEnemyMiningFatigueAmplifier();
        cfg.fire.heatHazeEnemyWeaknessAmplifier = v.fire().heatHazeEnemyWeaknessAmplifier();
        cfg.fire.fireballChargeUpSeconds = ticksToSeconds(v.fire().fireballChargeUpTicks());
        cfg.fire.fireballChargeDownSeconds = ticksToSeconds(v.fire().fireballChargeDownTicks());
        cfg.fire.fireballInternalCooldownSeconds = ticksToSeconds(v.fire().fireballInternalCooldownTicks());
        cfg.fire.fireballMaxDistanceBlocks = v.fire().fireballMaxDistanceBlocks();
        cfg.fire.meteorShowerCooldownSeconds = ticksToSeconds(v.fire().meteorShowerCooldownTicks());
        cfg.fire.meteorShowerCount = v.fire().meteorShowerCount();
        cfg.fire.meteorShowerSpreadBlocks = v.fire().meteorShowerSpreadBlocks();
        cfg.fire.meteorShowerHeightBlocks = v.fire().meteorShowerHeightBlocks();
        cfg.fire.meteorShowerVelocity = v.fire().meteorShowerVelocity();

        cfg.flux.fluxBeamCooldownSeconds = ticksToSeconds(v.flux().fluxBeamCooldownTicks());
        cfg.flux.fluxBeamRangeBlocks = v.flux().fluxBeamRangeBlocks();
        cfg.flux.fluxBeamMinDamage = v.flux().fluxBeamMinDamage();
        cfg.flux.fluxBeamMaxDamageAt100 = v.flux().fluxBeamMaxDamageAt100();
        cfg.flux.fluxBeamMaxDamageAt200 = v.flux().fluxBeamMaxDamageAt200();
        cfg.flux.fluxBeamArmorDamageAt100 = v.flux().fluxBeamArmorDamageAt100();
        cfg.flux.fluxBeamArmorDamagePerPercent = v.flux().fluxBeamArmorDamagePerPercent();
        cfg.flux.staticBurstCooldownSeconds = ticksToSeconds(v.flux().staticBurstCooldownTicks());
        cfg.flux.staticBurstRadiusBlocks = v.flux().staticBurstRadiusBlocks();
        cfg.flux.staticBurstMaxDamage = v.flux().staticBurstMaxDamage();
        cfg.flux.staticBurstStoreWindowSeconds = ticksToSeconds(v.flux().staticBurstStoreWindowTicks());
        cfg.flux.chargeDiamondBlock = v.flux().chargeDiamondBlock();
        cfg.flux.chargeGoldBlock = v.flux().chargeGoldBlock();
        cfg.flux.chargeCopperBlock = v.flux().chargeCopperBlock();
        cfg.flux.chargeEnchantedDiamondItem = v.flux().chargeEnchantedDiamondItem();
        cfg.flux.overchargeDelaySeconds = ticksToSeconds(v.flux().overchargeDelayTicks());
        cfg.flux.overchargePerSecond = v.flux().overchargePerSecond();
        cfg.flux.overchargeSelfDamagePerSecond = v.flux().overchargeSelfDamagePerSecond();

        cfg.life.vitalityVortexCooldownSeconds = ticksToSeconds(v.life().vitalityVortexCooldownTicks());
        cfg.life.vitalityVortexRadiusBlocks = v.life().vitalityVortexRadiusBlocks();
        cfg.life.vitalityVortexDurationSeconds = ticksToSeconds(v.life().vitalityVortexDurationTicks());
        cfg.life.vitalityVortexScanRadiusBlocks = v.life().vitalityVortexScanRadiusBlocks();
        cfg.life.vitalityVortexVerdantThreshold = v.life().vitalityVortexVerdantThreshold();
        cfg.life.vitalityVortexAllyHeal = v.life().vitalityVortexAllyHeal();
        cfg.life.healthDrainCooldownSeconds = ticksToSeconds(v.life().healthDrainCooldownTicks());
        cfg.life.healthDrainRangeBlocks = v.life().healthDrainRangeBlocks();
        cfg.life.healthDrainAmount = v.life().healthDrainAmount();
        cfg.life.lifeCircleCooldownSeconds = ticksToSeconds(v.life().lifeCircleCooldownTicks());
        cfg.life.lifeCircleDurationSeconds = ticksToSeconds(v.life().lifeCircleDurationTicks());
        cfg.life.lifeCircleRadiusBlocks = v.life().lifeCircleRadiusBlocks();
        cfg.life.lifeCircleMaxHealthDelta = v.life().lifeCircleMaxHealthDelta();
        cfg.life.heartLockCooldownSeconds = ticksToSeconds(v.life().heartLockCooldownTicks());
        cfg.life.heartLockDurationSeconds = ticksToSeconds(v.life().heartLockDurationTicks());
        cfg.life.heartLockRangeBlocks = v.life().heartLockRangeBlocks();

        cfg.puff.doubleJumpCooldownSeconds = ticksToSeconds(v.puff().doubleJumpCooldownTicks());
        cfg.puff.doubleJumpVelocityY = v.puff().doubleJumpVelocityY();
        cfg.puff.dashCooldownSeconds = ticksToSeconds(v.puff().dashCooldownTicks());
        cfg.puff.dashVelocity = v.puff().dashVelocity();
        cfg.puff.dashDamage = v.puff().dashDamage();
        cfg.puff.dashHitRangeBlocks = v.puff().dashHitRangeBlocks();
        cfg.puff.breezyBashCooldownSeconds = ticksToSeconds(v.puff().breezyBashCooldownTicks());
        cfg.puff.breezyBashRangeBlocks = v.puff().breezyBashRangeBlocks();
        cfg.puff.breezyBashUpVelocityY = v.puff().breezyBashUpVelocityY();
        cfg.puff.breezyBashKnockback = v.puff().breezyBashKnockback();
        cfg.puff.breezyBashInitialDamage = v.puff().breezyBashInitialDamage();
        cfg.puff.breezyBashImpactDamage = v.puff().breezyBashImpactDamage();
        cfg.puff.breezyBashImpactWindowSeconds = ticksToSeconds(v.puff().breezyBashImpactWindowTicks());
        cfg.puff.groupBashCooldownSeconds = ticksToSeconds(v.puff().groupBashCooldownTicks());
        cfg.puff.groupBashRadiusBlocks = v.puff().groupBashRadiusBlocks();
        cfg.puff.groupBashKnockback = v.puff().groupBashKnockback();
        cfg.puff.groupBashUpVelocityY = v.puff().groupBashUpVelocityY();

        cfg.speed.arcShotCooldownSeconds = ticksToSeconds(v.speed().arcShotCooldownTicks());
        cfg.speed.arcShotRangeBlocks = v.speed().arcShotRangeBlocks();
        cfg.speed.arcShotRadiusBlocks = v.speed().arcShotRadiusBlocks();
        cfg.speed.arcShotMaxTargets = v.speed().arcShotMaxTargets();
        cfg.speed.arcShotDamage = v.speed().arcShotDamage();
        cfg.speed.speedStormCooldownSeconds = ticksToSeconds(v.speed().speedStormCooldownTicks());
        cfg.speed.speedStormDurationSeconds = ticksToSeconds(v.speed().speedStormDurationTicks());
        cfg.speed.speedStormRadiusBlocks = v.speed().speedStormRadiusBlocks();
        cfg.speed.speedStormAllySpeedAmplifier = v.speed().speedStormAllySpeedAmplifier();
        cfg.speed.speedStormAllyHasteAmplifier = v.speed().speedStormAllyHasteAmplifier();
        cfg.speed.speedStormEnemySlownessAmplifier = v.speed().speedStormEnemySlownessAmplifier();
        cfg.speed.speedStormEnemyMiningFatigueAmplifier = v.speed().speedStormEnemyMiningFatigueAmplifier();
        cfg.speed.terminalVelocityCooldownSeconds = ticksToSeconds(v.speed().terminalVelocityCooldownTicks());
        cfg.speed.terminalVelocityDurationSeconds = ticksToSeconds(v.speed().terminalVelocityDurationTicks());
        cfg.speed.terminalVelocitySpeedAmplifier = v.speed().terminalVelocitySpeedAmplifier();
        cfg.speed.terminalVelocityHasteAmplifier = v.speed().terminalVelocityHasteAmplifier();

        cfg.strength.nullifyCooldownSeconds = ticksToSeconds(v.strength().nullifyCooldownTicks());
        cfg.strength.nullifyRadiusBlocks = v.strength().nullifyRadiusBlocks();
        cfg.strength.frailerCooldownSeconds = ticksToSeconds(v.strength().frailerCooldownTicks());
        cfg.strength.frailerRangeBlocks = v.strength().frailerRangeBlocks();
        cfg.strength.frailerDurationSeconds = ticksToSeconds(v.strength().frailerDurationTicks());
        cfg.strength.bountyCooldownSeconds = ticksToSeconds(v.strength().bountyCooldownTicks());
        cfg.strength.bountyDurationSeconds = ticksToSeconds(v.strength().bountyDurationTicks());
        cfg.strength.chadCooldownSeconds = ticksToSeconds(v.strength().chadCooldownTicks());
        cfg.strength.chadDurationSeconds = ticksToSeconds(v.strength().chadDurationTicks());
        cfg.strength.chadEveryHits = v.strength().chadEveryHits();
        cfg.strength.chadBonusDamage = v.strength().chadBonusDamage();

        cfg.wealth.fumbleCooldownSeconds = ticksToSeconds(v.wealth().fumbleCooldownTicks());
        cfg.wealth.fumbleDurationSeconds = ticksToSeconds(v.wealth().fumbleDurationTicks());
        cfg.wealth.fumbleRadiusBlocks = v.wealth().fumbleRadiusBlocks();
        cfg.wealth.hotbarLockCooldownSeconds = ticksToSeconds(v.wealth().hotbarLockCooldownTicks());
        cfg.wealth.hotbarLockDurationSeconds = ticksToSeconds(v.wealth().hotbarLockDurationTicks());
        cfg.wealth.hotbarLockRangeBlocks = v.wealth().hotbarLockRangeBlocks();
        cfg.wealth.amplificationCooldownSeconds = ticksToSeconds(v.wealth().amplificationCooldownTicks());
        cfg.wealth.amplificationDurationSeconds = ticksToSeconds(v.wealth().amplificationDurationTicks());
        cfg.wealth.richRushCooldownSeconds = ticksToSeconds(v.wealth().richRushCooldownTicks());
        cfg.wealth.richRushDurationSeconds = ticksToSeconds(v.wealth().richRushDurationTicks());

        return cfg;
    }

    private static int ticksToSeconds(int ticks) {
        if (ticks <= 0) {
            return 0;
        }
        return (int) Math.round(ticks / (double) TICKS_PER_SECOND);
    }

    public record Values(
            Visual visual,
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
                    Visual.from(cfg.visual != null ? cfg.visual : new GemsBalanceConfig.Visual()),
                    Astra.from(cfg.astra != null ? cfg.astra : new GemsBalanceConfig.Astra()),
                    Fire.from(cfg.fire != null ? cfg.fire : new GemsBalanceConfig.Fire()),
                    Flux.from(cfg.flux != null ? cfg.flux : new GemsBalanceConfig.Flux()),
                    Life.from(cfg.life != null ? cfg.life : new GemsBalanceConfig.Life()),
                    Puff.from(cfg.puff != null ? cfg.puff : new GemsBalanceConfig.Puff()),
                    Speed.from(cfg.speed != null ? cfg.speed : new GemsBalanceConfig.Speed()),
                    Strength.from(cfg.strength != null ? cfg.strength : new GemsBalanceConfig.Strength()),
                    Wealth.from(cfg.wealth != null ? cfg.wealth : new GemsBalanceConfig.Wealth())
            );
        }
    }

    public record Visual(
            boolean enableParticles,
            boolean enableSounds,
            int particleScalePercent,
            int maxParticlesPerCall,
            int maxBeamSteps,
            int maxRingPoints
    ) {
        static Visual from(GemsBalanceConfig.Visual cfg) {
            return new Visual(
                    cfg.enableParticles,
                    cfg.enableSounds,
                    clampInt(cfg.particleScalePercent, 0, 200),
                    clampInt(cfg.maxParticlesPerCall, 0, 2048),
                    clampInt(cfg.maxBeamSteps, 0, 2048),
                    clampInt(cfg.maxRingPoints, 0, 2048)
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
