package com.feel.gems.power.gem.spy;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;


public final class SpyBackstab {
    private SpyBackstab() {
    }

    public static void apply(ServerPlayerEntity attacker, LivingEntity target) {
        if (!GemPowers.isPassiveActive(attacker, PowerIds.SPY_BACKSTEP)) {
            return;
        }
        if (target instanceof ServerPlayerEntity other && GemTrust.isTrusted(attacker, other)) {
            return;
        }
        float bonus = GemsBalance.v().spyMimic().backstabBonusDamage();
        if (bonus <= 0.0F) {
            return;
        }

        Vec3d toAttacker = attacker.getEntityPos().subtract(target.getEntityPos());
        if (toAttacker.lengthSquared() <= 1.0E-4D) {
            return;
        }
        Vec3d targetLook = target.getRotationVec(1.0F);
        if (targetLook.lengthSquared() <= 1.0E-4D) {
            return;
        }
        double dot = targetLook.normalize().dotProduct(toAttacker.normalize());
        double cone = Math.cos(Math.toRadians(GemsBalance.v().spyMimic().backstabAngleDegrees()));
        if (dot > -cone) {
            return;
        }

        if (!(attacker.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        target.damage(world, attacker.getDamageSources().playerAttack(attacker), bonus);
        AbilityFeedback.burstAt(world, target.getEntityPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.CRIT, 10, 0.2D);
        AbilityFeedback.sound(attacker, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 0.7F, 1.1F);
    }
}
