package com.blissmc.gems.config;

/**
 * User-editable balancing config. Units:
 * - durations/cooldowns are in seconds unless explicitly noted
 * - radii/ranges are in blocks
 * - damage is in half-hearts (Minecraft health points) where 1.0 = half-heart, 2.0 = 1 heart
 */
public final class GemsBalanceConfig {
    public Astra astra = new Astra();
    public Fire fire = new Fire();
    public Flux flux = new Flux();
    public Life life = new Life();
    public Puff puff = new Puff();
    public Speed speed = new Speed();
    public Strength strength = new Strength();
    public Wealth wealth = new Wealth();

    public static final class Astra {
        public int shadowAnchorWindowSeconds = 10;

        public int dimensionalVoidCooldownSeconds = 60;
        public int dimensionalVoidDurationSeconds = 8;
        public int dimensionalVoidRadiusBlocks = 10;

        public int astralDaggersCooldownSeconds = 8;
        public int astralDaggersCount = 5;
        public float astralDaggersDamage = 4.0F;
        public float astralDaggersVelocity = 3.5F;
        public float astralDaggersSpread = 0.05F;

        public int unboundedCooldownSeconds = 60;
        public int unboundedDurationSeconds = 3;

        public int astralCameraCooldownSeconds = 60;
        public int astralCameraDurationSeconds = 8;

        public int spookCooldownSeconds = 30;
        public int spookRadiusBlocks = 10;
        public int spookDurationSeconds = 6;

        public int tagCooldownSeconds = 20;
        public int tagRangeBlocks = 30;
        public int tagDurationSeconds = 12;
    }

    public static final class Fire {
        public int cosyCampfireCooldownSeconds = 45;
        public int cosyCampfireDurationSeconds = 10;
        public int cosyCampfireRadiusBlocks = 8;
        public int cosyCampfireRegenAmplifier = 3; // Regen IV = amplifier 3

        public int heatHazeCooldownSeconds = 90;
        public int heatHazeDurationSeconds = 10;
        public int heatHazeRadiusBlocks = 10;
        public int heatHazeEnemyMiningFatigueAmplifier = 0;
        public int heatHazeEnemyWeaknessAmplifier = 0;

        public int fireballChargeUpSeconds = 3;
        public int fireballChargeDownSeconds = 3;
        public int fireballInternalCooldownSeconds = 4;
        public int fireballMaxDistanceBlocks = 60;

        public int meteorShowerCooldownSeconds = 120;
        public int meteorShowerCount = 10;
        public int meteorShowerSpreadBlocks = 10;
        public int meteorShowerHeightBlocks = 25;
        public float meteorShowerVelocity = 1.5F;
    }

    public static final class Flux {
        public int fluxBeamCooldownSeconds = 4;
        public int fluxBeamRangeBlocks = 60;
        public float fluxBeamMinDamage = 6.0F;
        public float fluxBeamMaxDamageAt100 = 12.0F;
        public float fluxBeamMaxDamageAt200 = 24.0F;
        public int fluxBeamArmorDamageAt100 = 200;
        public int fluxBeamArmorDamagePerPercent = 2;

        public int staticBurstCooldownSeconds = 30;
        public int staticBurstRadiusBlocks = 8;
        public float staticBurstMaxDamage = 20.0F;
        public int staticBurstStoreWindowSeconds = 120;

        public int chargeDiamondBlock = 25;
        public int chargeGoldBlock = 15;
        public int chargeCopperBlock = 5;
        public int chargeEnchantedDiamondItem = 20;

        public int overchargeDelaySeconds = 5;
        public int overchargePerSecond = 5;
        public float overchargeSelfDamagePerSecond = 1.0F;
    }

    public static final class Life {
        public int vitalityVortexCooldownSeconds = 30;
        public int vitalityVortexRadiusBlocks = 8;
        public int vitalityVortexDurationSeconds = 8;
        public int vitalityVortexScanRadiusBlocks = 3;
        public int vitalityVortexVerdantThreshold = 10;
        public float vitalityVortexAllyHeal = 2.0F;

        public int healthDrainCooldownSeconds = 12;
        public int healthDrainRangeBlocks = 20;
        public float healthDrainAmount = 6.0F;

        public int lifeCircleCooldownSeconds = 60;
        public int lifeCircleDurationSeconds = 12;
        public int lifeCircleRadiusBlocks = 8;
        public double lifeCircleMaxHealthDelta = 8.0D;

        public int heartLockCooldownSeconds = 45;
        public int heartLockDurationSeconds = 6;
        public int heartLockRangeBlocks = 20;
    }

    public static final class Puff {
        public int doubleJumpCooldownSeconds = 2;
        public double doubleJumpVelocityY = 0.85D;

        public int dashCooldownSeconds = 6;
        public double dashVelocity = 1.8D;
        public float dashDamage = 6.0F;
        public double dashHitRangeBlocks = 4.0D;

        public int breezyBashCooldownSeconds = 20;
        public int breezyBashRangeBlocks = 10;
        public double breezyBashUpVelocityY = 1.2D;
        public double breezyBashKnockback = 0.6D;
        public float breezyBashInitialDamage = 4.0F;
        public float breezyBashImpactDamage = 6.0F;
        public int breezyBashImpactWindowSeconds = 6;

        public int groupBashCooldownSeconds = 45;
        public int groupBashRadiusBlocks = 10;
        public double groupBashKnockback = 1.2D;
        public double groupBashUpVelocityY = 0.8D;
    }

    public static final class Speed {
        public int arcShotCooldownSeconds = 20;
        public int arcShotRangeBlocks = 40;
        public double arcShotRadiusBlocks = 2.0D;
        public int arcShotMaxTargets = 3;
        public float arcShotDamage = 5.0F;

        public int speedStormCooldownSeconds = 60;
        public int speedStormDurationSeconds = 8;
        public int speedStormRadiusBlocks = 10;
        public int speedStormAllySpeedAmplifier = 1;
        public int speedStormAllyHasteAmplifier = 1;
        public int speedStormEnemySlownessAmplifier = 6;
        public int speedStormEnemyMiningFatigueAmplifier = 2;

        public int terminalVelocityCooldownSeconds = 30;
        public int terminalVelocityDurationSeconds = 10;
        public int terminalVelocitySpeedAmplifier = 2; // Speed III
        public int terminalVelocityHasteAmplifier = 1; // Haste II
    }

    public static final class Strength {
        public int nullifyCooldownSeconds = 20;
        public int nullifyRadiusBlocks = 10;

        public int frailerCooldownSeconds = 20;
        public int frailerRangeBlocks = 20;
        public int frailerDurationSeconds = 8;

        public int bountyCooldownSeconds = 60;
        public int bountyDurationSeconds = 60;

        public int chadCooldownSeconds = 90;
        public int chadDurationSeconds = 45;
        public int chadEveryHits = 4;
        public float chadBonusDamage = 7.0F;
    }

    public static final class Wealth {
        public int fumbleCooldownSeconds = 30;
        public int fumbleDurationSeconds = 8;
        public int fumbleRadiusBlocks = 10;

        public int hotbarLockCooldownSeconds = 30;
        public int hotbarLockDurationSeconds = 6;
        public int hotbarLockRangeBlocks = 20;

        public int amplificationCooldownSeconds = 180;
        public int amplificationDurationSeconds = 45;

        public int richRushCooldownSeconds = 9 * 60;
        public int richRushDurationSeconds = 3 * 60;
    }
}

