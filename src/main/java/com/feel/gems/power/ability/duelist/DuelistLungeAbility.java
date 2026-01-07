package com.feel.gems.power.ability.duelist;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
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

public final class DuelistLungeAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.DUELIST_LUNGE;
    }

    @Override
    public String name() {
        return "Lunge";
    }

    @Override
    public String description() {
        return "Dash forward with your sword, dealing damage to the first enemy hit.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().duelist().lungeCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Vec3d dir = player.getRotationVec(1.0F).normalize();
        int distance = GemsBalance.v().duelist().lungeDistanceBlocks();
        float damage = GemsBalance.v().duelist().lungeDamage();

        // Apply velocity
        double vel = distance / 4.0;
        player.setVelocity(dir.x * vel, 0.1D, dir.z * vel);
        player.velocityDirty = true;
        AbilityFeedback.syncVelocity(player);

        // Find first enemy in path
        Box box = player.getBoundingBox().stretch(dir.multiply(distance)).expand(1.0D);
        LivingEntity target = null;
        double closest = Double.MAX_VALUE;

        for (Entity e : world.getOtherEntities(player, box, ent -> ent instanceof LivingEntity living && living.isAlive())) {
            if (e instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
                continue;
            }
            if (e instanceof ServerPlayerEntity other && !VoidImmunity.canBeTargeted(player, other)) {
                continue;
            }
            double dist = e.squaredDistanceTo(player);
            if (dist < closest) {
                closest = dist;
                target = (LivingEntity) e;
            }
        }

        if (target != null) {
            target.damage(world, player.getDamageSources().playerAttack(player), damage);
            AbilityFeedback.burstAt(world, target.getEntityPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.SWEEP_ATTACK, 15, 0.3D);
        }

        AbilityFeedback.beam(world, player.getEntityPos().add(0.0D, 1.0D, 0.0D),
                player.getEntityPos().add(dir.multiply(distance)).add(0.0D, 1.0D, 0.0D),
                ParticleTypes.SWEEP_ATTACK, 10);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.2F);
        return true;
    }
}
