package com.feel.gems.power.ability.pillager;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;



public final class PillagerRavageAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.PILLAGER_RAVAGE;
    }

    @Override
    public String name() {
        return "Ravage";
    }

    @Override
    public String description() {
        return "Bashes a target with heavy knockback.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().pillager().ravageCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int range = GemsBalance.v().pillager().ravageRangeBlocks();
        if (range <= 0) {
            return false;
        }

        float damage = GemsBalance.v().pillager().ravageDamage();
        double knockback = GemsBalance.v().pillager().ravageKnockback();
        Vec3d forward = player.getRotationVec(1.0F).normalize();
        double cosHalfArc = Math.cos(Math.toRadians(55.0D)); // ~110 degree frontal cone

        boolean hit = false;
        Vec3d origin = player.getEntityPos();
        Box box = new Box(origin, origin).expand(range, 1.5D, range);
        DamageSource src = player.getDamageSources().playerAttack(player);
        ServerWorld world = player.getEntityWorld();

        for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            Vec3d to = target.getEntityPos().subtract(origin);
            double distSq = to.lengthSquared();
            if (distSq > (double) (range * range)) {
                continue;
            }
            Vec3d dir = to.normalize();
            if (forward.dotProduct(dir) < cosHalfArc) {
                continue;
            }
            if (target instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
                continue;
            }
            if (target instanceof ServerPlayerEntity other && !VoidImmunity.canBeTargeted(player, other)) {
                continue;
            }

            target.damage(world, src, damage);
            target.addVelocity(dir.x * knockback, 0.15D, dir.z * knockback);
            target.velocityDirty = true;
            hit = true;
        }

        if (!hit) {
            player.sendMessage(Text.translatable("gems.ability.pillager.ravage.no_targets"), true);
            return false;
        }

        AbilityFeedback.burstAt(world, player.getEntityPos().add(0.0D, 0.8D, 0.0D), ParticleTypes.SWEEP_ATTACK, 4, 0.1D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_RAVAGER_ATTACK, 0.9F, 1.0F);
        return true;
    }
}

