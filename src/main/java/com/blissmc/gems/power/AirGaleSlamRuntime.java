package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTime;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class AirGaleSlamRuntime {
    private static final String KEY_GALE_SLAM_UNTIL = "airGaleSlamUntil";

    private AirGaleSlamRuntime() {
    }

    public static void start(ServerPlayerEntity player, int windowTicks) {
        ((GemsPersistentDataHolder) player).gems$getPersistentData().putLong(KEY_GALE_SLAM_UNTIL, GemsTime.now(player) + windowTicks);
    }

    public static boolean consumeIfActive(ServerPlayerEntity player) {
        var nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        long until = nbt.getLong(KEY_GALE_SLAM_UNTIL);
        if (until <= 0) {
            return false;
        }
        long now = GemsTime.now(player);
        nbt.remove(KEY_GALE_SLAM_UNTIL);
        return now <= until;
    }

    public static void trigger(ServerPlayerEntity player, Entity target) {
        int radius = GemsBalance.v().air().galeSlamRadiusBlocks();
        float bonusDamage = GemsBalance.v().air().galeSlamBonusDamage();
        double knockback = GemsBalance.v().air().galeSlamKnockback();
        if (radius <= 0 || bonusDamage <= 0.0F) {
            return;
        }

        ServerWorld world = player.getServerWorld();
        Vec3d center = (target != null) ? target.getPos() : player.getPos();
        Box box = new Box(center, center).expand(radius);

        for (Entity entity : world.getOtherEntities(player, box, e -> e instanceof LivingEntity living && living.isAlive())) {
            if (entity instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
                continue;
            }
            LivingEntity living = (LivingEntity) entity;
            living.damage(player.getDamageSources().playerAttack(player), bonusDamage);
            if (knockback > 0.0D) {
                Vec3d away = living.getPos().subtract(center).normalize();
                living.addVelocity(away.x * knockback, 0.2D, away.z * knockback);
                living.velocityModified = true;
            }
        }

        AbilityFeedback.burstAt(world, center.add(0.0D, 0.6D, 0.0D), ParticleTypes.GUST, 18, 0.3D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_BREEZE_WIND_BURST, 1.0F, 1.0F);
    }
}

