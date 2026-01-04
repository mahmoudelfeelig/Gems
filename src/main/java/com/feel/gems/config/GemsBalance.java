package com.feel.gems.config;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Identifier;




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
        cfg.systems.minMaxHearts = v.systems().minMaxHearts();
        cfg.systems.assassinTriggerHearts = v.systems().assassinTriggerHearts();
        cfg.systems.assassinMaxHearts = v.systems().assassinMaxHearts();
        cfg.systems.assassinEliminationHeartsThreshold = v.systems().assassinEliminationHeartsThreshold();
        cfg.systems.assassinVsAssassinVictimHeartsLoss = v.systems().assassinVsAssassinVictimHeartsLoss();
        cfg.systems.assassinVsAssassinKillerHeartsGain = v.systems().assassinVsAssassinKillerHeartsGain();
        cfg.systems.controlledFollowStartBlocks = v.systems().controlledFollowStartBlocks();
        cfg.systems.controlledFollowStopBlocks = v.systems().controlledFollowStopBlocks();
        cfg.systems.controlledFollowSpeed = v.systems().controlledFollowSpeed();

        cfg.astra.shadowAnchorWindowSeconds = ticksToSeconds(v.astra().shadowAnchorWindowTicks());
        cfg.astra.shadowAnchorPostCooldownSeconds = ticksToSeconds(v.astra().shadowAnchorPostCooldownTicks());
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
        cfg.astra.soulHealingHearts = v.astra().soulHealingHearts();
        cfg.astra.soulReleaseForwardBlocks = v.astra().soulReleaseForwardBlocks();
        cfg.astra.soulReleaseUpBlocks = v.astra().soulReleaseUpBlocks();

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
        cfg.fire.meteorShowerTargetRangeBlocks = v.fire().meteorShowerTargetRangeBlocks();
        cfg.fire.meteorShowerCount = v.fire().meteorShowerCount();
        cfg.fire.meteorShowerSpreadBlocks = v.fire().meteorShowerSpreadBlocks();
        cfg.fire.meteorShowerHeightBlocks = v.fire().meteorShowerHeightBlocks();
        cfg.fire.meteorShowerVelocity = v.fire().meteorShowerVelocity();
        cfg.fire.meteorShowerExplosionPower = v.fire().meteorShowerExplosionPower();

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
        cfg.flux.chargeEmeraldBlock = v.flux().chargeEmeraldBlock();
        cfg.flux.chargeAmethystBlock = v.flux().chargeAmethystBlock();
        cfg.flux.chargeNetheriteScrap = v.flux().chargeNetheriteScrap();
        cfg.flux.chargeEnchantedDiamondItem = v.flux().chargeEnchantedDiamondItem();
        cfg.flux.overchargeDelaySeconds = ticksToSeconds(v.flux().overchargeDelayTicks());
        cfg.flux.overchargePerSecond = v.flux().overchargePerSecond();
        cfg.flux.overchargeSelfDamagePerSecond = v.flux().overchargeSelfDamagePerSecond();
        cfg.flux.fluxCapacitorChargeThreshold = v.flux().fluxCapacitorChargeThreshold();
        cfg.flux.fluxCapacitorAbsorptionAmplifier = v.flux().fluxCapacitorAbsorptionAmplifier();
        cfg.flux.fluxConductivityChargePerDamage = v.flux().fluxConductivityChargePerDamage();
        cfg.flux.fluxConductivityMaxChargePerHit = v.flux().fluxConductivityMaxChargePerHit();
        cfg.flux.fluxInsulationChargeThreshold = v.flux().fluxInsulationChargeThreshold();
        cfg.flux.fluxInsulationDamageMultiplier = v.flux().fluxInsulationDamageMultiplier();
        cfg.flux.fluxSurgeCooldownSeconds = ticksToSeconds(v.flux().fluxSurgeCooldownTicks());
        cfg.flux.fluxSurgeDurationSeconds = ticksToSeconds(v.flux().fluxSurgeDurationTicks());
        cfg.flux.fluxSurgeSpeedAmplifier = v.flux().fluxSurgeSpeedAmplifier();
        cfg.flux.fluxSurgeResistanceAmplifier = v.flux().fluxSurgeResistanceAmplifier();
        cfg.flux.fluxSurgeChargeCost = v.flux().fluxSurgeChargeCost();
        cfg.flux.fluxSurgeRadiusBlocks = v.flux().fluxSurgeRadiusBlocks();
        cfg.flux.fluxSurgeKnockback = v.flux().fluxSurgeKnockback();
        cfg.flux.fluxDischargeCooldownSeconds = ticksToSeconds(v.flux().fluxDischargeCooldownTicks());
        cfg.flux.fluxDischargeRadiusBlocks = v.flux().fluxDischargeRadiusBlocks();
        cfg.flux.fluxDischargeBaseDamage = v.flux().fluxDischargeBaseDamage();
        cfg.flux.fluxDischargeDamagePerCharge = v.flux().fluxDischargeDamagePerCharge();
        cfg.flux.fluxDischargeMaxDamage = v.flux().fluxDischargeMaxDamage();
        cfg.flux.fluxDischargeMinCharge = v.flux().fluxDischargeMinCharge();
        cfg.flux.fluxDischargeKnockback = v.flux().fluxDischargeKnockback();

        cfg.life.vitalityVortexCooldownSeconds = ticksToSeconds(v.life().vitalityVortexCooldownTicks());
        cfg.life.vitalityVortexRadiusBlocks = v.life().vitalityVortexRadiusBlocks();
        cfg.life.vitalityVortexDurationSeconds = ticksToSeconds(v.life().vitalityVortexDurationTicks());
        cfg.life.vitalityVortexScanRadiusBlocks = v.life().vitalityVortexScanRadiusBlocks();
        cfg.life.vitalityVortexVerdantThreshold = v.life().vitalityVortexVerdantThreshold();
        cfg.life.vitalityVortexAllyHeal = v.life().vitalityVortexAllyHeal();
        cfg.life.healthDrainCooldownSeconds = ticksToSeconds(v.life().healthDrainCooldownTicks());
        cfg.life.healthDrainRangeBlocks = v.life().healthDrainRangeBlocks();
        cfg.life.healthDrainAmount = v.life().healthDrainAmount();
        cfg.life.lifeSwapCooldownSeconds = ticksToSeconds(v.life().lifeSwapCooldownTicks());
        cfg.life.lifeSwapRangeBlocks = v.life().lifeSwapRangeBlocks();
        cfg.life.lifeSwapMinHearts = v.life().lifeSwapMinHearts();
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
        cfg.puff.gustCooldownSeconds = ticksToSeconds(v.puff().gustCooldownTicks());
        cfg.puff.gustRadiusBlocks = v.puff().gustRadiusBlocks();
        cfg.puff.gustUpVelocityY = v.puff().gustUpVelocityY();
        cfg.puff.gustKnockback = v.puff().gustKnockback();
        cfg.puff.gustSlownessDurationSeconds = ticksToSeconds(v.puff().gustSlownessDurationTicks());
        cfg.puff.gustSlownessAmplifier = v.puff().gustSlownessAmplifier();
        cfg.puff.gustSlowFallingDurationSeconds = ticksToSeconds(v.puff().gustSlowFallingDurationTicks());
        cfg.puff.windborneDurationSeconds = ticksToSeconds(v.puff().windborneDurationTicks());
        cfg.puff.windborneSlowFallingAmplifier = v.puff().windborneSlowFallingAmplifier();

        cfg.speed.momentumMinSpeed = v.speed().momentumMinSpeed();
        cfg.speed.momentumMaxSpeed = v.speed().momentumMaxSpeed();
        cfg.speed.momentumMinMultiplier = v.speed().momentumMinMultiplier();
        cfg.speed.momentumMaxMultiplier = v.speed().momentumMaxMultiplier();
        cfg.speed.frictionlessSpeedAmplifier = v.speed().frictionlessSpeedAmplifier();

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

        cfg.speed.slipstreamCooldownSeconds = ticksToSeconds(v.speed().slipstreamCooldownTicks());
        cfg.speed.slipstreamDurationSeconds = ticksToSeconds(v.speed().slipstreamDurationTicks());
        cfg.speed.slipstreamLengthBlocks = v.speed().slipstreamLengthBlocks();
        cfg.speed.slipstreamRadiusBlocks = v.speed().slipstreamRadiusBlocks();
        cfg.speed.slipstreamAllySpeedAmplifier = v.speed().slipstreamAllySpeedAmplifier();
        cfg.speed.slipstreamEnemySlownessAmplifier = v.speed().slipstreamEnemySlownessAmplifier();
        cfg.speed.slipstreamEnemyKnockback = v.speed().slipstreamEnemyKnockback();

        cfg.speed.afterimageCooldownSeconds = ticksToSeconds(v.speed().afterimageCooldownTicks());
        cfg.speed.afterimageDurationSeconds = ticksToSeconds(v.speed().afterimageDurationTicks());
        cfg.speed.afterimageSpeedAmplifier = v.speed().afterimageSpeedAmplifier();
        cfg.speed.tempoShiftCooldownSeconds = ticksToSeconds(v.speed().tempoShiftCooldownTicks());
        cfg.speed.tempoShiftDurationSeconds = ticksToSeconds(v.speed().tempoShiftDurationTicks());
        cfg.speed.tempoShiftRadiusBlocks = v.speed().tempoShiftRadiusBlocks();
        cfg.speed.tempoShiftAllyCooldownTicksPerSecond = v.speed().tempoShiftAllyCooldownTicksPerSecond();
        cfg.speed.tempoShiftEnemyCooldownTicksPerSecond = v.speed().tempoShiftEnemyCooldownTicksPerSecond();

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
        cfg.strength.adrenalineThresholdHearts = v.strength().adrenalineThresholdHearts();
        cfg.strength.adrenalineDurationSeconds = ticksToSeconds(v.strength().adrenalineDurationTicks());
        cfg.strength.adrenalineResistanceAmplifier = v.strength().adrenalineResistanceAmplifier();

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
        cfg.wealth.pocketsRows = v.wealth().pocketsRows();

        cfg.terror.dreadAuraRadiusBlocks = v.terror().dreadAuraRadiusBlocks();
        cfg.terror.dreadAuraAmplifier = v.terror().dreadAuraAmplifier();
        cfg.terror.bloodPriceDurationSeconds = ticksToSeconds(v.terror().bloodPriceDurationTicks());
        cfg.terror.bloodPriceStrengthAmplifier = v.terror().bloodPriceStrengthAmplifier();
        cfg.terror.bloodPriceResistanceAmplifier = v.terror().bloodPriceResistanceAmplifier();
        cfg.terror.terrorTradeCooldownSeconds = ticksToSeconds(v.terror().terrorTradeCooldownTicks());
        cfg.terror.terrorTradeRangeBlocks = v.terror().terrorTradeRangeBlocks();
        cfg.terror.terrorTradeMaxUses = v.terror().terrorTradeMaxUses();
        cfg.terror.terrorTradeHeartsCost = v.terror().terrorTradeHeartsCost();
        cfg.terror.terrorTradePermanentEnergyPenalty = v.terror().terrorTradePermanentEnergyPenalty();
        cfg.terror.terrorTradeNormalTargetHeartsPenalty = v.terror().terrorTradeNormalTargetHeartsPenalty();
        cfg.terror.terrorTradeNormalTargetEnergyPenalty = v.terror().terrorTradeNormalTargetEnergyPenalty();
        cfg.terror.panicRingCooldownSeconds = ticksToSeconds(v.terror().panicRingCooldownTicks());
        cfg.terror.panicRingTntCount = v.terror().panicRingTntCount();
        cfg.terror.panicRingFuseTicks = v.terror().panicRingFuseTicks();
        cfg.terror.panicRingRadiusBlocks = v.terror().panicRingRadiusBlocks();
        cfg.terror.rigCooldownSeconds = ticksToSeconds(v.terror().rigCooldownTicks());
        cfg.terror.rigRangeBlocks = v.terror().rigRangeBlocks();
        cfg.terror.rigDurationSeconds = ticksToSeconds(v.terror().rigDurationTicks());
        cfg.terror.rigFuseTicks = v.terror().rigFuseTicks();
        cfg.terror.rigTntCount = v.terror().rigTntCount();
        cfg.terror.remoteChargeArmWindowSeconds = ticksToSeconds(v.terror().remoteChargeArmWindowTicks());
        cfg.terror.remoteChargeDetonateWindowSeconds = ticksToSeconds(v.terror().remoteChargeDetonateWindowTicks());
        cfg.terror.remoteChargeFuseTicks = v.terror().remoteChargeFuseTicks();
        cfg.terror.remoteChargeCooldownSeconds = ticksToSeconds(v.terror().remoteChargeCooldownTicks());
        cfg.terror.breachChargeCooldownSeconds = ticksToSeconds(v.terror().breachChargeCooldownTicks());
        cfg.terror.breachChargeRangeBlocks = v.terror().breachChargeRangeBlocks();
        cfg.terror.breachChargeFuseTicks = v.terror().breachChargeFuseTicks();
        cfg.terror.breachChargeExplosionPower = v.terror().breachChargeExplosionPower();

        cfg.summoner.maxPoints = v.summoner().maxPoints();
        cfg.summoner.maxActiveSummons = v.summoner().maxActiveSummons();
        cfg.summoner.summonLifetimeSeconds = ticksToSeconds(v.summoner().summonLifetimeTicks());
        cfg.summoner.commandRangeBlocks = v.summoner().commandRangeBlocks();
        cfg.summoner.summonSlotCooldownSeconds = ticksToSeconds(v.summoner().summonSlotCooldownTicks());
        cfg.summoner.recallCooldownSeconds = ticksToSeconds(v.summoner().recallCooldownTicks());
        cfg.summoner.commandersMarkDurationSeconds = ticksToSeconds(v.summoner().commandersMarkDurationTicks());
        cfg.summoner.commandersMarkStrengthAmplifier = v.summoner().commandersMarkStrengthAmplifier();
        cfg.summoner.summonBonusHealth = v.summoner().summonBonusHealth();
        cfg.summoner.summonSpawnForwardBlocks = v.summoner().summonSpawnForwardBlocks();
        cfg.summoner.summonSpawnUpBlocks = v.summoner().summonSpawnUpBlocks();
        cfg.summoner.summonSpawnRingBaseBlocks = v.summoner().summonSpawnRingBaseBlocks();
        cfg.summoner.summonSpawnRingStepBlocks = v.summoner().summonSpawnRingStepBlocks();
        cfg.summoner.summonSpawnRingLayers = v.summoner().summonSpawnRingLayers();
        cfg.summoner.summonSpawnRingSegments = v.summoner().summonSpawnRingSegments();
        cfg.summoner.costs = v.summoner().costs();
        cfg.summoner.slot1 = v.summoner().slot1();
        cfg.summoner.slot2 = v.summoner().slot2();
        cfg.summoner.slot3 = v.summoner().slot3();
        cfg.summoner.slot4 = v.summoner().slot4();
        cfg.summoner.slot5 = v.summoner().slot5();

        cfg.space.lunarMinMultiplier = v.space().lunarMinMultiplier();
        cfg.space.lunarMaxMultiplier = v.space().lunarMaxMultiplier();
        cfg.space.starshieldProjectileDamageMultiplier = v.space().starshieldProjectileDamageMultiplier();
        cfg.space.orbitalLaserCooldownSeconds = ticksToSeconds(v.space().orbitalLaserCooldownTicks());
        cfg.space.orbitalLaserRangeBlocks = v.space().orbitalLaserRangeBlocks();
        cfg.space.orbitalLaserDelaySeconds = ticksToSeconds(v.space().orbitalLaserDelayTicks());
        cfg.space.orbitalLaserRadiusBlocks = v.space().orbitalLaserRadiusBlocks();
        cfg.space.orbitalLaserDamage = v.space().orbitalLaserDamage();
        cfg.space.orbitalLaserMiningRadiusBlocks = v.space().orbitalLaserMiningRadiusBlocks();
        cfg.space.orbitalLaserMiningHardnessCap = v.space().orbitalLaserMiningHardnessCap();
        cfg.space.orbitalLaserMiningMaxBlocks = v.space().orbitalLaserMiningMaxBlocks();
        cfg.space.gravityFieldCooldownSeconds = ticksToSeconds(v.space().gravityFieldCooldownTicks());
        cfg.space.gravityFieldDurationSeconds = ticksToSeconds(v.space().gravityFieldDurationTicks());
        cfg.space.gravityFieldRadiusBlocks = v.space().gravityFieldRadiusBlocks();
        cfg.space.gravityFieldAllyGravityMultiplier = v.space().gravityFieldAllyGravityMultiplier();
        cfg.space.gravityFieldEnemyGravityMultiplier = v.space().gravityFieldEnemyGravityMultiplier();
        cfg.space.blackHoleCooldownSeconds = ticksToSeconds(v.space().blackHoleCooldownTicks());
        cfg.space.blackHoleDurationSeconds = ticksToSeconds(v.space().blackHoleDurationTicks());
        cfg.space.blackHoleRadiusBlocks = v.space().blackHoleRadiusBlocks();
        cfg.space.blackHolePullStrength = v.space().blackHolePullStrength();
        cfg.space.blackHoleDamagePerSecond = v.space().blackHoleDamagePerSecond();
        cfg.space.whiteHoleCooldownSeconds = ticksToSeconds(v.space().whiteHoleCooldownTicks());
        cfg.space.whiteHoleDurationSeconds = ticksToSeconds(v.space().whiteHoleDurationTicks());
        cfg.space.whiteHoleRadiusBlocks = v.space().whiteHoleRadiusBlocks();
        cfg.space.whiteHolePushStrength = v.space().whiteHolePushStrength();
        cfg.space.whiteHoleDamagePerSecond = v.space().whiteHoleDamagePerSecond();

        cfg.reaper.undeadWardDamageMultiplier = v.reaper().undeadWardDamageMultiplier();
        cfg.reaper.harvestRegenDurationSeconds = ticksToSeconds(v.reaper().harvestRegenDurationTicks());
        cfg.reaper.harvestRegenAmplifier = v.reaper().harvestRegenAmplifier();
        cfg.reaper.graveSteedCooldownSeconds = ticksToSeconds(v.reaper().graveSteedCooldownTicks());
        cfg.reaper.graveSteedDurationSeconds = ticksToSeconds(v.reaper().graveSteedDurationTicks());
        cfg.reaper.graveSteedDecayDamagePerSecond = v.reaper().graveSteedDecayDamagePerSecond();
        cfg.reaper.witheringStrikesCooldownSeconds = ticksToSeconds(v.reaper().witheringStrikesCooldownTicks());
        cfg.reaper.witheringStrikesDurationSeconds = ticksToSeconds(v.reaper().witheringStrikesDurationTicks());
        cfg.reaper.witheringStrikesWitherDurationSeconds = ticksToSeconds(v.reaper().witheringStrikesWitherDurationTicks());
        cfg.reaper.witheringStrikesWitherAmplifier = v.reaper().witheringStrikesWitherAmplifier();
        cfg.reaper.deathOathCooldownSeconds = ticksToSeconds(v.reaper().deathOathCooldownTicks());
        cfg.reaper.deathOathDurationSeconds = ticksToSeconds(v.reaper().deathOathDurationTicks());
        cfg.reaper.deathOathRangeBlocks = v.reaper().deathOathRangeBlocks();
        cfg.reaper.deathOathSelfDamagePerSecond = v.reaper().deathOathSelfDamagePerSecond();
        cfg.reaper.deathOathBonusDamage = v.reaper().deathOathBonusDamage();
        cfg.reaper.retributionCooldownSeconds = ticksToSeconds(v.reaper().retributionCooldownTicks());
        cfg.reaper.retributionDurationSeconds = ticksToSeconds(v.reaper().retributionDurationTicks());
        cfg.reaper.retributionDamageMultiplier = v.reaper().retributionDamageMultiplier();
        cfg.reaper.scytheSweepCooldownSeconds = ticksToSeconds(v.reaper().scytheSweepCooldownTicks());
        cfg.reaper.scytheSweepRangeBlocks = v.reaper().scytheSweepRangeBlocks();
        cfg.reaper.scytheSweepArcDegrees = v.reaper().scytheSweepArcDegrees();
        cfg.reaper.scytheSweepDamage = v.reaper().scytheSweepDamage();
        cfg.reaper.scytheSweepKnockback = v.reaper().scytheSweepKnockback();
        cfg.reaper.bloodChargeCooldownSeconds = ticksToSeconds(v.reaper().bloodChargeCooldownTicks());
        cfg.reaper.bloodChargeMaxChargeSeconds = ticksToSeconds(v.reaper().bloodChargeMaxChargeTicks());
        cfg.reaper.bloodChargeSelfDamagePerSecond = v.reaper().bloodChargeSelfDamagePerSecond();
        cfg.reaper.bloodChargeMaxMultiplier = v.reaper().bloodChargeMaxMultiplier();
        cfg.reaper.bloodChargeBuffDurationSeconds = ticksToSeconds(v.reaper().bloodChargeBuffDurationTicks());
        cfg.reaper.shadowCloneCooldownSeconds = ticksToSeconds(v.reaper().shadowCloneCooldownTicks());
        cfg.reaper.shadowCloneDurationSeconds = ticksToSeconds(v.reaper().shadowCloneDurationTicks());
        cfg.reaper.shadowCloneMaxHealth = v.reaper().shadowCloneMaxHealth();
        cfg.reaper.shadowCloneCount = v.reaper().shadowCloneCount();
        cfg.reaper.shadowCloneEntityId = v.reaper().shadowCloneEntityId();

        cfg.pillager.raidersTrainingProjectileVelocityMultiplier = v.pillager().raidersTrainingProjectileVelocityMultiplier();
        cfg.pillager.shieldbreakerDisableCooldownTicks = v.pillager().shieldbreakerDisableCooldownTicks();
        cfg.pillager.illagerDisciplineThresholdHearts = v.pillager().illagerDisciplineThresholdHearts();
        cfg.pillager.illagerDisciplineResistanceDurationSeconds = ticksToSeconds(v.pillager().illagerDisciplineResistanceDurationTicks());
        cfg.pillager.illagerDisciplineResistanceAmplifier = v.pillager().illagerDisciplineResistanceAmplifier();
        cfg.pillager.illagerDisciplineCooldownSeconds = ticksToSeconds(v.pillager().illagerDisciplineCooldownTicks());
        cfg.pillager.fangsCooldownSeconds = ticksToSeconds(v.pillager().fangsCooldownTicks());
        cfg.pillager.fangsRangeBlocks = v.pillager().fangsRangeBlocks();
        cfg.pillager.fangsCount = v.pillager().fangsCount();
        cfg.pillager.fangsSpacingBlocks = v.pillager().fangsSpacingBlocks();
        cfg.pillager.fangsWarmupStepTicks = v.pillager().fangsWarmupStepTicks();
        cfg.pillager.ravageCooldownSeconds = ticksToSeconds(v.pillager().ravageCooldownTicks());
        cfg.pillager.ravageRangeBlocks = v.pillager().ravageRangeBlocks();
        cfg.pillager.ravageDamage = v.pillager().ravageDamage();
        cfg.pillager.ravageKnockback = v.pillager().ravageKnockback();
        cfg.pillager.vindicatorBreakCooldownSeconds = ticksToSeconds(v.pillager().vindicatorBreakCooldownTicks());
        cfg.pillager.vindicatorBreakDurationSeconds = ticksToSeconds(v.pillager().vindicatorBreakDurationTicks());
        cfg.pillager.vindicatorBreakStrengthAmplifier = v.pillager().vindicatorBreakStrengthAmplifier();
        cfg.pillager.vindicatorBreakShieldDisableCooldownTicks = v.pillager().vindicatorBreakShieldDisableCooldownTicks();
        cfg.pillager.volleyCooldownSeconds = ticksToSeconds(v.pillager().volleyCooldownTicks());
        cfg.pillager.volleyDurationSeconds = ticksToSeconds(v.pillager().volleyDurationTicks());
        cfg.pillager.volleyPeriodTicks = v.pillager().volleyPeriodTicks();
        cfg.pillager.volleyArrowsPerShot = v.pillager().volleyArrowsPerShot();
        cfg.pillager.volleyArrowDamage = v.pillager().volleyArrowDamage();
        cfg.pillager.volleyArrowVelocity = v.pillager().volleyArrowVelocity();
        cfg.pillager.volleyArrowInaccuracy = v.pillager().volleyArrowInaccuracy();
        cfg.pillager.warhornCooldownSeconds = ticksToSeconds(v.pillager().warhornCooldownTicks());
        cfg.pillager.warhornRadiusBlocks = v.pillager().warhornRadiusBlocks();
        cfg.pillager.warhornDurationSeconds = ticksToSeconds(v.pillager().warhornDurationTicks());
        cfg.pillager.warhornAllySpeedAmplifier = v.pillager().warhornAllySpeedAmplifier();
        cfg.pillager.warhornAllyResistanceAmplifier = v.pillager().warhornAllyResistanceAmplifier();
        cfg.pillager.warhornEnemySlownessAmplifier = v.pillager().warhornEnemySlownessAmplifier();
        cfg.pillager.warhornEnemyWeaknessAmplifier = v.pillager().warhornEnemyWeaknessAmplifier();
        cfg.pillager.snareCooldownSeconds = ticksToSeconds(v.pillager().snareCooldownTicks());
        cfg.pillager.snareRangeBlocks = v.pillager().snareRangeBlocks();
        cfg.pillager.snareDurationSeconds = ticksToSeconds(v.pillager().snareDurationTicks());
        cfg.pillager.snareSlownessAmplifier = v.pillager().snareSlownessAmplifier();

        cfg.spyMimic.stillnessSeconds = ticksToSeconds(v.spyMimic().stillnessTicks());
        cfg.spyMimic.stillnessMoveEpsilonBlocks = v.spyMimic().stillnessMoveEpsilonBlocks();
        cfg.spyMimic.stillnessInvisRefreshSeconds = ticksToSeconds(v.spyMimic().stillnessInvisRefreshTicks());
        cfg.spyMimic.backstepCooldownSeconds = ticksToSeconds(v.spyMimic().backstepCooldownTicks());
        cfg.spyMimic.backstepVelocity = v.spyMimic().backstepVelocity();
        cfg.spyMimic.backstepUpVelocity = v.spyMimic().backstepUpVelocity();
        cfg.spyMimic.backstabBonusDamage = v.spyMimic().backstabBonusDamage();
        cfg.spyMimic.backstabAngleDegrees = v.spyMimic().backstabAngleDegrees();
        cfg.spyMimic.observeRangeBlocks = v.spyMimic().observeRangeBlocks();
        cfg.spyMimic.observeWindowSeconds = ticksToSeconds(v.spyMimic().observeWindowTicks());
        cfg.spyMimic.stealRequiredWitnessCount = v.spyMimic().stealRequiredWitnessCount();
        cfg.spyMimic.maxStolenAbilities = v.spyMimic().maxStolenAbilities();
        cfg.spyMimic.mimicFormCooldownSeconds = ticksToSeconds(v.spyMimic().mimicFormCooldownTicks());
        cfg.spyMimic.mimicFormDurationSeconds = ticksToSeconds(v.spyMimic().mimicFormDurationTicks());
        cfg.spyMimic.mimicFormBonusMaxHealth = v.spyMimic().mimicFormBonusMaxHealth();
        cfg.spyMimic.mimicFormSpeedMultiplier = v.spyMimic().mimicFormSpeedMultiplier();
        cfg.spyMimic.echoCooldownSeconds = ticksToSeconds(v.spyMimic().echoCooldownTicks());
        cfg.spyMimic.echoWindowSeconds = ticksToSeconds(v.spyMimic().echoWindowTicks());
        cfg.spyMimic.stealCooldownSeconds = ticksToSeconds(v.spyMimic().stealCooldownTicks());
        cfg.spyMimic.smokeBombCooldownSeconds = ticksToSeconds(v.spyMimic().smokeBombCooldownTicks());
        cfg.spyMimic.smokeBombRadiusBlocks = v.spyMimic().smokeBombRadiusBlocks();
        cfg.spyMimic.smokeBombDurationSeconds = ticksToSeconds(v.spyMimic().smokeBombDurationTicks());
        cfg.spyMimic.smokeBombBlindnessAmplifier = v.spyMimic().smokeBombBlindnessAmplifier();
        cfg.spyMimic.smokeBombSlownessAmplifier = v.spyMimic().smokeBombSlownessAmplifier();
        cfg.spyMimic.stolenCastCooldownSeconds = ticksToSeconds(v.spyMimic().stolenCastCooldownTicks());
        cfg.spyMimic.skinshiftCooldownSeconds = ticksToSeconds(v.spyMimic().skinshiftCooldownTicks());
        cfg.spyMimic.skinshiftDurationSeconds = ticksToSeconds(v.spyMimic().skinshiftDurationTicks());
        cfg.spyMimic.skinshiftRangeBlocks = v.spyMimic().skinshiftRangeBlocks();

        cfg.beacon.coreRadiusBlocks = v.beacon().coreRadiusBlocks();
        cfg.beacon.corePulsePeriodSeconds = ticksToSeconds(v.beacon().corePulsePeriodTicks());
        cfg.beacon.coreRegenDurationSeconds = ticksToSeconds(v.beacon().coreRegenDurationTicks());
        cfg.beacon.coreRegenAmplifier = v.beacon().coreRegenAmplifier();
        cfg.beacon.stabilizeRadiusBlocks = v.beacon().stabilizeRadiusBlocks();
        cfg.beacon.stabilizeReduceTicksPerSecond = v.beacon().stabilizeReduceTicksPerSecond();
        cfg.beacon.rallyRadiusBlocks = v.beacon().rallyRadiusBlocks();
        cfg.beacon.rallyAbsorptionHearts = v.beacon().rallyAbsorptionHearts();
        cfg.beacon.rallyDurationSeconds = ticksToSeconds(v.beacon().rallyDurationTicks());
        cfg.beacon.auraCooldownSeconds = ticksToSeconds(v.beacon().auraCooldownTicks());
        cfg.beacon.auraDurationSeconds = ticksToSeconds(v.beacon().auraDurationTicks());
        cfg.beacon.auraRadiusBlocks = v.beacon().auraRadiusBlocks();
        cfg.beacon.auraRefreshSeconds = ticksToSeconds(v.beacon().auraRefreshTicks());
        cfg.beacon.auraSpeedAmplifier = v.beacon().auraSpeedAmplifier();
        cfg.beacon.auraHasteAmplifier = v.beacon().auraHasteAmplifier();
        cfg.beacon.auraResistanceAmplifier = v.beacon().auraResistanceAmplifier();
        cfg.beacon.auraJumpAmplifier = v.beacon().auraJumpAmplifier();
        cfg.beacon.auraStrengthAmplifier = v.beacon().auraStrengthAmplifier();
        cfg.beacon.auraRegenAmplifier = v.beacon().auraRegenAmplifier();

        cfg.air.aerialGuardFallDamageMultiplier = v.air().aerialGuardFallDamageMultiplier();
        cfg.air.aerialGuardDamageMultiplier = v.air().aerialGuardDamageMultiplier();
        cfg.air.aerialGuardKnockbackMultiplier = v.air().aerialGuardKnockbackMultiplier();
        cfg.air.windShearKnockback = v.air().windShearKnockback();
        cfg.air.windShearSlownessDurationSeconds = ticksToSeconds(v.air().windShearSlownessDurationTicks());
        cfg.air.windShearSlownessAmplifier = v.air().windShearSlownessAmplifier();
        cfg.air.windJumpCooldownSeconds = ticksToSeconds(v.air().windJumpCooldownTicks());
        cfg.air.windJumpVerticalVelocity = v.air().windJumpVerticalVelocity();
        cfg.air.windJumpForwardVelocity = v.air().windJumpForwardVelocity();
        cfg.air.galeSlamCooldownSeconds = ticksToSeconds(v.air().galeSlamCooldownTicks());
        cfg.air.galeSlamWindowSeconds = ticksToSeconds(v.air().galeSlamWindowTicks());
        cfg.air.galeSlamRadiusBlocks = v.air().galeSlamRadiusBlocks();
        cfg.air.galeSlamBonusDamage = v.air().galeSlamBonusDamage();
        cfg.air.galeSlamKnockback = v.air().galeSlamKnockback();
        cfg.air.crosswindCooldownSeconds = ticksToSeconds(v.air().crosswindCooldownTicks());
        cfg.air.crosswindRangeBlocks = v.air().crosswindRangeBlocks();
        cfg.air.crosswindRadiusBlocks = v.air().crosswindRadiusBlocks();
        cfg.air.crosswindDamage = v.air().crosswindDamage();
        cfg.air.crosswindKnockback = v.air().crosswindKnockback();
        cfg.air.crosswindSlownessDurationSeconds = ticksToSeconds(v.air().crosswindSlownessDurationTicks());
        cfg.air.crosswindSlownessAmplifier = v.air().crosswindSlownessAmplifier();
        cfg.air.dashCooldownSeconds = ticksToSeconds(v.air().dashCooldownTicks());
        cfg.air.dashVelocity = v.air().dashVelocity();
        cfg.air.dashUpVelocity = v.air().dashUpVelocity();
        cfg.air.dashIFrameDurationSeconds = ticksToSeconds(v.air().dashIFrameDurationTicks());
        cfg.air.dashIFrameResistanceAmplifier = v.air().dashIFrameResistanceAmplifier();
        cfg.air.airMaceBreachLevel = v.air().airMaceBreachLevel();
        cfg.air.airMaceWindBurstLevel = v.air().airMaceWindBurstLevel();
        cfg.air.airMaceMendingLevel = v.air().airMaceMendingLevel();
        cfg.air.airMaceUnbreakingLevel = v.air().airMaceUnbreakingLevel();
        cfg.air.airMaceFireAspectLevel = v.air().airMaceFireAspectLevel();

        cfg.legendary.craftSeconds = ticksToSeconds(v.legendary().craftTicks());
        cfg.legendary.craftMaxPerItem = v.legendary().craftMaxPerItem();
        cfg.legendary.craftMaxActivePerItem = v.legendary().craftMaxActivePerItem();
        cfg.legendary.trackerRefreshSeconds = ticksToSeconds(v.legendary().trackerRefreshTicks());
        cfg.legendary.trackerMaxDistanceBlocks = v.legendary().trackerMaxDistanceBlocks();
        cfg.legendary.recallCooldownSeconds = ticksToSeconds(v.legendary().recallCooldownTicks());
        cfg.legendary.hypnoHoldSeconds = ticksToSeconds(v.legendary().hypnoHoldTicks());
        cfg.legendary.hypnoRangeBlocks = v.legendary().hypnoRangeBlocks();
        cfg.legendary.hypnoViewRangeBlocks = v.legendary().hypnoViewRangeBlocks();
        cfg.legendary.hypnoHealHearts = v.legendary().hypnoHealHearts();
        cfg.legendary.hypnoMaxControlled = v.legendary().hypnoMaxControlled();
        cfg.legendary.hypnoDurationSeconds = ticksToSeconds(v.legendary().hypnoDurationTicks());
        cfg.legendary.earthsplitterRadiusBlocks = v.legendary().earthsplitterRadiusBlocks();
        cfg.legendary.earthsplitterTunnelLengthBlocks = v.legendary().earthsplitterTunnelLengthBlocks();
        cfg.legendary.bloodOathSharpnessCap = v.legendary().bloodOathSharpnessCap();
        cfg.legendary.demolitionCooldownSeconds = ticksToSeconds(v.legendary().demolitionCooldownTicks());
        cfg.legendary.demolitionCooldownScalePercent = v.legendary().demolitionCooldownScalePercent();
        cfg.legendary.demolitionFuseTicks = v.legendary().demolitionFuseTicks();
        cfg.legendary.demolitionRangeBlocks = v.legendary().demolitionRangeBlocks();
        cfg.legendary.demolitionExplosionPower = v.legendary().demolitionExplosionPower();
        cfg.legendary.demolitionTntCount = v.legendary().demolitionTntCount();
        cfg.legendary.hunterAimRangeBlocks = v.legendary().hunterAimRangeBlocks();
        cfg.legendary.hunterAimTimeoutSeconds = ticksToSeconds(v.legendary().hunterAimTimeoutTicks());
        cfg.legendary.hunterAimAssistStrength = v.legendary().hunterAimAssistStrength();
        cfg.legendary.thirdStrikeWindowSeconds = ticksToSeconds(v.legendary().thirdStrikeWindowTicks());
        cfg.legendary.thirdStrikeBonusDamage = v.legendary().thirdStrikeBonusDamage();
        cfg.legendary.vampiricHealAmount = v.legendary().vampiricHealAmount();
        cfg.legendary.supremeHelmetNightVisionAmplifier = v.legendary().supremeHelmetNightVisionAmplifier();
        cfg.legendary.supremeHelmetWaterBreathingAmplifier = v.legendary().supremeHelmetWaterBreathingAmplifier();
        cfg.legendary.supremeChestStrengthAmplifier = v.legendary().supremeChestStrengthAmplifier();
        cfg.legendary.supremeLeggingsFireResAmplifier = v.legendary().supremeLeggingsFireResAmplifier();
        cfg.legendary.supremeBootsSpeedAmplifier = v.legendary().supremeBootsSpeedAmplifier();
        cfg.legendary.supremeSetResistanceAmplifier = v.legendary().supremeSetResistanceAmplifier();
        cfg.legendary.recipeGemRequirements = new HashMap<>();
        for (var entry : v.legendary().recipeGemRequirements().entrySet()) {
            cfg.legendary.recipeGemRequirements.put(entry.getKey().toString(), entry.getValue().name().toLowerCase());
        }

        // Duelist
        cfg.duelist.riposteBonusDamageMultiplier = v.duelist().riposteBonusDamageMultiplier();
        cfg.duelist.riposteWindowSeconds = ticksToSeconds(v.duelist().riposteWindowTicks());
        cfg.duelist.focusBonusDamageMultiplier = v.duelist().focusBonusDamageMultiplier();
        cfg.duelist.focusRadiusBlocks = v.duelist().focusRadiusBlocks();
        cfg.duelist.combatStanceSpeedMultiplier = v.duelist().combatStanceSpeedMultiplier();
        cfg.duelist.lungeCooldownSeconds = ticksToSeconds(v.duelist().lungeCooldownTicks());
        cfg.duelist.lungeDistanceBlocks = v.duelist().lungeDistanceBlocks();
        cfg.duelist.lungeDamage = v.duelist().lungeDamage();
        cfg.duelist.parryCooldownSeconds = ticksToSeconds(v.duelist().parryCooldownTicks());
        cfg.duelist.parryWindowTicks = v.duelist().parryWindowTicks();
        cfg.duelist.parryStunSeconds = ticksToSeconds(v.duelist().parryStunTicks());
        cfg.duelist.flourishCooldownSeconds = ticksToSeconds(v.duelist().flourishCooldownTicks());
        cfg.duelist.flourishRadiusBlocks = v.duelist().flourishRadiusBlocks();
        cfg.duelist.flourishDamage = v.duelist().flourishDamage();
        cfg.duelist.mirrorMatchCooldownSeconds = ticksToSeconds(v.duelist().mirrorMatchCooldownTicks());
        cfg.duelist.mirrorMatchDurationSeconds = ticksToSeconds(v.duelist().mirrorMatchDurationTicks());
        cfg.duelist.mirrorMatchRangeBlocks = v.duelist().mirrorMatchRangeBlocks();
        cfg.duelist.bladeDanceCooldownSeconds = ticksToSeconds(v.duelist().bladeDanceCooldownTicks());
        cfg.duelist.bladeDanceDurationSeconds = ticksToSeconds(v.duelist().bladeDanceDurationTicks());
        cfg.duelist.bladeDanceStartingMultiplier = v.duelist().bladeDanceStartingMultiplier();
        cfg.duelist.bladeDanceIncreasePerHit = v.duelist().bladeDanceIncreasePerHit();
        cfg.duelist.bladeDanceMaxMultiplier = v.duelist().bladeDanceMaxMultiplier();
        cfg.duelist.bladeDanceResetSeconds = ticksToSeconds(v.duelist().bladeDanceResetTicks());

        // Hunter
        cfg.hunter.preyMarkBonusDamageMultiplier = v.hunter().preyMarkBonusDamageMultiplier();
        cfg.hunter.preyMarkDurationSeconds = ticksToSeconds(v.hunter().preyMarkDurationTicks());
        cfg.hunter.trackersEyeRangeBlocks = v.hunter().trackersEyeRangeBlocks();
        cfg.hunter.trophyHunterDurationSeconds = ticksToSeconds(v.hunter().trophyHunterDurationTicks());
        cfg.hunter.huntingTrapCooldownSeconds = ticksToSeconds(v.hunter().huntingTrapCooldownTicks());
        cfg.hunter.huntingTrapRootSeconds = ticksToSeconds(v.hunter().huntingTrapRootTicks());
        cfg.hunter.huntingTrapDamage = v.hunter().huntingTrapDamage();
        cfg.hunter.huntingTrapLifetimeSeconds = ticksToSeconds(v.hunter().huntingTrapLifetimeTicks());
        cfg.hunter.pounceCooldownSeconds = ticksToSeconds(v.hunter().pounceCooldownTicks());
        cfg.hunter.pounceRangeBlocks = v.hunter().pounceRangeBlocks();
        cfg.hunter.pounceDamage = v.hunter().pounceDamage();
        cfg.hunter.netShotCooldownSeconds = ticksToSeconds(v.hunter().netShotCooldownTicks());
        cfg.hunter.netShotSlowSeconds = ticksToSeconds(v.hunter().netShotSlowTicks());
        cfg.hunter.netShotRangeBlocks = v.hunter().netShotRangeBlocks();
        cfg.hunter.cripplingCooldownSeconds = ticksToSeconds(v.hunter().cripplingCooldownTicks());
        cfg.hunter.cripplingSlowMultiplier = v.hunter().cripplingSlowMultiplier();
        cfg.hunter.cripplingDurationSeconds = ticksToSeconds(v.hunter().cripplingDurationTicks());
        cfg.hunter.cripplingRangeBlocks = v.hunter().cripplingRangeBlocks();
        cfg.hunter.packTacticsCooldownSeconds = ticksToSeconds(v.hunter().packTacticsCooldownTicks());
        cfg.hunter.packTacticsBonusDamageMultiplier = v.hunter().packTacticsBonusDamageMultiplier();
        cfg.hunter.packTacticsDurationSeconds = ticksToSeconds(v.hunter().packTacticsDurationTicks());
        cfg.hunter.packTacticsRadiusBlocks = v.hunter().packTacticsRadiusBlocks();

        // Sentinel
        cfg.sentinel.guardianAuraDamageReduction = v.sentinel().guardianAuraDamageReduction();
        cfg.sentinel.guardianAuraRadiusBlocks = v.sentinel().guardianAuraRadiusBlocks();
        cfg.sentinel.fortressStandStillSeconds = ticksToSeconds(v.sentinel().fortressStandStillTicks());
        cfg.sentinel.fortressResistanceAmplifier = v.sentinel().fortressResistanceAmplifier();
        cfg.sentinel.retributionThornsDamageMultiplier = v.sentinel().retributionThornsDamageMultiplier();
        cfg.sentinel.shieldWallCooldownSeconds = ticksToSeconds(v.sentinel().shieldWallCooldownTicks());
        cfg.sentinel.shieldWallDurationSeconds = ticksToSeconds(v.sentinel().shieldWallDurationTicks());
        cfg.sentinel.shieldWallWidthBlocks = v.sentinel().shieldWallWidthBlocks();
        cfg.sentinel.shieldWallHeightBlocks = v.sentinel().shieldWallHeightBlocks();
        cfg.sentinel.tauntCooldownSeconds = ticksToSeconds(v.sentinel().tauntCooldownTicks());
        cfg.sentinel.tauntDurationSeconds = ticksToSeconds(v.sentinel().tauntDurationTicks());
        cfg.sentinel.tauntRadiusBlocks = v.sentinel().tauntRadiusBlocks();
        cfg.sentinel.tauntDamageReduction = v.sentinel().tauntDamageReduction();
        cfg.sentinel.interventionCooldownSeconds = ticksToSeconds(v.sentinel().interventionCooldownTicks());
        cfg.sentinel.interventionRangeBlocks = v.sentinel().interventionRangeBlocks();
        cfg.sentinel.rallyCryCooldownSeconds = ticksToSeconds(v.sentinel().rallyCryCooldownTicks());
        cfg.sentinel.rallyCryHealHearts = v.sentinel().rallyCryHealHearts();
        cfg.sentinel.rallyCryResistanceDurationSeconds = ticksToSeconds(v.sentinel().rallyCryResistanceDurationTicks());
        cfg.sentinel.rallyCryRadiusBlocks = v.sentinel().rallyCryRadiusBlocks();
        cfg.sentinel.lockdownCooldownSeconds = ticksToSeconds(v.sentinel().lockdownCooldownTicks());
        cfg.sentinel.lockdownDurationSeconds = ticksToSeconds(v.sentinel().lockdownDurationTicks());
        cfg.sentinel.lockdownRadiusBlocks = v.sentinel().lockdownRadiusBlocks();

        // Trickster
        cfg.trickster.sleightOfHandChance = v.trickster().sleightOfHandChance();
        cfg.trickster.slipperyChance = v.trickster().slipperyChance();
        cfg.trickster.shadowSwapCooldownSeconds = ticksToSeconds(v.trickster().shadowSwapCooldownTicks());
        cfg.trickster.shadowSwapCloneDurationSeconds = ticksToSeconds(v.trickster().shadowSwapCloneDurationTicks());
        cfg.trickster.mirageCooldownSeconds = ticksToSeconds(v.trickster().mirageCooldownTicks());
        cfg.trickster.mirageDurationSeconds = ticksToSeconds(v.trickster().mirageDurationTicks());
        cfg.trickster.mirageCloneCount = v.trickster().mirageCloneCount();
        cfg.trickster.glitchStepCooldownSeconds = ticksToSeconds(v.trickster().glitchStepCooldownTicks());
        cfg.trickster.glitchStepDistanceBlocks = v.trickster().glitchStepDistanceBlocks();
        cfg.trickster.glitchStepAfterimgDamage = v.trickster().glitchStepAfterimgDamage();
        cfg.trickster.glitchStepAfterimgRadiusBlocks = v.trickster().glitchStepAfterimgRadiusBlocks();
        cfg.trickster.puppetMasterCooldownSeconds = ticksToSeconds(v.trickster().puppetMasterCooldownTicks());
        cfg.trickster.puppetMasterDurationSeconds = ticksToSeconds(v.trickster().puppetMasterDurationTicks());
        cfg.trickster.puppetMasterRangeBlocks = v.trickster().puppetMasterRangeBlocks();
        cfg.trickster.mindGamesCooldownSeconds = ticksToSeconds(v.trickster().mindGamesCooldownTicks());
        cfg.trickster.mindGamesDurationSeconds = ticksToSeconds(v.trickster().mindGamesDurationTicks());
        cfg.trickster.mindGamesRangeBlocks = v.trickster().mindGamesRangeBlocks();

        // BonusPool
        cfg.bonusPool.maxBonusAbilities = v.bonusPool().maxBonusAbilities();
        cfg.bonusPool.maxBonusPassives = v.bonusPool().maxBonusPassives();
        cfg.bonusPool.showBonusesInHud = v.bonusPool().showBonusesInHud();
        cfg.bonusPool.bonusAbilityCooldownMultiplier = v.bonusPool().bonusAbilityCooldownMultiplier();
        cfg.bonusPool.bonusAbilityDamageMultiplier = v.bonusPool().bonusAbilityDamageMultiplier();
        cfg.bonusPool.bonusPassiveEffectMultiplier = v.bonusPool().bonusPassiveEffectMultiplier();

        cfg.mobBlacklist = v.mobBlacklist().stream().map(Identifier::toString).toList();

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
            Systems systems,
            Astra astra,
            Fire fire,
            Flux flux,
            Life life,
            Puff puff,
            Speed speed,
            Strength strength,
            Wealth wealth,
            Terror terror,
            Summoner summoner,
            Space space,
            Reaper reaper,
            Pillager pillager,
            SpyMimic spyMimic,
            Beacon beacon,
            Air air,
            Legendary legendary,
            Duelist duelist,
            Hunter hunter,
            Sentinel sentinel,
            Trickster trickster,
            BonusPool bonusPool,
            List<Identifier> mobBlacklist
    ) {
        public static Values defaults() {
            return from(new GemsBalanceConfig());
        }

        public static Values from(GemsBalanceConfig cfg) {
            return new Values(
                    Visual.from(cfg.visual != null ? cfg.visual : new GemsBalanceConfig.Visual()),
                    Systems.from(cfg.systems != null ? cfg.systems : new GemsBalanceConfig.Systems()),
                    Astra.from(cfg.astra != null ? cfg.astra : new GemsBalanceConfig.Astra()),
                    Fire.from(cfg.fire != null ? cfg.fire : new GemsBalanceConfig.Fire()),
                    Flux.from(cfg.flux != null ? cfg.flux : new GemsBalanceConfig.Flux()),
                    Life.from(cfg.life != null ? cfg.life : new GemsBalanceConfig.Life()),
                    Puff.from(cfg.puff != null ? cfg.puff : new GemsBalanceConfig.Puff()),
                    Speed.from(cfg.speed != null ? cfg.speed : new GemsBalanceConfig.Speed()),
                    Strength.from(cfg.strength != null ? cfg.strength : new GemsBalanceConfig.Strength()),
                    Wealth.from(cfg.wealth != null ? cfg.wealth : new GemsBalanceConfig.Wealth()),
                    Terror.from(cfg.terror != null ? cfg.terror : new GemsBalanceConfig.Terror()),
                    Summoner.from(cfg.summoner != null ? cfg.summoner : new GemsBalanceConfig.Summoner()),
                    Space.from(cfg.space != null ? cfg.space : new GemsBalanceConfig.Space()),
                    Reaper.from(cfg.reaper != null ? cfg.reaper : new GemsBalanceConfig.Reaper()),
                    Pillager.from(cfg.pillager != null ? cfg.pillager : new GemsBalanceConfig.Pillager()),
                    SpyMimic.from(cfg.spyMimic != null ? cfg.spyMimic : new GemsBalanceConfig.SpyMimic()),
                    Beacon.from(cfg.beacon != null ? cfg.beacon : new GemsBalanceConfig.Beacon()),
                    Air.from(cfg.air != null ? cfg.air : new GemsBalanceConfig.Air()),
                    Legendary.from(cfg.legendary != null ? cfg.legendary : new GemsBalanceConfig.Legendary()),
                    Duelist.from(cfg.duelist != null ? cfg.duelist : new GemsBalanceConfig.Duelist()),
                    Hunter.from(cfg.hunter != null ? cfg.hunter : new GemsBalanceConfig.Hunter()),
                    Sentinel.from(cfg.sentinel != null ? cfg.sentinel : new GemsBalanceConfig.Sentinel()),
                    Trickster.from(cfg.trickster != null ? cfg.trickster : new GemsBalanceConfig.Trickster()),
                    BonusPool.from(cfg.bonusPool != null ? cfg.bonusPool : new GemsBalanceConfig.BonusPool()),
                    parseIdentifierList(cfg.mobBlacklist)
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

    public record Systems(
            int minMaxHearts,
            int assassinTriggerHearts,
            int assassinMaxHearts,
            int assassinEliminationHeartsThreshold,
            int assassinVsAssassinVictimHeartsLoss,
            int assassinVsAssassinKillerHeartsGain,
            double controlledFollowStartBlocks,
            double controlledFollowStopBlocks,
            double controlledFollowSpeed
    ) {
        static Systems from(GemsBalanceConfig.Systems cfg) {
            double followStart = clampDouble(cfg.controlledFollowStartBlocks, 0.0D, 128.0D);
            double followStop = clampDouble(cfg.controlledFollowStopBlocks, 0.0D, 128.0D);
            if (followStop > followStart) {
                followStop = followStart;
            }
            double followSpeed = clampDouble(cfg.controlledFollowSpeed, 0.0D, 3.0D);
            int maxHearts = clampInt(cfg.assassinMaxHearts, 1, com.feel.gems.state.GemPlayerState.MAX_MAX_HEARTS);
            int eliminationThreshold = clampInt(cfg.assassinEliminationHeartsThreshold, 0, maxHearts);
            return new Systems(
                    clampInt(cfg.minMaxHearts, 1, com.feel.gems.state.GemPlayerState.MAX_MAX_HEARTS),
                    clampInt(cfg.assassinTriggerHearts, 1, com.feel.gems.state.GemPlayerState.MAX_MAX_HEARTS),
                    maxHearts,
                    eliminationThreshold,
                    clampInt(cfg.assassinVsAssassinVictimHeartsLoss, 0, maxHearts),
                    clampInt(cfg.assassinVsAssassinKillerHeartsGain, 0, maxHearts),
                    followStart,
                    followStop,
                    followSpeed
            );
        }
    }

    public record Astra(
            int shadowAnchorWindowTicks,
            int shadowAnchorPostCooldownTicks,
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
            int tagDurationTicks,
            float soulHealingHearts,
            double soulReleaseForwardBlocks,
            double soulReleaseUpBlocks
    ) {
        static Astra from(GemsBalanceConfig.Astra cfg) {
            return new Astra(
                    secClamped(cfg.shadowAnchorWindowSeconds, 1, 60),
                    secClamped(cfg.shadowAnchorPostCooldownSeconds, 0, 3600),
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
                    secClamped(cfg.tagDurationSeconds, 0, 120),
                    clampFloat(cfg.soulHealingHearts, 0.0F, 40.0F),
                    clampDouble(cfg.soulReleaseForwardBlocks, 0.0D, 32.0D),
                    clampDouble(cfg.soulReleaseUpBlocks, 0.0D, 16.0D)
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
            int meteorShowerTargetRangeBlocks,
            int meteorShowerCount,
            int meteorShowerSpreadBlocks,
            int meteorShowerHeightBlocks,
            float meteorShowerVelocity,
            int meteorShowerExplosionPower
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
                    clampInt(cfg.meteorShowerTargetRangeBlocks, 1, 128),
                    clampInt(cfg.meteorShowerCount, 0, 50),
                    clampInt(cfg.meteorShowerSpreadBlocks, 0, 48),
                    clampInt(cfg.meteorShowerHeightBlocks, 1, 256),
                    clampFloat(cfg.meteorShowerVelocity, 0.1F, 6.0F),
                    clampInt(cfg.meteorShowerExplosionPower, 1, 6)
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
            int chargeEmeraldBlock,
            int chargeAmethystBlock,
            int chargeNetheriteScrap,
            int chargeEnchantedDiamondItem,
            int overchargeDelayTicks,
            int overchargePerSecond,
            float overchargeSelfDamagePerSecond,
            int fluxCapacitorChargeThreshold,
            int fluxCapacitorAbsorptionAmplifier,
            int fluxConductivityChargePerDamage,
            int fluxConductivityMaxChargePerHit,
            int fluxInsulationChargeThreshold,
            float fluxInsulationDamageMultiplier,
            int fluxSurgeCooldownTicks,
            int fluxSurgeDurationTicks,
            int fluxSurgeSpeedAmplifier,
            int fluxSurgeResistanceAmplifier,
            int fluxSurgeChargeCost,
            int fluxSurgeRadiusBlocks,
            double fluxSurgeKnockback,
            int fluxDischargeCooldownTicks,
            int fluxDischargeRadiusBlocks,
            float fluxDischargeBaseDamage,
            float fluxDischargeDamagePerCharge,
            float fluxDischargeMaxDamage,
            int fluxDischargeMinCharge,
            double fluxDischargeKnockback
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
                    clampInt(cfg.chargeEmeraldBlock, 0, 200),
                    clampInt(cfg.chargeAmethystBlock, 0, 200),
                    clampInt(cfg.chargeNetheriteScrap, 0, 200),
                    clampInt(cfg.chargeEnchantedDiamondItem, 0, 200),
                    secClamped(cfg.overchargeDelaySeconds, 0, 60),
                    clampInt(cfg.overchargePerSecond, 0, 100),
                    clampFloat(cfg.overchargeSelfDamagePerSecond, 0.0F, 20.0F),
                    clampInt(cfg.fluxCapacitorChargeThreshold, 0, 200),
                    clampInt(cfg.fluxCapacitorAbsorptionAmplifier, 0, 10),
                    clampInt(cfg.fluxConductivityChargePerDamage, 0, 50),
                    clampInt(cfg.fluxConductivityMaxChargePerHit, 0, 200),
                    clampInt(cfg.fluxInsulationChargeThreshold, 0, 200),
                    clampFloat(cfg.fluxInsulationDamageMultiplier, 0.0F, 1.0F),
                    secClamped(cfg.fluxSurgeCooldownSeconds, 0, 3600),
                    secClamped(cfg.fluxSurgeDurationSeconds, 0, 120),
                    clampInt(cfg.fluxSurgeSpeedAmplifier, 0, 10),
                    clampInt(cfg.fluxSurgeResistanceAmplifier, 0, 10),
                    clampInt(cfg.fluxSurgeChargeCost, 0, 200),
                    clampInt(cfg.fluxSurgeRadiusBlocks, 0, 32),
                    clampDouble(cfg.fluxSurgeKnockback, 0.0D, 5.0D),
                    secClamped(cfg.fluxDischargeCooldownSeconds, 0, 3600),
                    clampInt(cfg.fluxDischargeRadiusBlocks, 0, 32),
                    clampFloat(cfg.fluxDischargeBaseDamage, 0.0F, 120.0F),
                    clampFloat(cfg.fluxDischargeDamagePerCharge, 0.0F, 10.0F),
                    clampFloat(cfg.fluxDischargeMaxDamage, 0.0F, 120.0F),
                    clampInt(cfg.fluxDischargeMinCharge, 0, 200),
                    clampDouble(cfg.fluxDischargeKnockback, 0.0D, 5.0D)
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
            int lifeSwapCooldownTicks,
            int lifeSwapRangeBlocks,
            float lifeSwapMinHearts,
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
                    secClamped(cfg.lifeSwapCooldownSeconds, 0, 3600),
                    clampInt(cfg.lifeSwapRangeBlocks, 0, 128),
                    clampFloat(cfg.lifeSwapMinHearts, 1.0F, 20.0F),
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
            double groupBashUpVelocityY,
            int gustCooldownTicks,
            int gustRadiusBlocks,
            double gustUpVelocityY,
            double gustKnockback,
            int gustSlownessDurationTicks,
            int gustSlownessAmplifier,
            int gustSlowFallingDurationTicks,
            int windborneDurationTicks,
            int windborneSlowFallingAmplifier
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
                    clampDouble(cfg.groupBashUpVelocityY, 0.0D, 5.0D),
                    secClamped(cfg.gustCooldownSeconds, 0, 3600),
                    clampInt(cfg.gustRadiusBlocks, 0, 64),
                    clampDouble(cfg.gustUpVelocityY, 0.0D, 5.0D),
                    clampDouble(cfg.gustKnockback, 0.0D, 5.0D),
                    secClamped(cfg.gustSlownessDurationSeconds, 0, 120),
                    clampInt(cfg.gustSlownessAmplifier, 0, 10),
                    secClamped(cfg.gustSlowFallingDurationSeconds, 0, 120),
                    secClamped(cfg.windborneDurationSeconds, 0, 60),
                    clampInt(cfg.windborneSlowFallingAmplifier, 0, 10)
            );
        }
    }

    public record Speed(
            double momentumMinSpeed,
            double momentumMaxSpeed,
            float momentumMinMultiplier,
            float momentumMaxMultiplier,
            int frictionlessSpeedAmplifier,
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
            int terminalVelocityHasteAmplifier,
            int slipstreamCooldownTicks,
            int slipstreamDurationTicks,
            int slipstreamLengthBlocks,
            int slipstreamRadiusBlocks,
            int slipstreamAllySpeedAmplifier,
            int slipstreamEnemySlownessAmplifier,
            double slipstreamEnemyKnockback,
            int afterimageCooldownTicks,
            int afterimageDurationTicks,
            int afterimageSpeedAmplifier,
            int tempoShiftCooldownTicks,
            int tempoShiftDurationTicks,
            int tempoShiftRadiusBlocks,
            int tempoShiftAllyCooldownTicksPerSecond,
            int tempoShiftEnemyCooldownTicksPerSecond
    ) {
        static Speed from(GemsBalanceConfig.Speed cfg) {
            return new Speed(
                    clampDouble(cfg.momentumMinSpeed, 0.0D, 2.0D),
                    clampDouble(cfg.momentumMaxSpeed, 0.0D, 2.0D),
                    clampFloat(cfg.momentumMinMultiplier, 0.1F, 3.0F),
                    clampFloat(cfg.momentumMaxMultiplier, 0.1F, 3.0F),
                    clampInt(cfg.frictionlessSpeedAmplifier, 0, 5),
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
                    clampInt(cfg.terminalVelocityHasteAmplifier, 0, 10),
                    secClamped(cfg.slipstreamCooldownSeconds, 0, 3600),
                    secClamped(cfg.slipstreamDurationSeconds, 0, 120),
                    clampInt(cfg.slipstreamLengthBlocks, 0, 128),
                    clampInt(cfg.slipstreamRadiusBlocks, 0, 32),
                    clampInt(cfg.slipstreamAllySpeedAmplifier, 0, 10),
                    clampInt(cfg.slipstreamEnemySlownessAmplifier, 0, 10),
                    clampDouble(cfg.slipstreamEnemyKnockback, 0.0D, 3.0D),
                    secClamped(cfg.afterimageCooldownSeconds, 0, 3600),
                    secClamped(cfg.afterimageDurationSeconds, 0, 120),
                    clampInt(cfg.afterimageSpeedAmplifier, 0, 10),
                    secClamped(cfg.tempoShiftCooldownSeconds, 0, 3600),
                    secClamped(cfg.tempoShiftDurationSeconds, 0, 3600),
                    clampInt(cfg.tempoShiftRadiusBlocks, 0, 48),
                    clampInt(cfg.tempoShiftAllyCooldownTicksPerSecond, 0, 200),
                    clampInt(cfg.tempoShiftEnemyCooldownTicksPerSecond, 0, 200)
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
            float chadBonusDamage,
            float adrenalineThresholdHearts,
            int adrenalineDurationTicks,
            int adrenalineResistanceAmplifier
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
                    clampFloat(cfg.chadBonusDamage, 0.0F, 80.0F),
                    clampFloat(cfg.adrenalineThresholdHearts, 0.0F, 20.0F),
                    secClamped(cfg.adrenalineDurationSeconds, 0, 120),
                    clampInt(cfg.adrenalineResistanceAmplifier, 0, 10)
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
            int richRushDurationTicks,
            int pocketsRows
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
                    secClamped(cfg.richRushDurationSeconds, 0, 24 * 3600),
                    clampInt(cfg.pocketsRows, 1, 6)
            );
        }
    }

    public record Terror(
            int dreadAuraRadiusBlocks,
            int dreadAuraAmplifier,
            int bloodPriceDurationTicks,
            int bloodPriceStrengthAmplifier,
            int bloodPriceResistanceAmplifier,
            int terrorTradeCooldownTicks,
            int terrorTradeRangeBlocks,
            int terrorTradeMaxUses,
            int terrorTradeHeartsCost,
            int terrorTradePermanentEnergyPenalty,
            int terrorTradeNormalTargetHeartsPenalty,
            int terrorTradeNormalTargetEnergyPenalty,
            int panicRingCooldownTicks,
            int panicRingTntCount,
            int panicRingFuseTicks,
            double panicRingRadiusBlocks,
            int rigCooldownTicks,
            int rigRangeBlocks,
            int rigDurationTicks,
            int rigFuseTicks,
            int rigTntCount,
            int remoteChargeArmWindowTicks,
            int remoteChargeDetonateWindowTicks,
            int remoteChargeFuseTicks,
            int remoteChargeCooldownTicks,
            int breachChargeCooldownTicks,
            int breachChargeRangeBlocks,
            int breachChargeFuseTicks,
            float breachChargeExplosionPower
    ) {
        static Terror from(GemsBalanceConfig.Terror cfg) {
            return new Terror(
                    clampInt(cfg.dreadAuraRadiusBlocks, 0, 32),
                    clampInt(cfg.dreadAuraAmplifier, 0, 10),
                    secClamped(cfg.bloodPriceDurationSeconds, 0, 60),
                    clampInt(cfg.bloodPriceStrengthAmplifier, 0, 10),
                    clampInt(cfg.bloodPriceResistanceAmplifier, 0, 10),
                    secClamped(cfg.terrorTradeCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.terrorTradeRangeBlocks, 0, 128),
                    clampInt(cfg.terrorTradeMaxUses, 0, 10),
                    clampInt(cfg.terrorTradeHeartsCost, 0, 10),
                    clampInt(cfg.terrorTradePermanentEnergyPenalty, 0, 10),
                    clampInt(cfg.terrorTradeNormalTargetHeartsPenalty, 0, 20),
                    clampInt(cfg.terrorTradeNormalTargetEnergyPenalty, 0, 10),
                    secClamped(cfg.panicRingCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.panicRingTntCount, 0, 40),
                    clampInt(cfg.panicRingFuseTicks, 1, 200),
                    clampDouble(cfg.panicRingRadiusBlocks, 0.0D, 8.0D),
                    secClamped(cfg.rigCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.rigRangeBlocks, 0, 128),
                    secClamped(cfg.rigDurationSeconds, 0, 24 * 3600),
                    clampInt(cfg.rigFuseTicks, 1, 200),
                    clampInt(cfg.rigTntCount, 0, 50),
                    secClamped(cfg.remoteChargeArmWindowSeconds, 0, 60),
                    secClamped(cfg.remoteChargeDetonateWindowSeconds, 0, 3600),
                    clampInt(cfg.remoteChargeFuseTicks, 1, 200),
                    secClamped(cfg.remoteChargeCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.breachChargeCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.breachChargeRangeBlocks, 0, 128),
                    clampInt(cfg.breachChargeFuseTicks, 1, 200),
                    clampFloat(cfg.breachChargeExplosionPower, 0.0F, 12.0F)
            );
        }
    }

    public record Summoner(
            int maxPoints,
            int maxActiveSummons,
            int summonLifetimeTicks,
            int commandRangeBlocks,
            int summonSlotCooldownTicks,
            int recallCooldownTicks,
            int commandersMarkDurationTicks,
            int commandersMarkStrengthAmplifier,
            float summonBonusHealth,
            double summonSpawnForwardBlocks,
            double summonSpawnUpBlocks,
            double summonSpawnRingBaseBlocks,
            double summonSpawnRingStepBlocks,
            int summonSpawnRingLayers,
            int summonSpawnRingSegments,
            java.util.Map<String, Integer> costs,
            java.util.List<GemsBalanceConfig.Summoner.SummonSpec> slot1,
            java.util.List<GemsBalanceConfig.Summoner.SummonSpec> slot2,
            java.util.List<GemsBalanceConfig.Summoner.SummonSpec> slot3,
            java.util.List<GemsBalanceConfig.Summoner.SummonSpec> slot4,
            java.util.List<GemsBalanceConfig.Summoner.SummonSpec> slot5
    ) {
        static Summoner from(GemsBalanceConfig.Summoner cfg) {
            return new Summoner(
                    clampInt(cfg.maxPoints, 0, 500),
                    clampInt(cfg.maxActiveSummons, 0, 200),
                    secClamped(cfg.summonLifetimeSeconds, 0, 24 * 3600),
                    clampInt(cfg.commandRangeBlocks, 0, 128),
                    secClamped(cfg.summonSlotCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.recallCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.commandersMarkDurationSeconds, 0, 60),
                    clampInt(cfg.commandersMarkStrengthAmplifier, 0, 10),
                    clampFloat(cfg.summonBonusHealth, 0.0F, 40.0F),
                    clampDouble(cfg.summonSpawnForwardBlocks, 0.0D, 16.0D),
                    clampDouble(cfg.summonSpawnUpBlocks, 0.0D, 8.0D),
                    clampDouble(cfg.summonSpawnRingBaseBlocks, 0.0D, 4.0D),
                    clampDouble(cfg.summonSpawnRingStepBlocks, 0.0D, 4.0D),
                    clampInt(cfg.summonSpawnRingLayers, 1, 8),
                    clampInt(cfg.summonSpawnRingSegments, 1, 16),
                    sanitizeCosts(cfg.costs),
                    sanitizeSpecs(cfg.slot1),
                    sanitizeSpecs(cfg.slot2),
                    sanitizeSpecs(cfg.slot3),
                    sanitizeSpecs(cfg.slot4),
                    sanitizeSpecs(cfg.slot5)
            );
        }

        private static java.util.Map<String, Integer> sanitizeCosts(java.util.Map<String, Integer> costs) {
            if (costs == null || costs.isEmpty()) {
                return java.util.Map.of();
            }
            java.util.Map<String, Integer> out = new java.util.HashMap<>();
            for (var e : costs.entrySet()) {
                if (e.getKey() == null || e.getKey().isBlank()) {
                    continue;
                }
                int v = e.getValue() != null ? e.getValue() : 0;
                out.put(e.getKey(), clampInt(v, 0, 500));
            }
            return java.util.Collections.unmodifiableMap(out);
        }

        private static java.util.List<GemsBalanceConfig.Summoner.SummonSpec> sanitizeSpecs(java.util.List<GemsBalanceConfig.Summoner.SummonSpec> list) {
            if (list == null || list.isEmpty()) {
                return java.util.List.of();
            }
            java.util.List<GemsBalanceConfig.Summoner.SummonSpec> out = new java.util.ArrayList<>();
            for (GemsBalanceConfig.Summoner.SummonSpec spec : list) {
                if (spec == null || spec.entityId == null || spec.entityId.isBlank()) {
                    continue;
                }
                int count = clampInt(spec.count, 0, 64);
                if (count <= 0) {
                    continue;
                }
                out.add(new GemsBalanceConfig.Summoner.SummonSpec(spec.entityId, count));
            }
            return java.util.Collections.unmodifiableList(out);
        }
    }

    public record Space(
            float lunarMinMultiplier,
            float lunarMaxMultiplier,
            float starshieldProjectileDamageMultiplier,
            int orbitalLaserCooldownTicks,
            int orbitalLaserRangeBlocks,
            int orbitalLaserDelayTicks,
            int orbitalLaserRadiusBlocks,
            float orbitalLaserDamage,
            int orbitalLaserMiningRadiusBlocks,
            float orbitalLaserMiningHardnessCap,
            int orbitalLaserMiningMaxBlocks,
            int gravityFieldCooldownTicks,
            int gravityFieldDurationTicks,
            int gravityFieldRadiusBlocks,
            float gravityFieldAllyGravityMultiplier,
            float gravityFieldEnemyGravityMultiplier,
            int blackHoleCooldownTicks,
            int blackHoleDurationTicks,
            int blackHoleRadiusBlocks,
            float blackHolePullStrength,
            float blackHoleDamagePerSecond,
            int whiteHoleCooldownTicks,
            int whiteHoleDurationTicks,
            int whiteHoleRadiusBlocks,
            float whiteHolePushStrength,
            float whiteHoleDamagePerSecond
    ) {
        static Space from(GemsBalanceConfig.Space cfg) {
            float lunarMin = clampFloat(cfg.lunarMinMultiplier, 0.1F, 5.0F);
            float lunarMax = clampFloat(cfg.lunarMaxMultiplier, lunarMin, 5.0F);
            return new Space(
                    lunarMin,
                    lunarMax,
                    clampFloat(cfg.starshieldProjectileDamageMultiplier, 0.0F, 1.0F),
                    secClamped(cfg.orbitalLaserCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.orbitalLaserRangeBlocks, 0, 256),
                    secClamped(cfg.orbitalLaserDelaySeconds, 0, 10),
                    clampInt(cfg.orbitalLaserRadiusBlocks, 0, 32),
                    clampFloat(cfg.orbitalLaserDamage, 0.0F, 200.0F),
                    clampInt(cfg.orbitalLaserMiningRadiusBlocks, 0, 16),
                    clampFloat(cfg.orbitalLaserMiningHardnessCap, 0.0F, 2000.0F),
                    clampInt(cfg.orbitalLaserMiningMaxBlocks, 0, 2048),
                    secClamped(cfg.gravityFieldCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.gravityFieldDurationSeconds, 0, 120),
                    clampInt(cfg.gravityFieldRadiusBlocks, 0, 64),
                    clampFloat(cfg.gravityFieldAllyGravityMultiplier, 0.1F, 5.0F),
                    clampFloat(cfg.gravityFieldEnemyGravityMultiplier, 0.1F, 5.0F),
                    secClamped(cfg.blackHoleCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.blackHoleDurationSeconds, 0, 60),
                    clampInt(cfg.blackHoleRadiusBlocks, 0, 64),
                    clampFloat(cfg.blackHolePullStrength, 0.0F, 5.0F),
                    clampFloat(cfg.blackHoleDamagePerSecond, 0.0F, 200.0F),
                    secClamped(cfg.whiteHoleCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.whiteHoleDurationSeconds, 0, 60),
                    clampInt(cfg.whiteHoleRadiusBlocks, 0, 64),
                    clampFloat(cfg.whiteHolePushStrength, 0.0F, 5.0F),
                    clampFloat(cfg.whiteHoleDamagePerSecond, 0.0F, 200.0F)
            );
        }
    }

    public record Reaper(
            float undeadWardDamageMultiplier,
            int harvestRegenDurationTicks,
            int harvestRegenAmplifier,
            int graveSteedCooldownTicks,
            int graveSteedDurationTicks,
            float graveSteedDecayDamagePerSecond,
            int witheringStrikesCooldownTicks,
            int witheringStrikesDurationTicks,
            int witheringStrikesWitherDurationTicks,
            int witheringStrikesWitherAmplifier,
            int deathOathCooldownTicks,
            int deathOathDurationTicks,
            int deathOathRangeBlocks,
            float deathOathSelfDamagePerSecond,
            float deathOathBonusDamage,
            int retributionCooldownTicks,
            int retributionDurationTicks,
            float retributionDamageMultiplier,
            int scytheSweepCooldownTicks,
            int scytheSweepRangeBlocks,
            int scytheSweepArcDegrees,
            float scytheSweepDamage,
            double scytheSweepKnockback,
            int bloodChargeCooldownTicks,
            int bloodChargeMaxChargeTicks,
            float bloodChargeSelfDamagePerSecond,
            float bloodChargeMaxMultiplier,
            int bloodChargeBuffDurationTicks,
            int shadowCloneCooldownTicks,
            int shadowCloneDurationTicks,
            float shadowCloneMaxHealth,
            int shadowCloneCount,
            String shadowCloneEntityId
    ) {
        static Reaper from(GemsBalanceConfig.Reaper cfg) {
            return new Reaper(
                    clampFloat(cfg.undeadWardDamageMultiplier, 0.0F, 1.0F),
                    secClamped(cfg.harvestRegenDurationSeconds, 0, 60),
                    clampInt(cfg.harvestRegenAmplifier, 0, 10),
                    secClamped(cfg.graveSteedCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.graveSteedDurationSeconds, 0, 24 * 3600),
                    clampFloat(cfg.graveSteedDecayDamagePerSecond, 0.0F, 200.0F),
                    secClamped(cfg.witheringStrikesCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.witheringStrikesDurationSeconds, 0, 120),
                    secClamped(cfg.witheringStrikesWitherDurationSeconds, 0, 60),
                    clampInt(cfg.witheringStrikesWitherAmplifier, 0, 10),
                    secClamped(cfg.deathOathCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.deathOathDurationSeconds, 0, 300),
                    clampInt(cfg.deathOathRangeBlocks, 0, 256),
                    clampFloat(cfg.deathOathSelfDamagePerSecond, 0.0F, 200.0F),
                    clampFloat(cfg.deathOathBonusDamage, 0.0F, 200.0F),
                    secClamped(cfg.retributionCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.retributionDurationSeconds, 0, 300),
                    clampFloat(cfg.retributionDamageMultiplier, 0.0F, 10.0F),
                    secClamped(cfg.scytheSweepCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.scytheSweepRangeBlocks, 0, 16),
                    clampInt(cfg.scytheSweepArcDegrees, 10, 180),
                    clampFloat(cfg.scytheSweepDamage, 0.0F, 200.0F),
                    clampDouble(cfg.scytheSweepKnockback, 0.0D, 5.0D),
                    secClamped(cfg.bloodChargeCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.bloodChargeMaxChargeSeconds, 0, 60),
                    clampFloat(cfg.bloodChargeSelfDamagePerSecond, 0.0F, 200.0F),
                    clampFloat(cfg.bloodChargeMaxMultiplier, 1.0F, 10.0F),
                    secClamped(cfg.bloodChargeBuffDurationSeconds, 0, 300),
                    secClamped(cfg.shadowCloneCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.shadowCloneDurationSeconds, 0, 120),
                    clampFloat(cfg.shadowCloneMaxHealth, 1.0F, 200.0F),
                    clampInt(cfg.shadowCloneCount, 1, 50),
                    cfg.shadowCloneEntityId
            );
        }
    }

    public record Pillager(
            float raidersTrainingProjectileVelocityMultiplier,
            int shieldbreakerDisableCooldownTicks,
            float illagerDisciplineThresholdHearts,
            int illagerDisciplineResistanceDurationTicks,
            int illagerDisciplineResistanceAmplifier,
            int illagerDisciplineCooldownTicks,
            int fangsCooldownTicks,
            int fangsRangeBlocks,
            int fangsCount,
            float fangsSpacingBlocks,
            int fangsWarmupStepTicks,
            int ravageCooldownTicks,
            int ravageRangeBlocks,
            float ravageDamage,
            double ravageKnockback,
            int vindicatorBreakCooldownTicks,
            int vindicatorBreakDurationTicks,
            int vindicatorBreakStrengthAmplifier,
            int vindicatorBreakShieldDisableCooldownTicks,
            int volleyCooldownTicks,
            int volleyDurationTicks,
            int volleyPeriodTicks,
            int volleyArrowsPerShot,
            float volleyArrowDamage,
            float volleyArrowVelocity,
            float volleyArrowInaccuracy,
            int warhornCooldownTicks,
            int warhornRadiusBlocks,
            int warhornDurationTicks,
            int warhornAllySpeedAmplifier,
            int warhornAllyResistanceAmplifier,
            int warhornEnemySlownessAmplifier,
            int warhornEnemyWeaknessAmplifier,
            int snareCooldownTicks,
            int snareRangeBlocks,
            int snareDurationTicks,
            int snareSlownessAmplifier
    ) {
        static Pillager from(GemsBalanceConfig.Pillager cfg) {
            return new Pillager(
                    clampFloat(cfg.raidersTrainingProjectileVelocityMultiplier, 1.0F, 3.0F),
                    clampInt(cfg.shieldbreakerDisableCooldownTicks, 0, 20 * 60),
                    clampFloat(cfg.illagerDisciplineThresholdHearts, 1.0F, 20.0F),
                    secClamped(cfg.illagerDisciplineResistanceDurationSeconds, 0, 60),
                    clampInt(cfg.illagerDisciplineResistanceAmplifier, 0, 10),
                    secClamped(cfg.illagerDisciplineCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.fangsCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.fangsRangeBlocks, 0, 128),
                    clampInt(cfg.fangsCount, 0, 32),
                    clampFloat(cfg.fangsSpacingBlocks, 0.1F, 6.0F),
                    clampInt(cfg.fangsWarmupStepTicks, 0, 40),
                    secClamped(cfg.ravageCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.ravageRangeBlocks, 0, 32),
                    clampFloat(cfg.ravageDamage, 0.0F, 200.0F),
                    clampDouble(cfg.ravageKnockback, 0.0D, 5.0D),
                    secClamped(cfg.vindicatorBreakCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.vindicatorBreakDurationSeconds, 0, 120),
                    clampInt(cfg.vindicatorBreakStrengthAmplifier, 0, 10),
                    clampInt(cfg.vindicatorBreakShieldDisableCooldownTicks, 0, 20 * 60),
                    secClamped(cfg.volleyCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.volleyDurationSeconds, 0, 60),
                    clampInt(cfg.volleyPeriodTicks, 1, 40),
                    clampInt(cfg.volleyArrowsPerShot, 0, 8),
                    clampFloat(cfg.volleyArrowDamage, 0.0F, 200.0F),
                    clampFloat(cfg.volleyArrowVelocity, 0.1F, 10.0F),
                    clampFloat(cfg.volleyArrowInaccuracy, 0.0F, 10.0F),
                    secClamped(cfg.warhornCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.warhornRadiusBlocks, 0, 64),
                    secClamped(cfg.warhornDurationSeconds, 0, 60),
                    clampInt(cfg.warhornAllySpeedAmplifier, 0, 10),
                    clampInt(cfg.warhornAllyResistanceAmplifier, 0, 10),
                    clampInt(cfg.warhornEnemySlownessAmplifier, 0, 10),
                    clampInt(cfg.warhornEnemyWeaknessAmplifier, 0, 10),
                    secClamped(cfg.snareCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.snareRangeBlocks, 0, 128),
                    secClamped(cfg.snareDurationSeconds, 0, 60),
                    clampInt(cfg.snareSlownessAmplifier, 0, 10)
            );
        }
    }

    public record SpyMimic(
            int stillnessTicks,
            float stillnessMoveEpsilonBlocks,
            int stillnessInvisRefreshTicks,
            int backstepCooldownTicks,
            double backstepVelocity,
            double backstepUpVelocity,
            float backstabBonusDamage,
            int backstabAngleDegrees,
            int observeRangeBlocks,
            int observeWindowTicks,
            int stealRequiredWitnessCount,
            int maxStolenAbilities,
            int mimicFormCooldownTicks,
            int mimicFormDurationTicks,
            float mimicFormBonusMaxHealth,
            float mimicFormSpeedMultiplier,
            int echoCooldownTicks,
            int echoWindowTicks,
            int stealCooldownTicks,
            int smokeBombCooldownTicks,
            int smokeBombRadiusBlocks,
            int smokeBombDurationTicks,
            int smokeBombBlindnessAmplifier,
            int smokeBombSlownessAmplifier,
            int stolenCastCooldownTicks,
            int skinshiftCooldownTicks,
            int skinshiftDurationTicks,
            int skinshiftRangeBlocks
    ) {
        static SpyMimic from(GemsBalanceConfig.SpyMimic cfg) {
            return new SpyMimic(
                    secClamped(cfg.stillnessSeconds, 0, 60),
                    clampFloat(cfg.stillnessMoveEpsilonBlocks, 0.0F, 2.0F),
                    secClamped(cfg.stillnessInvisRefreshSeconds, 0, 60),
                    secClamped(cfg.backstepCooldownSeconds, 0, 3600),
                    clampDouble(cfg.backstepVelocity, 0.0D, 3.0D),
                    clampDouble(cfg.backstepUpVelocity, 0.0D, 3.0D),
                    clampFloat(cfg.backstabBonusDamage, 0.0F, 200.0F),
                    clampInt(cfg.backstabAngleDegrees, 10, 180),
                    clampInt(cfg.observeRangeBlocks, 0, 128),
                    secClamped(cfg.observeWindowSeconds, 0, 24 * 3600),
                    clampInt(cfg.stealRequiredWitnessCount, 1, 64),
                    clampInt(cfg.maxStolenAbilities, 0, 10),
                    secClamped(cfg.mimicFormCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.mimicFormDurationSeconds, 0, 300),
                    clampFloat(cfg.mimicFormBonusMaxHealth, 0.0F, 200.0F),
                    clampFloat(cfg.mimicFormSpeedMultiplier, 0.1F, 3.0F),
                    secClamped(cfg.echoCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.echoWindowSeconds, 0, 120),
                    secClamped(cfg.stealCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.smokeBombCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.smokeBombRadiusBlocks, 0, 64),
                    secClamped(cfg.smokeBombDurationSeconds, 0, 60),
                    clampInt(cfg.smokeBombBlindnessAmplifier, 0, 10),
                    clampInt(cfg.smokeBombSlownessAmplifier, 0, 10),
                    secClamped(cfg.stolenCastCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.skinshiftCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.skinshiftDurationSeconds, 0, 300),
                    clampInt(cfg.skinshiftRangeBlocks, 0, 128)
            );
        }
    }

    public record Beacon(
            int coreRadiusBlocks,
            int corePulsePeriodTicks,
            int coreRegenDurationTicks,
            int coreRegenAmplifier,
            int stabilizeRadiusBlocks,
            int stabilizeReduceTicksPerSecond,
            int rallyRadiusBlocks,
            int rallyAbsorptionHearts,
            int rallyDurationTicks,
            int auraCooldownTicks,
            int auraDurationTicks,
            int auraRadiusBlocks,
            int auraRefreshTicks,
            int auraSpeedAmplifier,
            int auraHasteAmplifier,
            int auraResistanceAmplifier,
            int auraJumpAmplifier,
            int auraStrengthAmplifier,
            int auraRegenAmplifier
    ) {
        static Beacon from(GemsBalanceConfig.Beacon cfg) {
            return new Beacon(
                    clampInt(cfg.coreRadiusBlocks, 0, 64),
                    secClamped(cfg.corePulsePeriodSeconds, 1, 60),
                    secClamped(cfg.coreRegenDurationSeconds, 1, 60),
                    clampInt(cfg.coreRegenAmplifier, 0, 10),
                    clampInt(cfg.stabilizeRadiusBlocks, 0, 64),
                    clampInt(cfg.stabilizeReduceTicksPerSecond, 0, 200),
                    clampInt(cfg.rallyRadiusBlocks, 0, 64),
                    clampInt(cfg.rallyAbsorptionHearts, 0, 40),
                    secClamped(cfg.rallyDurationSeconds, 0, 120),
                    secClamped(cfg.auraCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.auraDurationSeconds, 0, 600),
                    clampInt(cfg.auraRadiusBlocks, 0, 128),
                    secClamped(cfg.auraRefreshSeconds, 1, 60),
                    clampInt(cfg.auraSpeedAmplifier, 0, 10),
                    clampInt(cfg.auraHasteAmplifier, 0, 10),
                    clampInt(cfg.auraResistanceAmplifier, 0, 10),
                    clampInt(cfg.auraJumpAmplifier, 0, 10),
                    clampInt(cfg.auraStrengthAmplifier, 0, 10),
                    clampInt(cfg.auraRegenAmplifier, 0, 10)
            );
        }
    }

    public record Air(
            float aerialGuardFallDamageMultiplier,
            float aerialGuardDamageMultiplier,
            float aerialGuardKnockbackMultiplier,
            double windShearKnockback,
            int windShearSlownessDurationTicks,
            int windShearSlownessAmplifier,
            int windJumpCooldownTicks,
            double windJumpVerticalVelocity,
            double windJumpForwardVelocity,
            int galeSlamCooldownTicks,
            int galeSlamWindowTicks,
            int galeSlamRadiusBlocks,
            float galeSlamBonusDamage,
            double galeSlamKnockback,
            int crosswindCooldownTicks,
            int crosswindRangeBlocks,
            int crosswindRadiusBlocks,
            float crosswindDamage,
            double crosswindKnockback,
            int crosswindSlownessDurationTicks,
            int crosswindSlownessAmplifier,
            int dashCooldownTicks,
            double dashVelocity,
            double dashUpVelocity,
            int dashIFrameDurationTicks,
            int dashIFrameResistanceAmplifier,
            int airMaceBreachLevel,
            int airMaceWindBurstLevel,
            int airMaceMendingLevel,
            int airMaceUnbreakingLevel,
            int airMaceFireAspectLevel
    ) {
        static Air from(GemsBalanceConfig.Air cfg) {
            return new Air(
                    clampFloat(cfg.aerialGuardFallDamageMultiplier, 0.0F, 1.0F),
                    clampFloat(cfg.aerialGuardDamageMultiplier, 0.0F, 1.0F),
                    clampFloat(cfg.aerialGuardKnockbackMultiplier, 0.0F, 1.0F),
                    clampDouble(cfg.windShearKnockback, 0.0D, 5.0D),
                    secClamped(cfg.windShearSlownessDurationSeconds, 0, 120),
                    clampInt(cfg.windShearSlownessAmplifier, 0, 10),
                    secClamped(cfg.windJumpCooldownSeconds, 0, 24 * 3600),
                    clampDouble(cfg.windJumpVerticalVelocity, 0.0D, 5.0D),
                    clampDouble(cfg.windJumpForwardVelocity, 0.0D, 5.0D),
                    secClamped(cfg.galeSlamCooldownSeconds, 0, 24 * 3600),
                    secClamped(cfg.galeSlamWindowSeconds, 0, 60),
                    clampInt(cfg.galeSlamRadiusBlocks, 0, 32),
                    clampFloat(cfg.galeSlamBonusDamage, 0.0F, 200.0F),
                    clampDouble(cfg.galeSlamKnockback, 0.0D, 5.0D),
                    secClamped(cfg.crosswindCooldownSeconds, 0, 24 * 3600),
                    clampInt(cfg.crosswindRangeBlocks, 1, 128),
                    clampInt(cfg.crosswindRadiusBlocks, 0, 32),
                    clampFloat(cfg.crosswindDamage, 0.0F, 200.0F),
                    clampDouble(cfg.crosswindKnockback, 0.0D, 5.0D),
                    secClamped(cfg.crosswindSlownessDurationSeconds, 0, 120),
                    clampInt(cfg.crosswindSlownessAmplifier, 0, 10),
                    secClamped(cfg.dashCooldownSeconds, 0, 24 * 3600),
                    clampDouble(cfg.dashVelocity, 0.0D, 5.0D),
                    clampDouble(cfg.dashUpVelocity, -1.0D, 5.0D),
                    secClamped(cfg.dashIFrameDurationSeconds, 0, 10),
                    clampInt(cfg.dashIFrameResistanceAmplifier, 0, 10),
                    clampInt(cfg.airMaceBreachLevel, 0, 10),
                    clampInt(cfg.airMaceWindBurstLevel, 0, 10),
                    clampInt(cfg.airMaceMendingLevel, 0, 1),
                    clampInt(cfg.airMaceUnbreakingLevel, 0, 10),
                    clampInt(cfg.airMaceFireAspectLevel, 0, 10)
            );
        }
    }

    public record Legendary(
            int craftTicks,
            int craftMaxPerItem,
            int craftMaxActivePerItem,
            int trackerRefreshTicks,
            int trackerMaxDistanceBlocks,
            int recallCooldownTicks,
            int hypnoHoldTicks,
            int hypnoRangeBlocks,
            int hypnoViewRangeBlocks,
            float hypnoHealHearts,
            int hypnoMaxControlled,
            int hypnoDurationTicks,
            int earthsplitterRadiusBlocks,
            int earthsplitterTunnelLengthBlocks,
            int bloodOathSharpnessCap,
            int demolitionCooldownTicks,
            int demolitionCooldownScalePercent,
            int demolitionFuseTicks,
            int demolitionRangeBlocks,
            float demolitionExplosionPower,
            int demolitionTntCount,
            int hunterAimRangeBlocks,
            int hunterAimTimeoutTicks,
            float hunterAimAssistStrength,
            int thirdStrikeWindowTicks,
            float thirdStrikeBonusDamage,
            float vampiricHealAmount,
            int supremeHelmetNightVisionAmplifier,
            int supremeHelmetWaterBreathingAmplifier,
            int supremeChestStrengthAmplifier,
            int supremeLeggingsFireResAmplifier,
            int supremeBootsSpeedAmplifier,
            int supremeSetResistanceAmplifier,
            Map<Identifier, GemId> recipeGemRequirements
    ) {
        static Legendary from(GemsBalanceConfig.Legendary cfg) {
            return new Legendary(
                    secClamped(cfg.craftSeconds, 1, 24 * 3600),
                    clampInt(cfg.craftMaxPerItem, 0, 100),
                    clampInt(cfg.craftMaxActivePerItem, 0, 100),
                    secClamped(cfg.trackerRefreshSeconds, 1, 60),
                    clampInt(cfg.trackerMaxDistanceBlocks, 0, 200000),
                    secClamped(cfg.recallCooldownSeconds, 0, 3600),
                    secClamped(cfg.hypnoHoldSeconds, 1, 30),
                    clampInt(cfg.hypnoRangeBlocks, 1, 64),
                    clampInt(cfg.hypnoViewRangeBlocks, 0, 32),
                    clampFloat(cfg.hypnoHealHearts, 0.0F, 200.0F),
                    clampInt(cfg.hypnoMaxControlled, 1, 64),
                    secClamped(cfg.hypnoDurationSeconds, 0, 24 * 3600),
                    clampInt(cfg.earthsplitterRadiusBlocks, 0, 2),
                    clampInt(cfg.earthsplitterTunnelLengthBlocks, 1, 32),
                    clampInt(cfg.bloodOathSharpnessCap, 1, 10),
                    secClamped(cfg.demolitionCooldownSeconds, 0, 3600),
                    clampInt(cfg.demolitionCooldownScalePercent, 0, 200),
                    clampInt(cfg.demolitionFuseTicks, 1, 200),
                    clampInt(cfg.demolitionRangeBlocks, 1, 128),
                    clampFloat(cfg.demolitionExplosionPower, 0.0F, 12.0F),
                    clampInt(cfg.demolitionTntCount, 0, 50),
                    clampInt(cfg.hunterAimRangeBlocks, 1, 256),
                    secClamped(cfg.hunterAimTimeoutSeconds, 0, 3600),
                    clampFloat(cfg.hunterAimAssistStrength, 0.0F, 1.0F),
                    secClamped(cfg.thirdStrikeWindowSeconds, 1, 60),
                    clampFloat(cfg.thirdStrikeBonusDamage, 0.0F, 40.0F),
                    clampFloat(cfg.vampiricHealAmount, 0.0F, 40.0F),
                    clampInt(cfg.supremeHelmetNightVisionAmplifier, 0, 10),
                    clampInt(cfg.supremeHelmetWaterBreathingAmplifier, 0, 10),
                    clampInt(cfg.supremeChestStrengthAmplifier, 0, 10),
                    clampInt(cfg.supremeLeggingsFireResAmplifier, 0, 10),
                    clampInt(cfg.supremeBootsSpeedAmplifier, 0, 10),
                    clampInt(cfg.supremeSetResistanceAmplifier, 0, 10),
                    parseRecipeGemRequirements(cfg.recipeGemRequirements)
            );
        }
    }

    public record Duelist(
            // Passives
            float riposteBonusDamageMultiplier,
            int riposteWindowTicks,
            float focusBonusDamageMultiplier,
            int focusRadiusBlocks,
            float combatStanceSpeedMultiplier,
            // Abilities
            int lungeCooldownTicks,
            int lungeDistanceBlocks,
            float lungeDamage,
            int parryCooldownTicks,
            int parryWindowTicks,
            int parryStunTicks,
            int flourishCooldownTicks,
            int flourishRadiusBlocks,
            float flourishDamage,
            int mirrorMatchCooldownTicks,
            int mirrorMatchDurationTicks,
            int mirrorMatchRangeBlocks,
            int bladeDanceCooldownTicks,
            int bladeDanceDurationTicks,
            float bladeDanceStartingMultiplier,
            float bladeDanceIncreasePerHit,
            float bladeDanceMaxMultiplier,
            int bladeDanceResetTicks
    ) {
        static Duelist from(GemsBalanceConfig.Duelist cfg) {
            return new Duelist(
                    clampFloat(cfg.riposteBonusDamageMultiplier, 1.0F, 5.0F),
                    secClamped(cfg.riposteWindowSeconds, 0, 60),
                    clampFloat(cfg.focusBonusDamageMultiplier, 1.0F, 5.0F),
                    clampInt(cfg.focusRadiusBlocks, 0, 64),
                    clampFloat(cfg.combatStanceSpeedMultiplier, 1.0F, 2.0F),
                    secClamped(cfg.lungeCooldownSeconds, 0, 3600),
                    clampInt(cfg.lungeDistanceBlocks, 1, 32),
                    clampFloat(cfg.lungeDamage, 0.0F, 200.0F),
                    secClamped(cfg.parryCooldownSeconds, 0, 3600),
                    clampInt(cfg.parryWindowTicks, 1, 100),
                    secClamped(cfg.parryStunSeconds, 0, 60),
                    secClamped(cfg.flourishCooldownSeconds, 0, 3600),
                    clampInt(cfg.flourishRadiusBlocks, 0, 32),
                    clampFloat(cfg.flourishDamage, 0.0F, 200.0F),
                    secClamped(cfg.mirrorMatchCooldownSeconds, 0, 3600),
                    secClamped(cfg.mirrorMatchDurationSeconds, 0, 120),
                    clampInt(cfg.mirrorMatchRangeBlocks, 0, 64),
                    secClamped(cfg.bladeDanceCooldownSeconds, 0, 3600),
                    secClamped(cfg.bladeDanceDurationSeconds, 0, 120),
                    clampFloat(cfg.bladeDanceStartingMultiplier, 0.0F, 5.0F),
                    clampFloat(cfg.bladeDanceIncreasePerHit, 0.0F, 1.0F),
                    clampFloat(cfg.bladeDanceMaxMultiplier, 1.0F, 10.0F),
                    secClamped(cfg.bladeDanceResetSeconds, 0, 60)
            );
        }
    }

    public record Hunter(
            // Passives
            float preyMarkBonusDamageMultiplier,
            int preyMarkDurationTicks,
            int trackersEyeRangeBlocks,
            int trophyHunterDurationTicks,
            // Abilities
            int huntingTrapCooldownTicks,
            int huntingTrapRootTicks,
            float huntingTrapDamage,
            int huntingTrapLifetimeTicks,
            int pounceCooldownTicks,
            int pounceRangeBlocks,
            float pounceDamage,
            int netShotCooldownTicks,
            int netShotSlowTicks,
            int netShotRangeBlocks,
            int cripplingCooldownTicks,
            float cripplingSlowMultiplier,
            int cripplingDurationTicks,
            int cripplingRangeBlocks,
            int packTacticsCooldownTicks,
            float packTacticsBonusDamageMultiplier,
            int packTacticsDurationTicks,
            int packTacticsRadiusBlocks
    ) {
        static Hunter from(GemsBalanceConfig.Hunter cfg) {
            return new Hunter(
                    clampFloat(cfg.preyMarkBonusDamageMultiplier, 1.0F, 5.0F),
                    secClamped(cfg.preyMarkDurationSeconds, 0, 600),
                    clampInt(cfg.trackersEyeRangeBlocks, 0, 128),
                    secClamped(cfg.trophyHunterDurationSeconds, 0, 600),
                    secClamped(cfg.huntingTrapCooldownSeconds, 0, 3600),
                    secClamped(cfg.huntingTrapRootSeconds, 0, 60),
                    clampFloat(cfg.huntingTrapDamage, 0.0F, 200.0F),
                    secClamped(cfg.huntingTrapLifetimeSeconds, 0, 600),
                    secClamped(cfg.pounceCooldownSeconds, 0, 3600),
                    clampInt(cfg.pounceRangeBlocks, 0, 64),
                    clampFloat(cfg.pounceDamage, 0.0F, 200.0F),
                    secClamped(cfg.netShotCooldownSeconds, 0, 3600),
                    secClamped(cfg.netShotSlowSeconds, 0, 60),
                    clampInt(cfg.netShotRangeBlocks, 0, 64),
                    secClamped(cfg.cripplingCooldownSeconds, 0, 3600),
                    clampFloat(cfg.cripplingSlowMultiplier, 0.0F, 1.0F),
                    secClamped(cfg.cripplingDurationSeconds, 0, 60),
                    clampInt(cfg.cripplingRangeBlocks, 0, 64),
                    secClamped(cfg.packTacticsCooldownSeconds, 0, 3600),
                    clampFloat(cfg.packTacticsBonusDamageMultiplier, 1.0F, 5.0F),
                    secClamped(cfg.packTacticsDurationSeconds, 0, 120),
                    clampInt(cfg.packTacticsRadiusBlocks, 0, 64)
            );
        }
    }

    public record Sentinel(
            // Passives
            float guardianAuraDamageReduction,
            int guardianAuraRadiusBlocks,
            int fortressStandStillTicks,
            int fortressResistanceAmplifier,
            float retributionThornsDamageMultiplier,
            // Abilities
            int shieldWallCooldownTicks,
            int shieldWallDurationTicks,
            int shieldWallWidthBlocks,
            int shieldWallHeightBlocks,
            int tauntCooldownTicks,
            int tauntDurationTicks,
            int tauntRadiusBlocks,
            float tauntDamageReduction,
            int interventionCooldownTicks,
            int interventionRangeBlocks,
            int rallyCryCooldownTicks,
            float rallyCryHealHearts,
            int rallyCryResistanceDurationTicks,
            int rallyCryRadiusBlocks,
            int lockdownCooldownTicks,
            int lockdownDurationTicks,
            int lockdownRadiusBlocks
    ) {
        static Sentinel from(GemsBalanceConfig.Sentinel cfg) {
            return new Sentinel(
                    clampFloat(cfg.guardianAuraDamageReduction, 0.0F, 1.0F),
                    clampInt(cfg.guardianAuraRadiusBlocks, 0, 64),
                    secClamped(cfg.fortressStandStillSeconds, 0, 60),
                    clampInt(cfg.fortressResistanceAmplifier, 0, 10),
                    clampFloat(cfg.retributionThornsDamageMultiplier, 0.0F, 1.0F),
                    secClamped(cfg.shieldWallCooldownSeconds, 0, 3600),
                    secClamped(cfg.shieldWallDurationSeconds, 0, 120),
                    clampInt(cfg.shieldWallWidthBlocks, 1, 16),
                    clampInt(cfg.shieldWallHeightBlocks, 1, 16),
                    secClamped(cfg.tauntCooldownSeconds, 0, 3600),
                    secClamped(cfg.tauntDurationSeconds, 0, 60),
                    clampInt(cfg.tauntRadiusBlocks, 0, 64),
                    clampFloat(cfg.tauntDamageReduction, 0.0F, 1.0F),
                    secClamped(cfg.interventionCooldownSeconds, 0, 3600),
                    clampInt(cfg.interventionRangeBlocks, 0, 64),
                    secClamped(cfg.rallyCryCooldownSeconds, 0, 3600),
                    clampFloat(cfg.rallyCryHealHearts, 0.0F, 200.0F),
                    secClamped(cfg.rallyCryResistanceDurationSeconds, 0, 120),
                    clampInt(cfg.rallyCryRadiusBlocks, 0, 64),
                    secClamped(cfg.lockdownCooldownSeconds, 0, 3600),
                    secClamped(cfg.lockdownDurationSeconds, 0, 120),
                    clampInt(cfg.lockdownRadiusBlocks, 0, 64)
            );
        }
    }

    public record Trickster(
            // Passives
            float sleightOfHandChance,
            float slipperyChance,
            // Abilities
            int shadowSwapCooldownTicks,
            int shadowSwapCloneDurationTicks,
            int mirageCooldownTicks,
            int mirageDurationTicks,
            int mirageCloneCount,
            int glitchStepCooldownTicks,
            int glitchStepDistanceBlocks,
            float glitchStepAfterimgDamage,
            int glitchStepAfterimgRadiusBlocks,
            int puppetMasterCooldownTicks,
            int puppetMasterDurationTicks,
            int puppetMasterRangeBlocks,
            int mindGamesCooldownTicks,
            int mindGamesDurationTicks,
            int mindGamesRangeBlocks
    ) {
        static Trickster from(GemsBalanceConfig.Trickster cfg) {
            return new Trickster(
                    clampFloat(cfg.sleightOfHandChance, 0.0F, 1.0F),
                    clampFloat(cfg.slipperyChance, 0.0F, 1.0F),
                    secClamped(cfg.shadowSwapCooldownSeconds, 0, 3600),
                    secClamped(cfg.shadowSwapCloneDurationSeconds, 0, 600),
                    secClamped(cfg.mirageCooldownSeconds, 0, 3600),
                    secClamped(cfg.mirageDurationSeconds, 0, 120),
                    clampInt(cfg.mirageCloneCount, 0, 16),
                    secClamped(cfg.glitchStepCooldownSeconds, 0, 3600),
                    clampInt(cfg.glitchStepDistanceBlocks, 0, 32),
                    clampFloat(cfg.glitchStepAfterimgDamage, 0.0F, 200.0F),
                    clampInt(cfg.glitchStepAfterimgRadiusBlocks, 0, 16),
                    secClamped(cfg.puppetMasterCooldownSeconds, 0, 3600),
                    secClamped(cfg.puppetMasterDurationSeconds, 0, 60),
                    clampInt(cfg.puppetMasterRangeBlocks, 0, 64),
                    secClamped(cfg.mindGamesCooldownSeconds, 0, 3600),
                    secClamped(cfg.mindGamesDurationSeconds, 0, 60),
                    clampInt(cfg.mindGamesRangeBlocks, 0, 64)
            );
        }
    }

    public record BonusPool(
            int maxBonusAbilities,
            int maxBonusPassives,
            boolean showBonusesInHud,
            float bonusAbilityCooldownMultiplier,
            float bonusAbilityDamageMultiplier,
            float bonusPassiveEffectMultiplier
    ) {
        static BonusPool from(GemsBalanceConfig.BonusPool cfg) {
            return new BonusPool(
                    clampInt(cfg.maxBonusAbilities, 0, 10),
                    clampInt(cfg.maxBonusPassives, 0, 10),
                    cfg.showBonusesInHud,
                    clampFloat(cfg.bonusAbilityCooldownMultiplier, 0.0F, 10.0F),
                    clampFloat(cfg.bonusAbilityDamageMultiplier, 0.0F, 10.0F),
                    clampFloat(cfg.bonusPassiveEffectMultiplier, 0.0F, 10.0F)
            );
        }
    }

    private static List<Identifier> parseIdentifierList(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        List<Identifier> out = new ArrayList<>();
        for (String entry : raw) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            Identifier id = Identifier.tryParse(entry);
            if (id != null) {
                out.add(id);
            }
        }
        return List.copyOf(out);
    }

    private static Map<Identifier, GemId> parseRecipeGemRequirements(Map<String, String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<Identifier, GemId> out = new HashMap<>();
        for (var entry : raw.entrySet()) {
            if (entry == null) {
                continue;
            }
            Identifier recipeId = Identifier.tryParse(entry.getKey());
            if (recipeId == null) {
                continue;
            }
            String gemRaw = entry.getValue();
            if (gemRaw == null || gemRaw.isBlank()) {
                continue;
            }
            try {
                GemId gem = GemId.valueOf(gemRaw.trim().toUpperCase());
                out.put(recipeId, gem);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return Map.copyOf(out);
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
