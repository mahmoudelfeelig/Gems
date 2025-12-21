package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTime;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

public final class AbilityRuntime {
    private static final String KEY_CAMPFIRE_UNTIL = "cosyCampfireUntil";
    private static final String KEY_HEAT_HAZE_UNTIL = "heatHazeUntil";
    private static final String KEY_SPEED_STORM_UNTIL = "speedStormUntil";
    private static final String KEY_SPACE_GRAVITY_UNTIL = "spaceGravityUntil";
    private static final String KEY_SPACE_GRAVITY_CASTER = "spaceGravityCaster";

    private static final String KEY_UNBOUNDED_UNTIL = "unboundedUntil";
    private static final String KEY_UNBOUNDED_PREV_GAMEMODE = "unboundedPrevGamemode";

    private static final String KEY_ASTRAL_CAMERA_UNTIL = "astralCameraUntil";
    private static final String KEY_ASTRAL_CAMERA_PREV_GAMEMODE = "astralCameraPrevGamemode";
    private static final String KEY_ASTRAL_CAMERA_RETURN_DIM = "astralCameraReturnDim";
    private static final String KEY_ASTRAL_CAMERA_RETURN_POS = "astralCameraReturnPos";
    private static final String KEY_ASTRAL_CAMERA_RETURN_YAW = "astralCameraReturnYaw";
    private static final String KEY_ASTRAL_CAMERA_RETURN_PITCH = "astralCameraReturnPitch";

    private static final String KEY_LIFE_CIRCLE_UNTIL = "lifeCircleUntil";
    private static final String KEY_LIFE_CIRCLE_CASTER = "lifeCircleCaster";

    private static final String KEY_HEART_LOCK_UNTIL = "heartLockUntil";
    private static final String KEY_HEART_LOCK_CASTER = "heartLockCaster";
    private static final String KEY_HEART_LOCK_LOCKED_MAX = "heartLockLockedMax";

    private static final String KEY_BOUNTY_UNTIL = "bountyUntil";
    private static final String KEY_BOUNTY_TARGET = "bountyTarget";

    private static final String KEY_CHAD_UNTIL = "chadUntil";
    private static final String KEY_CHAD_HITS = "chadHits";

    private static final String KEY_RICH_RUSH_UNTIL = "richRushUntil";

    private static final String KEY_AMPLIFICATION_UNTIL = "amplificationUntil";

    // Reaper runtime state
    private static final String KEY_REAPER_WITHERING_UNTIL = "reaperWitheringUntil";
    private static final String KEY_REAPER_OATH_UNTIL = "reaperDeathOathUntil";
    private static final String KEY_REAPER_OATH_TARGET = "reaperDeathOathTarget";
    private static final String KEY_REAPER_STEED_UNTIL = "reaperGraveSteedUntil";
    private static final String KEY_REAPER_STEED_UUID = "reaperGraveSteedUuid";
    private static final String KEY_REAPER_CLONE_UNTIL = "reaperShadeCloneUntil";
    private static final String KEY_REAPER_CLONE_UUID = "reaperShadeCloneUuid";

    private static final String CUSTOM_DATA_KEY_AMPLIFY = "gemsAmplify";
    private static final String CUSTOM_DATA_KEY_OWNER = "gemsOwner";

    private AbilityRuntime() {
    }

    public static void tickEverySecond(ServerPlayerEntity player) {
        long now = GemsTime.now(player);

        FireballAbility.tickCharging(player, now);
        ShadowAnchorAbility.tick(player, now);
        tickCosyCampfire(player, now);
        tickHeatHazeZone(player, now);
        tickSpeedStorm(player, now);
        tickSpaceGravityField(player, now);
        tickLifeCircle(player, now);
        tickHeartLock(player, now);
        tickUnbounded(player, now);
        tickAstralCamera(player, now);
        tickBounty(player, now);
        tickAmplificationCleanup(player, now);
        tickReaperGraveSteed(player, now);
        tickReaperDeathOath(player, now);
        tickReaperBloodChargeCharging(player, now);
        tickReaperShadeClone(player, now);
    }

    public static void startCosyCampfire(ServerPlayerEntity player, int durationTicks) {
        persistent(player).putLong(KEY_CAMPFIRE_UNTIL, GemsTime.now(player) + durationTicks);
    }

    public static void startHeatHazeZone(ServerPlayerEntity player, int durationTicks) {
        persistent(player).putLong(KEY_HEAT_HAZE_UNTIL, GemsTime.now(player) + durationTicks);
    }

    public static void startSpeedStorm(ServerPlayerEntity player, int durationTicks) {
        persistent(player).putLong(KEY_SPEED_STORM_UNTIL, GemsTime.now(player) + durationTicks);
    }

    public static void startSpaceGravityField(ServerPlayerEntity player, int durationTicks) {
        NbtCompound nbt = persistent(player);
        nbt.putLong(KEY_SPACE_GRAVITY_UNTIL, GemsTime.now(player) + durationTicks);
        nbt.putUuid(KEY_SPACE_GRAVITY_CASTER, player.getUuid());
    }

    public static void startReaperWitheringStrikes(ServerPlayerEntity player, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        persistent(player).putLong(KEY_REAPER_WITHERING_UNTIL, GemsTime.now(player) + durationTicks);
    }

    public static boolean isReaperWitheringStrikesActive(ServerPlayerEntity player, long now) {
        long until = persistent(player).getLong(KEY_REAPER_WITHERING_UNTIL);
        return until > now;
    }

    public static void startReaperDeathOath(ServerPlayerEntity player, UUID target, int durationTicks) {
        if (durationTicks <= 0 || target == null) {
            return;
        }
        NbtCompound nbt = persistent(player);
        nbt.putLong(KEY_REAPER_OATH_UNTIL, GemsTime.now(player) + durationTicks);
        nbt.putUuid(KEY_REAPER_OATH_TARGET, target);
    }

    public static UUID reaperDeathOathTarget(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        if (!nbt.contains(KEY_REAPER_OATH_TARGET, NbtElement.INT_ARRAY_TYPE)) {
            return null;
        }
        return nbt.getUuid(KEY_REAPER_OATH_TARGET);
    }

    public static void clearReaperDeathOath(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        nbt.remove(KEY_REAPER_OATH_UNTIL);
        nbt.remove(KEY_REAPER_OATH_TARGET);
    }

    public static void startReaperGraveSteed(ServerPlayerEntity player, UUID steedUuid, int durationTicks) {
        if (durationTicks <= 0 || steedUuid == null) {
            return;
        }
        NbtCompound nbt = persistent(player);
        nbt.putLong(KEY_REAPER_STEED_UNTIL, GemsTime.now(player) + durationTicks);
        nbt.putUuid(KEY_REAPER_STEED_UUID, steedUuid);
    }

    public static void startReaperShadeClone(ServerPlayerEntity player, UUID cloneUuid, int durationTicks) {
        if (durationTicks <= 0 || cloneUuid == null) {
            return;
        }
        NbtCompound nbt = persistent(player);
        nbt.putLong(KEY_REAPER_CLONE_UNTIL, GemsTime.now(player) + durationTicks);
        nbt.putUuid(KEY_REAPER_CLONE_UUID, cloneUuid);
    }

    public static void startReaperBloodChargeCharging(ServerPlayerEntity player, int maxChargeTicks) {
        if (maxChargeTicks <= 0) {
            return;
        }
        NbtCompound nbt = persistent(player);
        nbt.putLong(ReaperBloodCharge.KEY_CHARGING_UNTIL, GemsTime.now(player) + maxChargeTicks);
        nbt.putInt(ReaperBloodCharge.KEY_CHARGED_TICKS, 0);
    }

    public static boolean finishReaperBloodChargeCharging(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        long until = nbt.getLong(ReaperBloodCharge.KEY_CHARGING_UNTIL);
        if (until <= 0) {
            return false;
        }
        int charged = nbt.getInt(ReaperBloodCharge.KEY_CHARGED_TICKS);
        int maxCharge = GemsBalance.v().reaper().bloodChargeMaxChargeTicks();
        float mult = ReaperBloodCharge.computeMultiplier(charged, Math.max(1, maxCharge));
        int buffDuration = GemsBalance.v().reaper().bloodChargeBuffDurationTicks();
        ReaperBloodCharge.setBuff(player, mult, buffDuration);
        ReaperBloodCharge.clearCharging(player);
        return true;
    }

    public static void startUnbounded(ServerPlayerEntity player, int durationTicks) {
        NbtCompound nbt = persistent(player);
        nbt.putLong(KEY_UNBOUNDED_UNTIL, GemsTime.now(player) + durationTicks);
        nbt.putString(KEY_UNBOUNDED_PREV_GAMEMODE, player.interactionManager.getGameMode().getName());
        player.changeGameMode(GameMode.SPECTATOR);
    }

    public static void startAstralCamera(ServerPlayerEntity player, int durationTicks) {
        NbtCompound nbt = persistent(player);
        nbt.putLong(KEY_ASTRAL_CAMERA_UNTIL, GemsTime.now(player) + durationTicks);
        nbt.putString(KEY_ASTRAL_CAMERA_PREV_GAMEMODE, player.interactionManager.getGameMode().getName());
        nbt.putString(KEY_ASTRAL_CAMERA_RETURN_DIM, player.getWorld().getRegistryKey().getValue().toString());
        nbt.put(KEY_ASTRAL_CAMERA_RETURN_POS, NbtHelper.fromBlockPos(player.getBlockPos()));
        nbt.putFloat(KEY_ASTRAL_CAMERA_RETURN_YAW, player.getYaw());
        nbt.putFloat(KEY_ASTRAL_CAMERA_RETURN_PITCH, player.getPitch());
        player.changeGameMode(GameMode.SPECTATOR);
    }

    public static void startLifeCircle(ServerPlayerEntity caster, int durationTicks) {
        NbtCompound nbt = persistent(caster);
        nbt.putLong(KEY_LIFE_CIRCLE_UNTIL, GemsTime.now(caster) + durationTicks);
        nbt.putUuid(KEY_LIFE_CIRCLE_CASTER, caster.getUuid());
    }

    public static void startHeartLock(ServerPlayerEntity caster, ServerPlayerEntity target, int durationTicks) {
        NbtCompound nbt = persistent(target);
        nbt.putLong(KEY_HEART_LOCK_UNTIL, GemsTime.now(target) + durationTicks);
        nbt.putUuid(KEY_HEART_LOCK_CASTER, caster.getUuid());

        double currentMax = target.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
        double lockedMax = Math.max(2.0D, target.getHealth());
        nbt.putFloat(KEY_HEART_LOCK_LOCKED_MAX, (float) lockedMax);
        double delta = lockedMax - currentMax;
        Identifier modifierId = heartLockModifierId(caster.getUuid());
        applyMaxHealthModifier(target, modifierId, delta);
    }

    public static void startBounty(ServerPlayerEntity hunter, UUID target, int durationTicks) {
        NbtCompound nbt = persistent(hunter);
        nbt.putLong(KEY_BOUNTY_UNTIL, GemsTime.now(hunter) + durationTicks);
        nbt.putUuid(KEY_BOUNTY_TARGET, target);
    }

    public static void startChadStrength(ServerPlayerEntity player, int durationTicks) {
        NbtCompound nbt = persistent(player);
        nbt.putLong(KEY_CHAD_UNTIL, GemsTime.now(player) + durationTicks);
        nbt.putInt(KEY_CHAD_HITS, 0);
    }

    public static boolean isChadStrengthActive(ServerPlayerEntity player) {
        long now = GemsTime.now(player);
        return persistent(player).getLong(KEY_CHAD_UNTIL) > now;
    }

    public static int incrementChadHit(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        int next = nbt.getInt(KEY_CHAD_HITS) + 1;
        nbt.putInt(KEY_CHAD_HITS, next);
        return next;
    }

    public static void startRichRush(ServerPlayerEntity player, int durationTicks) {
        persistent(player).putLong(KEY_RICH_RUSH_UNTIL, GemsTime.now(player) + durationTicks);
    }

    public static void startAmplification(ServerPlayerEntity player, int durationTicks) {
        persistent(player).putLong(KEY_AMPLIFICATION_UNTIL, GemsTime.now(player) + durationTicks);
    }

    public static boolean isRichRushActive(ServerPlayerEntity player) {
        long now = GemsTime.now(player);
        return persistent(player).getLong(KEY_RICH_RUSH_UNTIL) > now;
    }

    public static void setOwnerIfMissing(ItemStack stack, UUID owner) {
        if (stack.isEmpty()) {
            return;
        }
        NbtComponent existing = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (existing != null && existing.getNbt().contains(CUSTOM_DATA_KEY_OWNER, NbtElement.INT_ARRAY_TYPE)) {
            return;
        }
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.put(CUSTOM_DATA_KEY_OWNER, NbtHelper.fromUuid(owner)));
    }

    public static UUID getOwner(ItemStack stack) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) {
            return null;
        }
        NbtCompound nbt = custom.getNbt();
        if (!nbt.contains(CUSTOM_DATA_KEY_OWNER, NbtElement.INT_ARRAY_TYPE)) {
            return null;
        }
        return NbtHelper.toUuid(nbt.get(CUSTOM_DATA_KEY_OWNER));
    }

    private static void tickCosyCampfire(ServerPlayerEntity player, long now) {
        if (persistent(player).getLong(KEY_CAMPFIRE_UNTIL) <= now) {
            return;
        }
        ServerWorld world = player.getServerWorld();
        int radius = GemsBalance.v().fire().cosyCampfireRadiusBlocks();
        int amp = GemsBalance.v().fire().cosyCampfireRegenAmplifier();
        AbilityFeedback.ring(world, player.getPos().add(0.0D, 0.2D, 0.0D), Math.min(6.0D, radius), net.minecraft.particle.ParticleTypes.CAMPFIRE_COSY_SMOKE, 20);
        for (ServerPlayerEntity other : world.getPlayers(p -> p.squaredDistanceTo(player) <= radius * (double) radius)) {
            if (GemTrust.isTrusted(player, other)) {
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, amp, true, false, false));
                AbilityFeedback.burstAt(world, other.getPos().add(0.0D, 1.0D, 0.0D), net.minecraft.particle.ParticleTypes.HEART, 1, 0.1D);
            }
        }
    }

    private static void tickHeatHazeZone(ServerPlayerEntity player, long now) {
        if (persistent(player).getLong(KEY_HEAT_HAZE_UNTIL) <= now) {
            return;
        }
        ServerWorld world = player.getServerWorld();
        int duration = 40;
        int radius = GemsBalance.v().fire().heatHazeRadiusBlocks();
        int fatigueAmp = GemsBalance.v().fire().heatHazeEnemyMiningFatigueAmplifier();
        int weaknessAmp = GemsBalance.v().fire().heatHazeEnemyWeaknessAmplifier();
        AbilityFeedback.ring(world, player.getPos().add(0.0D, 0.2D, 0.0D), Math.min(7.0D, radius), net.minecraft.particle.ParticleTypes.FLAME, 24);
        for (ServerPlayerEntity other : world.getPlayers(p -> p.squaredDistanceTo(player) <= radius * (double) radius)) {
            if (GemTrust.isTrusted(player, other)) {
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, duration, 0, true, false, false));
                AbilityFeedback.burstAt(world, other.getPos().add(0.0D, 1.0D, 0.0D), net.minecraft.particle.ParticleTypes.FLAME, 1, 0.05D);
            } else {
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, duration, fatigueAmp, true, false, false));
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, weaknessAmp, true, false, false));
                AbilityFeedback.burstAt(world, other.getPos().add(0.0D, 1.0D, 0.0D), net.minecraft.particle.ParticleTypes.SMOKE, 1, 0.08D);
            }
        }
    }

    private static void tickSpeedStorm(ServerPlayerEntity player, long now) {
        if (persistent(player).getLong(KEY_SPEED_STORM_UNTIL) <= now) {
            return;
        }
        ServerWorld world = player.getServerWorld();
        int radius = GemsBalance.v().speed().speedStormRadiusBlocks();
        int allySpeed = GemsBalance.v().speed().speedStormAllySpeedAmplifier();
        int allyHaste = GemsBalance.v().speed().speedStormAllyHasteAmplifier();
        int enemySlow = GemsBalance.v().speed().speedStormEnemySlownessAmplifier();
        int enemyFatigue = GemsBalance.v().speed().speedStormEnemyMiningFatigueAmplifier();
        AbilityFeedback.ring(world, player.getPos().add(0.0D, 0.2D, 0.0D), Math.min(7.0D, radius), net.minecraft.particle.ParticleTypes.CLOUD, 24);
        for (ServerPlayerEntity other : world.getPlayers(p -> p.squaredDistanceTo(player) <= radius * (double) radius)) {
            if (GemTrust.isTrusted(player, other)) {
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, allySpeed, true, false, false));
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 40, allyHaste, true, false, false));
                AbilityFeedback.burstAt(world, other.getPos().add(0.0D, 1.0D, 0.0D), net.minecraft.particle.ParticleTypes.CLOUD, 1, 0.1D);
            } else {
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, enemySlow, true, false, false));
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 40, enemyFatigue, true, false, false));
                AbilityFeedback.burstAt(world, other.getPos().add(0.0D, 1.0D, 0.0D), net.minecraft.particle.ParticleTypes.SNOWFLAKE, 1, 0.1D);
            }
        }
    }

    private static void tickSpaceGravityField(ServerPlayerEntity caster, long now) {
        NbtCompound nbt = persistent(caster);
        long until = nbt.getLong(KEY_SPACE_GRAVITY_UNTIL);
        if (until <= 0) {
            return;
        }

        UUID casterId = nbt.contains(KEY_SPACE_GRAVITY_CASTER, NbtElement.INT_ARRAY_TYPE) ? nbt.getUuid(KEY_SPACE_GRAVITY_CASTER) : caster.getUuid();
        Identifier modifierId = spaceGravityModifierId(casterId);

        MinecraftServer server = caster.getServer();
        if (server == null) {
            nbt.remove(KEY_SPACE_GRAVITY_UNTIL);
            nbt.remove(KEY_SPACE_GRAVITY_CASTER);
            return;
        }

        if (now >= until) {
            nbt.remove(KEY_SPACE_GRAVITY_UNTIL);
            nbt.remove(KEY_SPACE_GRAVITY_CASTER);
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                applyGravityMultiplier(p, modifierId, 1.0F);
            }
            return;
        }

        ServerWorld world = caster.getServerWorld();
        int radius = GemsBalance.v().space().gravityFieldRadiusBlocks();
        float allyMult = GemsBalance.v().space().gravityFieldAllyGravityMultiplier();
        float enemyMult = GemsBalance.v().space().gravityFieldEnemyGravityMultiplier();
        AbilityFeedback.ring(world, caster.getPos().add(0.0D, 0.25D, 0.0D), Math.min(7.0D, radius), net.minecraft.particle.ParticleTypes.END_ROD, 18);

        for (ServerPlayerEntity other : world.getPlayers(p -> p.squaredDistanceTo(caster) <= radius * (double) radius)) {
            float mult = GemTrust.isTrusted(caster, other) ? allyMult : enemyMult;
            applyGravityMultiplier(other, modifierId, mult);
        }
    }

    private static void applyGravityMultiplier(ServerPlayerEntity player, Identifier modifierId, float multiplier) {
        EntityAttributeInstance gravity = player.getAttributeInstance(EntityAttributes.GENERIC_GRAVITY);
        if (gravity == null) {
            return;
        }
        gravity.removeModifier(modifierId);
        if (multiplier != 1.0F) {
            double delta = (double) multiplier - 1.0D;
            gravity.addTemporaryModifier(new EntityAttributeModifier(modifierId, delta, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static Identifier spaceGravityModifierId(UUID caster) {
        return Identifier.of("gems", "space_gravity_" + caster);
    }

    private static void tickLifeCircle(ServerPlayerEntity caster, long now) {
        NbtCompound nbt = persistent(caster);
        long until = nbt.getLong(KEY_LIFE_CIRCLE_UNTIL);
        if (until <= 0) {
            return;
        }
        UUID casterId = nbt.contains(KEY_LIFE_CIRCLE_CASTER, NbtElement.INT_ARRAY_TYPE) ? nbt.getUuid(KEY_LIFE_CIRCLE_CASTER) : caster.getUuid();
        Identifier bonusId = lifeCircleBonusId(casterId);
        Identifier penaltyId = lifeCirclePenaltyId(casterId);

        ServerWorld world = caster.getServerWorld();

        if (until <= now) {
            for (ServerPlayerEntity p : world.getPlayers()) {
                removeMaxHealthModifier(p, bonusId);
                removeMaxHealthModifier(p, penaltyId);
            }
            nbt.remove(KEY_LIFE_CIRCLE_UNTIL);
            nbt.remove(KEY_LIFE_CIRCLE_CASTER);
            return;
        }

        int radius = GemsBalance.v().life().lifeCircleRadiusBlocks();
        double deltaHealth = GemsBalance.v().life().lifeCircleMaxHealthDelta();
        AbilityFeedback.ring(world, caster.getPos().add(0.0D, 0.2D, 0.0D), Math.min(7.0D, radius), net.minecraft.particle.ParticleTypes.HEART, 24);
        for (ServerPlayerEntity other : world.getPlayers()) {
            double distSq = other.squaredDistanceTo(caster);
            boolean inRange = distSq <= radius * (double) radius;
            if (!inRange) {
                removeMaxHealthModifier(other, bonusId);
                removeMaxHealthModifier(other, penaltyId);
                continue;
            }

            if (GemTrust.isTrusted(caster, other)) {
                applyMaxHealthModifier(other, bonusId, deltaHealth);
                removeMaxHealthModifier(other, penaltyId);
            } else {
                applyMaxHealthModifier(other, penaltyId, -deltaHealth);
                removeMaxHealthModifier(other, bonusId);
            }
        }
    }

    private static void tickHeartLock(ServerPlayerEntity target, long now) {
        NbtCompound nbt = persistent(target);
        long until = nbt.getLong(KEY_HEART_LOCK_UNTIL);
        if (until <= 0) {
            return;
        }
        UUID caster = nbt.getUuid(KEY_HEART_LOCK_CASTER);
        Identifier modifierId = heartLockModifierId(caster);
        if (until <= now) {
            removeMaxHealthModifier(target, modifierId);
            nbt.remove(KEY_HEART_LOCK_UNTIL);
            nbt.remove(KEY_HEART_LOCK_CASTER);
            nbt.remove(KEY_HEART_LOCK_LOCKED_MAX);
            return;
        }

        EntityAttributeInstance maxHealth = target.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        // Ensure the lock calculation uses the max health without the Heart Lock modifier,
        // otherwise a "perfectly locked" state (delta = 0) would remove the modifier and oscillate.
        maxHealth.removeModifier(modifierId);
        double baseMax = maxHealth.getValue();

        float locked = nbt.contains(KEY_HEART_LOCK_LOCKED_MAX, NbtElement.FLOAT_TYPE) ? nbt.getFloat(KEY_HEART_LOCK_LOCKED_MAX) : (float) baseMax;
        double lockedMax = Math.max(2.0D, locked);
        double delta = lockedMax - baseMax;
        applyMaxHealthModifier(target, modifierId, delta);
    }

    private static void tickUnbounded(ServerPlayerEntity player, long now) {
        NbtCompound nbt = persistent(player);
        long until = nbt.getLong(KEY_UNBOUNDED_UNTIL);
        if (until <= 0) {
            return;
        }
        if (until > now) {
            return;
        }

        GameMode prev = parseGameMode(nbt.getString(KEY_UNBOUNDED_PREV_GAMEMODE));
        if (prev == null || prev == GameMode.SPECTATOR) {
            prev = GameMode.SURVIVAL;
        }
        player.changeGameMode(prev);
        ensureNotStuck(player);

        nbt.remove(KEY_UNBOUNDED_UNTIL);
        nbt.remove(KEY_UNBOUNDED_PREV_GAMEMODE);
    }

    private static void tickAstralCamera(ServerPlayerEntity player, long now) {
        NbtCompound nbt = persistent(player);
        long until = nbt.getLong(KEY_ASTRAL_CAMERA_UNTIL);
        if (until <= 0) {
            return;
        }
        if (until > now) {
            return;
        }

        GameMode prev = parseGameMode(nbt.getString(KEY_ASTRAL_CAMERA_PREV_GAMEMODE));
        if (prev == null || prev == GameMode.SPECTATOR) {
            prev = GameMode.SURVIVAL;
        }

        BlockPos returnPos = nbt.contains(KEY_ASTRAL_CAMERA_RETURN_POS)
                ? NbtHelper.toBlockPos(nbt, KEY_ASTRAL_CAMERA_RETURN_POS).orElse(player.getBlockPos())
                : player.getBlockPos();
        float yaw = nbt.contains(KEY_ASTRAL_CAMERA_RETURN_YAW, NbtElement.FLOAT_TYPE) ? nbt.getFloat(KEY_ASTRAL_CAMERA_RETURN_YAW) : player.getYaw();
        float pitch = nbt.contains(KEY_ASTRAL_CAMERA_RETURN_PITCH, NbtElement.FLOAT_TYPE) ? nbt.getFloat(KEY_ASTRAL_CAMERA_RETURN_PITCH) : player.getPitch();

        ServerWorld returnWorld = player.getServerWorld();
        if (nbt.contains(KEY_ASTRAL_CAMERA_RETURN_DIM, NbtElement.STRING_TYPE)) {
            Identifier dimId = Identifier.tryParse(nbt.getString(KEY_ASTRAL_CAMERA_RETURN_DIM));
            if (dimId != null) {
                var key = net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, dimId);
                var resolved = player.getServer().getWorld(key);
                if (resolved != null) {
                    returnWorld = resolved;
                }
            }
        }

        player.changeGameMode(prev);
        player.teleport(returnWorld, returnPos.getX() + 0.5D, returnPos.getY(), returnPos.getZ() + 0.5D, yaw, pitch);
        ensureNotStuck(player);

        nbt.remove(KEY_ASTRAL_CAMERA_UNTIL);
        nbt.remove(KEY_ASTRAL_CAMERA_PREV_GAMEMODE);
        nbt.remove(KEY_ASTRAL_CAMERA_RETURN_DIM);
        nbt.remove(KEY_ASTRAL_CAMERA_RETURN_POS);
        nbt.remove(KEY_ASTRAL_CAMERA_RETURN_YAW);
        nbt.remove(KEY_ASTRAL_CAMERA_RETURN_PITCH);
    }

    private static void tickBounty(ServerPlayerEntity hunter, long now) {
        NbtCompound nbt = persistent(hunter);
        long until = nbt.getLong(KEY_BOUNTY_UNTIL);
        if (until <= 0) {
            return;
        }
        if (until <= now) {
            nbt.remove(KEY_BOUNTY_UNTIL);
            nbt.remove(KEY_BOUNTY_TARGET);
            return;
        }
        if (!nbt.contains(KEY_BOUNTY_TARGET, NbtElement.INT_ARRAY_TYPE)) {
            return;
        }
        UUID targetUuid = nbt.getUuid(KEY_BOUNTY_TARGET);
        ServerPlayerEntity target = hunter.getServer().getPlayerManager().getPlayer(targetUuid);
        if (target == null) {
            hunter.sendMessage(Text.literal("Bounty: target offline"), true);
            return;
        }
        double dist = Math.sqrt(hunter.squaredDistanceTo(target));
        hunter.sendMessage(Text.literal("Bounty: " + target.getName().getString() + " (" + (int) dist + "m)"), true);
    }

    private static void tickAmplificationCleanup(ServerPlayerEntity player, long now) {
        long until = persistent(player).getLong(KEY_AMPLIFICATION_UNTIL);
        if (until <= 0) {
            return;
        }
        if (now < until) {
            return;
        }
        for (ItemStack stack : player.getInventory().main) {
            restoreAmplifiedIfExpired(player.getServerWorld(), stack, now);
        }
        for (ItemStack stack : player.getInventory().offHand) {
            restoreAmplifiedIfExpired(player.getServerWorld(), stack, now);
        }
        for (ItemStack stack : player.getInventory().armor) {
            restoreAmplifiedIfExpired(player.getServerWorld(), stack, now);
        }

        persistent(player).remove(KEY_AMPLIFICATION_UNTIL);
    }

    private static void restoreAmplifiedIfExpired(ServerWorld world, ItemStack stack, long now) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) {
            return;
        }
        NbtCompound nbt = custom.getNbt();
        if (!nbt.contains(CUSTOM_DATA_KEY_AMPLIFY, NbtElement.COMPOUND_TYPE)) {
            return;
        }
        NbtCompound marker = nbt.getCompound(CUSTOM_DATA_KEY_AMPLIFY);
        long until = marker.getLong("until");
        if (until > now) {
            return;
        }

        if (marker.contains("enchants", NbtElement.LIST_TYPE)) {
            NbtList list = marker.getList("enchants", NbtElement.COMPOUND_TYPE);
            EnchantmentAmplification.restoreFromList(world, stack, list);
        }

        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, data -> data.remove(CUSTOM_DATA_KEY_AMPLIFY));
    }

    private static void tickReaperGraveSteed(ServerPlayerEntity player, long now) {
        NbtCompound nbt = persistent(player);
        long until = nbt.getLong(KEY_REAPER_STEED_UNTIL);
        if (until <= 0) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            nbt.remove(KEY_REAPER_STEED_UNTIL);
            nbt.remove(KEY_REAPER_STEED_UUID);
            return;
        }

        if (!nbt.contains(KEY_REAPER_STEED_UUID, NbtElement.INT_ARRAY_TYPE)) {
            nbt.remove(KEY_REAPER_STEED_UNTIL);
            return;
        }
        UUID steedUuid = nbt.getUuid(KEY_REAPER_STEED_UUID);
        var e = SummonerSummons.findEntity(server, steedUuid);
        if (!(e instanceof net.minecraft.entity.mob.SkeletonHorseEntity horse) || !horse.isAlive()) {
            nbt.remove(KEY_REAPER_STEED_UNTIL);
            nbt.remove(KEY_REAPER_STEED_UUID);
            return;
        }

        if (now >= until) {
            horse.discard();
            nbt.remove(KEY_REAPER_STEED_UNTIL);
            nbt.remove(KEY_REAPER_STEED_UUID);
            return;
        }

        float decay = GemsBalance.v().reaper().graveSteedDecayDamagePerSecond();
        if (decay > 0.0F) {
            horse.damage(player.getDamageSources().magic(), decay);
        }
        AbilityFeedback.burstAt(player.getServerWorld(), horse.getPos().add(0.0D, 1.0D, 0.0D), net.minecraft.particle.ParticleTypes.SMOKE, 1, 0.12D);
    }

    private static void tickReaperDeathOath(ServerPlayerEntity player, long now) {
        NbtCompound nbt = persistent(player);
        long until = nbt.getLong(KEY_REAPER_OATH_UNTIL);
        if (until <= 0) {
            return;
        }
        if (now >= until) {
            clearReaperDeathOath(player);
            return;
        }

        UUID targetUuid = reaperDeathOathTarget(player);
        if (targetUuid == null) {
            clearReaperDeathOath(player);
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            clearReaperDeathOath(player);
            return;
        }

        var e = SummonerSummons.findEntity(server, targetUuid);
        if (!(e instanceof LivingEntity target) || !target.isAlive()) {
            clearReaperDeathOath(player);
            return;
        }

        float dmg = GemsBalance.v().reaper().deathOathSelfDamagePerSecond();
        nonlethalDrain(player, dmg);
        double dist = player.distanceTo(target);
        player.sendMessage(Text.literal("Death Oath: " + target.getName().getString() + " (" + (int) dist + "m)"), true);
    }

    private static void tickReaperBloodChargeCharging(ServerPlayerEntity player, long now) {
        NbtCompound nbt = persistent(player);
        long until = nbt.getLong(ReaperBloodCharge.KEY_CHARGING_UNTIL);
        if (until <= 0) {
            return;
        }
        if (now >= until) {
            ReaperBloodCharge.clearCharging(player);
            player.sendMessage(Text.literal("Blood Charge fizzled."), true);
            return;
        }

        float dmg = GemsBalance.v().reaper().bloodChargeSelfDamagePerSecond();
        nonlethalDrain(player, dmg);
        int charged = nbt.getInt(ReaperBloodCharge.KEY_CHARGED_TICKS);
        charged = Math.addExact(charged, 20);
        nbt.putInt(ReaperBloodCharge.KEY_CHARGED_TICKS, charged);
    }

    private static void tickReaperShadeClone(ServerPlayerEntity player, long now) {
        NbtCompound nbt = persistent(player);
        long until = nbt.getLong(KEY_REAPER_CLONE_UNTIL);
        if (until <= 0) {
            return;
        }

        MinecraftServer server = player.getServer();
        if (server == null) {
            nbt.remove(KEY_REAPER_CLONE_UNTIL);
            nbt.remove(KEY_REAPER_CLONE_UUID);
            return;
        }

        if (!nbt.contains(KEY_REAPER_CLONE_UUID, NbtElement.INT_ARRAY_TYPE)) {
            nbt.remove(KEY_REAPER_CLONE_UNTIL);
            return;
        }
        UUID cloneUuid = nbt.getUuid(KEY_REAPER_CLONE_UUID);
        var e = SummonerSummons.findEntity(server, cloneUuid);
        if (e == null || !e.isAlive()) {
            nbt.remove(KEY_REAPER_CLONE_UNTIL);
            nbt.remove(KEY_REAPER_CLONE_UUID);
            return;
        }

        if (now >= until) {
            e.discard();
            nbt.remove(KEY_REAPER_CLONE_UNTIL);
            nbt.remove(KEY_REAPER_CLONE_UUID);
            return;
        }

        AbilityFeedback.burstAt(player.getServerWorld(), e.getPos().add(0.0D, 1.0D, 0.0D), net.minecraft.particle.ParticleTypes.SOUL, 1, 0.08D);
    }

    private static void nonlethalDrain(ServerPlayerEntity player, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        float health = player.getHealth();
        float newHealth = Math.max(1.0F, health - amount);
        if (newHealth < health) {
            player.setHealth(newHealth);
        }
    }

    private static void applyMaxHealthModifier(ServerPlayerEntity player, Identifier id, double deltaHealth) {
        EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        maxHealth.removeModifier(id);
        if (deltaHealth != 0.0D) {
            maxHealth.addTemporaryModifier(new EntityAttributeModifier(id, deltaHealth, EntityAttributeModifier.Operation.ADD_VALUE));
        }
        float newMax = (float) maxHealth.getValue();
        if (player.getHealth() > newMax) {
            player.setHealth(newMax);
        }
    }

    private static void removeMaxHealthModifier(ServerPlayerEntity player, Identifier id) {
        EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        maxHealth.removeModifier(id);
        float newMax = (float) maxHealth.getValue();
        if (player.getHealth() > newMax) {
            player.setHealth(newMax);
        }
    }

    private static Identifier lifeCircleBonusId(UUID caster) {
        return Identifier.of("gems", "life_circle_bonus_" + caster);
    }

    private static Identifier lifeCirclePenaltyId(UUID caster) {
        return Identifier.of("gems", "life_circle_penalty_" + caster);
    }

    private static Identifier heartLockModifierId(UUID caster) {
        return Identifier.of("gems", "heart_lock_" + caster);
    }

    public static void cleanupOnDisconnect(MinecraftServer server, ServerPlayerEntity caster) {
        NbtCompound nbt = persistent(caster);
        long lifeCircleUntil = nbt.getLong(KEY_LIFE_CIRCLE_UNTIL);
        if (lifeCircleUntil > 0) {
            UUID casterId = nbt.contains(KEY_LIFE_CIRCLE_CASTER, NbtElement.INT_ARRAY_TYPE) ? nbt.getUuid(KEY_LIFE_CIRCLE_CASTER) : caster.getUuid();
            Identifier bonusId = lifeCircleBonusId(casterId);
            Identifier penaltyId = lifeCirclePenaltyId(casterId);
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                removeMaxHealthModifier(p, bonusId);
                removeMaxHealthModifier(p, penaltyId);
            }
            nbt.remove(KEY_LIFE_CIRCLE_UNTIL);
            nbt.remove(KEY_LIFE_CIRCLE_CASTER);
        }

        long gravityUntil = nbt.getLong(KEY_SPACE_GRAVITY_UNTIL);
        if (gravityUntil > 0) {
            UUID casterId = nbt.contains(KEY_SPACE_GRAVITY_CASTER, NbtElement.INT_ARRAY_TYPE) ? nbt.getUuid(KEY_SPACE_GRAVITY_CASTER) : caster.getUuid();
            Identifier modifierId = spaceGravityModifierId(casterId);
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                applyGravityMultiplier(p, modifierId, 1.0F);
            }
            nbt.remove(KEY_SPACE_GRAVITY_UNTIL);
            nbt.remove(KEY_SPACE_GRAVITY_CASTER);
        }

        if (nbt.contains(KEY_REAPER_STEED_UUID, NbtElement.INT_ARRAY_TYPE)) {
            UUID uuid = nbt.getUuid(KEY_REAPER_STEED_UUID);
            var e = SummonerSummons.findEntity(server, uuid);
            if (e != null) {
                e.discard();
            }
        }
        nbt.remove(KEY_REAPER_STEED_UNTIL);
        nbt.remove(KEY_REAPER_STEED_UUID);

        if (nbt.contains(KEY_REAPER_CLONE_UUID, NbtElement.INT_ARRAY_TYPE)) {
            UUID uuid = nbt.getUuid(KEY_REAPER_CLONE_UUID);
            var e = SummonerSummons.findEntity(server, uuid);
            if (e != null) {
                e.discard();
            }
        }
        nbt.remove(KEY_REAPER_CLONE_UNTIL);
        nbt.remove(KEY_REAPER_CLONE_UUID);

        nbt.remove(KEY_REAPER_WITHERING_UNTIL);
        clearReaperDeathOath(caster);
        ReaperBloodCharge.clearCharging(caster);
        ((GemsPersistentDataHolder) caster).gems$getPersistentData().remove(ReaperBloodCharge.KEY_BUFF_UNTIL);
        ((GemsPersistentDataHolder) caster).gems$getPersistentData().remove(ReaperBloodCharge.KEY_BUFF_MULT);
    }

    private static GameMode parseGameMode(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (GameMode mode : GameMode.values()) {
            if (mode.getName().equals(name)) {
                return mode;
            }
        }
        return null;
    }

    private static void ensureNotStuck(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        if (!world.getBlockCollisions(player, player.getBoundingBox()).iterator().hasNext()) {
            return;
        }

        BlockPos base = player.getBlockPos();
        BlockPos.Mutable p = new BlockPos.Mutable();
        var box = player.getBoundingBox();

        for (int dy = 0; dy <= 6; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    p.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    Vec3d candidate = new Vec3d(p.getX() + 0.5D, p.getY(), p.getZ() + 0.5D);
                    var moved = box.offset(candidate.subtract(player.getPos()));
                    if (!world.getBlockCollisions(player, moved).iterator().hasNext()) {
                        player.teleport(world, candidate.x, candidate.y, candidate.z, player.getYaw(), player.getPitch());
                        return;
                    }
                }
            }
        }
    }

    private static NbtCompound persistent(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
