package com.feel.gems.power.ability.duelist;

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

public final class DuelistFlourishAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.DUELIST_FLOURISH;
    }

    @Override
    public String name() {
        return "Flourish";
    }

    @Override
    public String description() {
        return "Quick 360° sword sweep that hits all nearby enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().duelist().flourishCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int radius = GemsBalance.v().duelist().flourishRadiusBlocks();
        float damage = GemsBalance.v().duelist().flourishDamage();

        Box box = player.getBoundingBox().expand(radius);
        for (Entity e : world.getOtherEntities(player, box, ent -> ent instanceof LivingEntity living && living.isAlive())) {
            if (e instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
                continue;
            }
            ((LivingEntity) e).damage(world, player.getDamageSources().playerAttack(player), damage);
            AbilityFeedback.burstAt(world, e.getEntityPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.SWEEP_ATTACK, 8, 0.2D);
        }

        // Ring particle effect
        AbilityFeedback.ring(world, player.getEntityPos().add(0.0D, 1.0D, 0.0D), radius, ParticleTypes.SWEEP_ATTACK, 24);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 0.8F);
        return true;
    }
}
