package com.feel.gems.power.ability.trickster;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.entity.ShadowCloneEntity;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.state.PlayerStateManager;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public final class TricksterMirageRuntime {
    private static final String MIRAGE_CLONE_UUID_KEY = "trickster_mirage_clone_uuid";
    private static final String MIRAGE_END_KEY = "trickster_mirage_end";
    private static final String MIRAGE_OFFSET_X_KEY = "trickster_mirage_offset_x";
    private static final String MIRAGE_OFFSET_Y_KEY = "trickster_mirage_offset_y";
    private static final String MIRAGE_OFFSET_Z_KEY = "trickster_mirage_offset_z";

    private TricksterMirageRuntime() {}

    public static void createMirage(ServerPlayerEntity player, UUID cloneUuid, Vec3d offset, long endTime) {
        PlayerStateManager.setPersistent(player, MIRAGE_CLONE_UUID_KEY, cloneUuid.toString());
        PlayerStateManager.setPersistent(player, MIRAGE_END_KEY, String.valueOf(endTime));
        PlayerStateManager.setPersistent(player, MIRAGE_OFFSET_X_KEY, String.valueOf(offset.x));
        PlayerStateManager.setPersistent(player, MIRAGE_OFFSET_Y_KEY, String.valueOf(offset.y));
        PlayerStateManager.setPersistent(player, MIRAGE_OFFSET_Z_KEY, String.valueOf(offset.z));
    }

    public static boolean hasMirages(ServerPlayerEntity player) {
        return getCloneUuid(player) != null;
    }

    public static void tickMirage(ServerPlayerEntity player) {
        UUID cloneUuid = getCloneUuid(player);
        if (cloneUuid == null) {
            return;
        }
        ServerWorld world = player.getEntityWorld();
        long endTime = getEndTime(player);
        if (endTime > 0 && world.getTime() > endTime) {
            Entity entity = world.getEntity(cloneUuid);
            if (entity != null) {
                entity.discard();
            }
            applyBuff(player);
            clearMirages(player);
            return;
        }
        Entity entity = world.getEntity(cloneUuid);
        if (!(entity instanceof ShadowCloneEntity clone) || !clone.isAlive()) {
            applyBuff(player);
            clearMirages(player);
            return;
        }
        Vec3d offset = getOffset(player);
        Vec3d nextPos = player.getEntityPos().add(offset);
        clone.refreshPositionAndAngles(nextPos.x, nextPos.y, nextPos.z, player.getYaw(), player.getPitch());
        clone.setHeadYaw(player.getHeadYaw());
        boolean sneaking = player.isSneaking();
        clone.setSneaking(sneaking);
        clone.setPose(sneaking ? EntityPose.CROUCHING : EntityPose.STANDING);
        clone.setSprinting(player.isSprinting());
        clone.setVelocity(Vec3d.ZERO);
    }

    public static void onMirageCloneHit(ShadowCloneEntity clone) {
        if (clone == null || !(clone.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        UUID ownerId = clone.getOwnerUuid();
        if (ownerId == null || world.getServer() == null) {
            return;
        }
        ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(ownerId);
        if (owner == null) {
            return;
        }
        applyBuff(owner);
        clearMirages(owner);
    }

    public static void clearMirages(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, MIRAGE_CLONE_UUID_KEY);
        PlayerStateManager.clearPersistent(player, MIRAGE_END_KEY);
        PlayerStateManager.clearPersistent(player, MIRAGE_OFFSET_X_KEY);
        PlayerStateManager.clearPersistent(player, MIRAGE_OFFSET_Y_KEY);
        PlayerStateManager.clearPersistent(player, MIRAGE_OFFSET_Z_KEY);
    }

    public static Vec3d getMiragePosition(ServerPlayerEntity player) {
        UUID cloneUuid = getCloneUuid(player);
        if (cloneUuid == null) {
            return null;
        }
        return player.getEntityPos().add(getOffset(player));
    }

    public static void tickMirageParticles(ServerPlayerEntity player) {
        Vec3d pos = getMiragePosition(player);
        if (pos == null) {
            return;
        }
        ServerWorld world = player.getEntityWorld();
        long time = world.getTime();
        Vec3d particlePos = pos.add(0, 1.0, 0);

        double angle = (time * 0.15) % (2 * Math.PI);
        double ringRadius = 0.9;
        double offsetX = Math.cos(angle) * ringRadius;
        double offsetZ = Math.sin(angle) * ringRadius;

        AbilityFeedback.burstAt(world, particlePos.add(offsetX, 0.1, offsetZ), ParticleTypes.ENCHANT, 4, 0.45D);
        AbilityFeedback.burstAt(world, particlePos.add(-offsetX, 0.6, -offsetZ), ParticleTypes.END_ROD, 3, 0.35D);

        if (time % 8 == 0) {
            AbilityFeedback.burstAt(world, particlePos, ParticleTypes.PORTAL, 6, 0.6D);
        }
        if (time % 12 == 0) {
            AbilityFeedback.burstAt(world, particlePos.add(0, 0.6, 0), ParticleTypes.WITCH, 2, 0.3D);
        }
        if (time % 16 == 0) {
            AbilityFeedback.burstAt(world, particlePos.add(0, -0.2, 0), ParticleTypes.SOUL, 2, 0.2D);
        }
    }

    private static UUID getCloneUuid(ServerPlayerEntity player) {
        String raw = PlayerStateManager.getPersistent(player, MIRAGE_CLONE_UUID_KEY);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static long getEndTime(ServerPlayerEntity player) {
        String raw = PlayerStateManager.getPersistent(player, MIRAGE_END_KEY);
        if (raw == null || raw.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static Vec3d getOffset(ServerPlayerEntity player) {
        String xStr = PlayerStateManager.getPersistent(player, MIRAGE_OFFSET_X_KEY);
        String yStr = PlayerStateManager.getPersistent(player, MIRAGE_OFFSET_Y_KEY);
        String zStr = PlayerStateManager.getPersistent(player, MIRAGE_OFFSET_Z_KEY);
        if (xStr == null || yStr == null || zStr == null) {
            return Vec3d.ZERO;
        }
        try {
            return new Vec3d(Double.parseDouble(xStr), Double.parseDouble(yStr), Double.parseDouble(zStr));
        } catch (NumberFormatException e) {
            return Vec3d.ZERO;
        }
    }

    private static void applyBuff(ServerPlayerEntity player) {
        var cfg = GemsBalance.v().trickster();
        int durationTicks = AugmentRuntime.applyDurationMultiplier(player, GemId.TRICKSTER, cfg.mirageBuffDurationTicks());
        if (durationTicks <= 0) {
            return;
        }
        int amp = Math.max(0, cfg.mirageBuffAmplifier());
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, durationTicks, amp, true, false, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, durationTicks, 0, true, false, true));
    }
}
