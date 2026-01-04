package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

/**
 * Curse Bolt - Drain life from nearby enemies, healing yourself.
 */
public final class BonusCurseBoltAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_CURSE_BOLT;
    }

    @Override
    public String name() {
        return "Curse Bolt";
    }

    @Override
    public String description() {
        return "Drain life from nearby enemies, healing yourself.";
    }

    @Override
    public int cooldownTicks() {
        return 500; // 25 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Box area = player.getBoundingBox().expand(8);
        
        float[] totalDrain = {0f};
        
        world.getOtherEntities(player, area, e -> e instanceof LivingEntity && !(e instanceof ServerPlayerEntity))
                .forEach(e -> {
                    if (e instanceof LivingEntity living) {
                        float damage = 4.0f;
                        living.damage(world, world.getDamageSources().magic(), damage);
                        totalDrain[0] += damage * 0.5f;
                        world.spawnParticles(ParticleTypes.SOUL, living.getX(), living.getY() + 1, living.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
                    }
                });
        
        player.heal(totalDrain[0]);
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 1, 0.5, 0.05);
        return true;
    }
}
