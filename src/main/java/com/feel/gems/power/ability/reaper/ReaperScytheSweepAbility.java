package com.feel.gems.power.ability.reaper;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import java.util.List;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;



public final class ReaperScytheSweepAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.REAPER_SCYTHE_SWEEP;
    }

    @Override
    public String name() {
        return "Scythe Sweep";
    }

    @Override
    public String description() {
        return "Cleave enemies in an arc in front of you.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().reaper().scytheSweepCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        var world = player.getServerWorld();
        int range = GemsBalance.v().reaper().scytheSweepRangeBlocks();
        int arc = GemsBalance.v().reaper().scytheSweepArcDegrees();
        float damage = GemsBalance.v().reaper().scytheSweepDamage();
        double knockback = GemsBalance.v().reaper().scytheSweepKnockback();
        if (range <= 0 || arc <= 0) {
            player.sendMessage(Text.literal("Scythe Sweep is disabled."), true);
            return false;
        }

        Vec3d origin = player.getPos().add(0.0D, 1.0D, 0.0D);
        Vec3d look = player.getRotationVec(1.0F).normalize();
        double cos = Math.cos(Math.toRadians(arc / 2.0D));

        Box box = player.getBoundingBox().expand(range, 2.0D, range);
        List<LivingEntity> candidates = world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && e != player);
        int hit = 0;
        for (LivingEntity living : candidates) {
            Vec3d to = living.getPos().add(0.0D, living.getHeight() * 0.5D, 0.0D).subtract(origin);
            double dist = to.length();
            if (dist <= 0.001D || dist > range) {
                continue;
            }
            Vec3d dir = to.normalize();
            if (dir.dotProduct(look) < cos) {
                continue;
            }
            if (living instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
                continue;
            }
            if (damage > 0.0F) {
                living.damage(player.getDamageSources().playerAttack(player), damage);
            }
            if (knockback > 0.0D) {
                Vec3d kb = dir.multiply(knockback);
                living.setVelocity(living.getVelocity().add(kb.x, 0.15D, kb.z));
                living.velocityModified = true;
            }
            AbilityFeedback.burstAt(world, living.getPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.SOUL, 3, 0.12D);
            hit++;
        }

        AbilityFeedback.sound(player, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.8F, 0.7F);
        AbilityFeedback.burstAt(world, origin, ParticleTypes.SWEEP_ATTACK, 1, 0.0D);
        player.sendMessage(Text.literal("Scythe Sweep hit " + hit + "."), true);
        return hit > 0;
    }
}
