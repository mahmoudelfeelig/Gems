package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

public final class BonusSanctuaryAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_SANCTUARY;
    }

    @Override
    public String name() {
        return "Sanctuary";
    }

    @Override
    public String description() {
        return "Create a protective zone that heals allies and pushes away enemies.";
    }

    @Override
    public int cooldownTicks() {
        return 1000; // 50 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Box area = player.getBoundingBox().expand(6);
        
        world.spawnParticles(ParticleTypes.COMPOSTER, player.getX(), player.getY() + 0.5, player.getZ(), 100, 3, 0.5, 3, 0.1);
        
        world.getOtherEntities(player, area).forEach(e -> {
            if (e instanceof ServerPlayerEntity ally) {
                ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
                ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 0));
            } else if (e instanceof net.minecraft.entity.LivingEntity living) {
                // Push enemies away
                var direction = e.getEntityPos().subtract(player.getEntityPos()).normalize().multiply(1.5);
                e.setVelocity(direction.x, 0.5, direction.z);
                e.velocityDirty = true;
            }
        });
        
        // Also buff self
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 100, 0));
        return true;
    }
}
