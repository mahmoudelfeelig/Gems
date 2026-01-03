package com.feel.gems.power.ability.puff;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;



public final class DashAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.DASH;
    }

    @Override
    public String name() {
        return "Dash";
    }

    @Override
    public String description() {
        return "Dashes forward and damages enemies in your path.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().puff().dashCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Vec3d dir = player.getRotationVec(1.0F).normalize();
        double dashVel = GemsBalance.v().puff().dashVelocity();
        player.addVelocity(dir.x * dashVel, 0.1D, dir.z * dashVel);
        player.velocityDirty = true;
        AbilityFeedback.beam(world, player.getEntityPos().add(0.0D, 1.0D, 0.0D), player.getEntityPos().add(dir.multiply(3.0D)).add(0.0D, 1.0D, 0.0D), ParticleTypes.CLOUD, 10);

        Box box = player.getBoundingBox().stretch(dir.multiply(GemsBalance.v().puff().dashHitRangeBlocks())).expand(1.0D);
        for (Entity e : world.getOtherEntities(player, box, ent -> ent instanceof LivingEntity living && living.isAlive())) {
            if (e instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
                continue;
            }
            ((LivingEntity) e).damage(world, player.getDamageSources().playerAttack(player), GemsBalance.v().puff().dashDamage());
            AbilityFeedback.burstAt(world, e.getEntityPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.GUST, 8, 0.25D);
        }

        AbilityFeedback.sound(player, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 0.7F, 1.3F);
        return true;
    }
}
