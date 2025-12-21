package com.feel.gems.config;

/**
 * User-editable balancing config. Units:
 * - durations/cooldowns are in seconds unless explicitly noted
 * - radii/ranges are in blocks
 * - damage is in half-hearts (Minecraft health points) where 1.0 = half-heart, 2.0 = 1 heart
 */
public final class GemsBalanceConfig {
    public Visual visual = new Visual();

    public Astra astra = new Astra();
    public Fire fire = new Fire();
    public Flux flux = new Flux();
    public Life life = new Life();
    public Puff puff = new Puff();
    public Speed speed = new Speed();
    public Strength strength = new Strength();
    public Wealth wealth = new Wealth();
    public Terror terror = new Terror();
    public Summoner summoner = new Summoner();
    public Space space = new Space();
    public Reaper reaper = new Reaper();
    public Pillager pillager = new Pillager();
    public SpyMimic spyMimic = new SpyMimic();
    public Beacon beacon = new Beacon();
    public Air air = new Air();

    public static final class Visual {
        public boolean enableParticles = true;
        public boolean enableSounds = true;

        /**
         * Scales particle counts for all abilities (0 = off, 100 = default, 200 = double).
         * This is a global safety knob for large servers.
         */
        public int particleScalePercent = 100;

        /**
         * Hard cap for a single server particle packet.
         */
        public int maxParticlesPerCall = 128;

        /**
         * Hard cap for beam particle steps (used by Flux Beam and similar effects).
         */
        public int maxBeamSteps = 256;

        /**
         * Hard cap for ring/aura particle points (used by campfire/zone-style abilities).
         */
        public int maxRingPoints = 128;
    }

    public static final class Astra {
        public int shadowAnchorWindowSeconds = 10;
        public int shadowAnchorPostCooldownSeconds = 10;

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
        public int meteorShowerExplosionPower = 2;
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
        public int chargeEmeraldBlock = 20;
        public int chargeAmethystBlock = 10;
        public int chargeNetheriteScrap = 35;
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
        public double momentumMinSpeed = 0.10D;
        public double momentumMaxSpeed = 0.60D;
        public float momentumMinMultiplier = 0.90F;
        public float momentumMaxMultiplier = 1.30F;
        public int frictionlessSpeedAmplifier = 1;

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

        public int slipstreamCooldownSeconds = 45;
        public int slipstreamDurationSeconds = 8;
        public int slipstreamLengthBlocks = 18;
        public int slipstreamRadiusBlocks = 3;
        public int slipstreamAllySpeedAmplifier = 1;
        public int slipstreamEnemySlownessAmplifier = 1;
        public double slipstreamEnemyKnockback = 0.35D;

        public int afterimageCooldownSeconds = 30;
        public int afterimageDurationSeconds = 6;
        public int afterimageSpeedAmplifier = 1;
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

    public static final class Terror {
        // Passives
        public int dreadAuraRadiusBlocks = 10;
        public int dreadAuraAmplifier = 0;
        public int bloodPriceDurationSeconds = 6;
        public int bloodPriceStrengthAmplifier = 1;
        public int bloodPriceResistanceAmplifier = 0;

        // Abilities
        public int terrorTradeCooldownSeconds = 180;
        public int terrorTradeRangeBlocks = 30;
        public int terrorTradeMaxUses = 3;
        public int terrorTradeHeartsCost = 2;
        public int terrorTradePermanentEnergyPenalty = 2;

        public int panicRingCooldownSeconds = 60;
        public int panicRingTntCount = 5;
        public int panicRingFuseTicks = 50;
        public double panicRingRadiusBlocks = 1.6D;
    }

    public static final class Summoner {
        public int maxPoints = 50;
        public int maxActiveSummons = 20;
        public int summonLifetimeSeconds = 120;
        public int commandRangeBlocks = 32;

        public int summonSlotCooldownSeconds = 30;
        public int recallCooldownSeconds = 20;

        public int commandersMarkDurationSeconds = 3;
        public int commandersMarkStrengthAmplifier = 0;

        public float summonBonusHealth = 4.0F;

        /**
         * Entity cost map, keyed by entity id string (e.g. "minecraft:zombie").
         */
        public java.util.Map<String, Integer> costs = defaultCosts();

        /**
         * Slot loadouts; each slot is a list of entity specs to spawn when the corresponding ability is used.
         */
        public java.util.List<SummonSpec> slot1 = java.util.List.of(new SummonSpec("minecraft:zombie", 2));
        public java.util.List<SummonSpec> slot2 = java.util.List.of(new SummonSpec("minecraft:skeleton", 2));
        public java.util.List<SummonSpec> slot3 = java.util.List.of(new SummonSpec("minecraft:creeper", 1));
        public java.util.List<SummonSpec> slot4 = java.util.List.of();
        public java.util.List<SummonSpec> slot5 = java.util.List.of();

        public static final class SummonSpec {
            public String entityId;
            public int count;

            public SummonSpec() {
            }

            public SummonSpec(String entityId, int count) {
                this.entityId = entityId;
                this.count = count;
            }
        }

        private static java.util.Map<String, Integer> defaultCosts() {
            java.util.Map<String, Integer> m = new java.util.HashMap<>();
            m.put("minecraft:zombie", 5);
            m.put("minecraft:skeleton", 5);
            m.put("minecraft:creeper", 10);
            m.put("minecraft:spider", 6);
            m.put("minecraft:cave_spider", 8);
            m.put("minecraft:husk", 6);
            m.put("minecraft:drowned", 8);
            m.put("minecraft:stray", 6);
            m.put("minecraft:piglin", 8);
            m.put("minecraft:piglin_brute", 25);
            m.put("minecraft:enderman", 20);
            m.put("minecraft:wolf", 8);
            m.put("minecraft:iron_golem", 40);
            m.put("minecraft:vindicator", 18);
            m.put("minecraft:pillager", 12);
            m.put("minecraft:ravager", 45);
            m.put("minecraft:witch", 14);
            m.put("minecraft:blaze", 14);
            m.put("minecraft:wither_skeleton", 18);
            m.put("minecraft:guardian", 18);
            m.put("minecraft:hoglin", 16);
            m.put("minecraft:zoglin", 22);
            m.put("minecraft:slime", 8);
            m.put("minecraft:magma_cube", 10);
            m.put("minecraft:snow_golem", 10);
            return m;
        }
    }

    public static final class Space {
        // Passives
        public float lunarMinMultiplier = 0.85F;
        public float lunarMaxMultiplier = 1.20F;
        public float starshieldProjectileDamageMultiplier = 0.80F;

        // Abilities
        public int orbitalLaserCooldownSeconds = 60;
        public int orbitalLaserRangeBlocks = 64;
        public int orbitalLaserDelaySeconds = 1;
        public int orbitalLaserRadiusBlocks = 4;
        public float orbitalLaserDamage = 10.0F;

        public int orbitalLaserMiningRadiusBlocks = 2;
        public float orbitalLaserMiningHardnessCap = 60.0F;
        public int orbitalLaserMiningMaxBlocks = 64;

        public int gravityFieldCooldownSeconds = 45;
        public int gravityFieldDurationSeconds = 10;
        public int gravityFieldRadiusBlocks = 10;
        public float gravityFieldAllyGravityMultiplier = 0.75F;
        public float gravityFieldEnemyGravityMultiplier = 1.25F;

        public int blackHoleCooldownSeconds = 60;
        public int blackHoleDurationSeconds = 6;
        public int blackHoleRadiusBlocks = 8;
        public float blackHolePullStrength = 0.10F;
        public float blackHoleDamagePerSecond = 2.0F;

        public int whiteHoleCooldownSeconds = 60;
        public int whiteHoleDurationSeconds = 6;
        public int whiteHoleRadiusBlocks = 8;
        public float whiteHolePushStrength = 0.12F;
        public float whiteHoleDamagePerSecond = 1.0F;
    }

    public static final class Reaper {
        // Passives
        public float undeadWardDamageMultiplier = 0.80F;
        public int harvestRegenDurationSeconds = 4;
        public int harvestRegenAmplifier = 0;

        // Abilities
        public int graveSteedCooldownSeconds = 60;
        public int graveSteedDurationSeconds = 30;
        public float graveSteedDecayDamagePerSecond = 1.0F;

        public int witheringStrikesCooldownSeconds = 45;
        public int witheringStrikesDurationSeconds = 10;
        public int witheringStrikesWitherDurationSeconds = 4;
        public int witheringStrikesWitherAmplifier = 0;

        public int deathOathCooldownSeconds = 60;
        public int deathOathDurationSeconds = 12;
        public int deathOathRangeBlocks = 48;
        public float deathOathSelfDamagePerSecond = 1.0F;

        public int scytheSweepCooldownSeconds = 20;
        public int scytheSweepRangeBlocks = 5;
        public int scytheSweepArcDegrees = 110;
        public float scytheSweepDamage = 7.0F;
        public double scytheSweepKnockback = 0.55D;

        public int bloodChargeCooldownSeconds = 60;
        public int bloodChargeMaxChargeSeconds = 8;
        public float bloodChargeSelfDamagePerSecond = 1.0F;
        public float bloodChargeMaxMultiplier = 1.60F;
        public int bloodChargeBuffDurationSeconds = 8;

        public int shadeCloneCooldownSeconds = 90;
        public int shadeCloneDurationSeconds = 12;
        public float shadeCloneMaxHealth = 20.0F;
    }

    public static final class Pillager {
        // Passives
        public float raidersTrainingProjectileVelocityMultiplier = 1.15F;
        public int shieldbreakerDisableCooldownTicks = 80;
        public float illagerDisciplineThresholdHearts = 4.0F;
        public int illagerDisciplineResistanceDurationSeconds = 4;
        public int illagerDisciplineResistanceAmplifier = 0;
        public int illagerDisciplineCooldownSeconds = 45;

        // Abilities
        public int fangsCooldownSeconds = 25;
        public int fangsRangeBlocks = 24;
        public int fangsCount = 16;
        public float fangsSpacingBlocks = 1.25F;
        public int fangsWarmupStepTicks = 2;

        public int ravageCooldownSeconds = 20;
        public int ravageRangeBlocks = 6;
        public float ravageDamage = 6.0F;
        public double ravageKnockback = 1.25D;

        public int vindicatorBreakCooldownSeconds = 35;
        public int vindicatorBreakDurationSeconds = 8;
        public int vindicatorBreakStrengthAmplifier = 0;
        public int vindicatorBreakShieldDisableCooldownTicks = 100;

        public int volleyCooldownSeconds = 45;
        public int volleyDurationSeconds = 3;
        public int volleyPeriodTicks = 10;
        public int volleyArrowsPerShot = 1;
        public float volleyArrowDamage = 4.0F;
        public float volleyArrowVelocity = 3.0F;
        public float volleyArrowInaccuracy = 1.0F;
    }

    public static final class SpyMimic {
        // Passives
        public int stillnessSeconds = 5;
        public float stillnessMoveEpsilonBlocks = 0.05F;
        public int stillnessInvisRefreshSeconds = 2;

        // Observation
        public int observeRangeBlocks = 24;
        public int observeWindowSeconds = 10 * 60;
        public int stealRequiredWitnessCount = 4;
        public int maxStolenAbilities = 3;

        // Abilities
        public int mimicFormCooldownSeconds = 60;
        public int mimicFormDurationSeconds = 12;
        public float mimicFormBonusMaxHealth = 4.0F;
        public float mimicFormSpeedMultiplier = 1.10F;

        public int echoCooldownSeconds = 25;
        public int echoWindowSeconds = 8;

        public int stealCooldownSeconds = 60;

        public int smokeBombCooldownSeconds = 30;
        public int smokeBombRadiusBlocks = 8;
        public int smokeBombDurationSeconds = 6;
        public int smokeBombBlindnessAmplifier = 0;
        public int smokeBombSlownessAmplifier = 0;

        public int stolenCastCooldownSeconds = 20;
    }

    public static final class Beacon {
        // Passives
        public int coreRadiusBlocks = 8;
        public int corePulsePeriodSeconds = 2;
        public int coreRegenDurationSeconds = 3;
        public int coreRegenAmplifier = 0;

        public int stabilizeRadiusBlocks = 8;
        public int stabilizeReduceTicksPerSecond = 20;

        public int rallyRadiusBlocks = 10;
        public int rallyAbsorptionHearts = 4;
        public int rallyDurationSeconds = 8;

        // Abilities (auras)
        public int auraCooldownSeconds = 30;
        public int auraDurationSeconds = 12;
        public int auraRadiusBlocks = 10;
        public int auraRefreshSeconds = 2;
        public int auraSpeedAmplifier = 1;
        public int auraHasteAmplifier = 1;
        public int auraResistanceAmplifier = 0;
        public int auraJumpAmplifier = 1;
        public int auraStrengthAmplifier = 0;
        public int auraRegenAmplifier = 0;
    }

    public static final class Air {
        // Passives
        public float aerialGuardFallDamageMultiplier = 0.50F;
        public float aerialGuardKnockbackMultiplier = 0.60F;
        public int skybornDurationSeconds = 3;
        public int skybornCooldownSeconds = 20;

        // Abilities
        public int windJumpCooldownSeconds = 8;
        public double windJumpVerticalVelocity = 1.0D;
        public double windJumpForwardVelocity = 0.2D;

        public int galeSlamCooldownSeconds = 30;
        public int galeSlamWindowSeconds = 8;
        public int galeSlamRadiusBlocks = 4;
        public float galeSlamBonusDamage = 6.0F;
        public double galeSlamKnockback = 1.0D;

        public int updraftZoneCooldownSeconds = 25;
        public int updraftZoneRadiusBlocks = 8;
        public double updraftZoneUpVelocity = 0.9D;
        public float updraftZoneEnemyDamage = 3.0F;
        public double updraftZoneEnemyKnockback = 0.6D;

        public int dashCooldownSeconds = 6;
        public double dashVelocity = 1.6D;
        public double dashUpVelocity = 0.1D;
        public int dashIFrameDurationSeconds = 1;
        public int dashIFrameResistanceAmplifier = 4;
    }
}
