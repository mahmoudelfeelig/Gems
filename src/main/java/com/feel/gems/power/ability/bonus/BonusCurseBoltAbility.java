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

        float totalDrain = 0.0F;
        for (LivingEntity living : world.getEntitiesByClass(
                LivingEntity.class,
                area,
                e -> e != player && !(e instanceof ServerPlayerEntity) && e.isAlive()
        )) {
            float before = living.getHealth();
            living.damage(world, player.getDamageSources().indirectMagic(player, player), 4.0F);
            float dealt = Math.max(0.0F, before - living.getHealth());
            if (dealt > 0.0F) {
                totalDrain += dealt * 0.5F;
            }
            world.spawnParticles(ParticleTypes.SOUL, living.getX(), living.getY() + 1, living.getZ(), 10, 0.3, 0.5, 0.3, 0.05);
        }

        if (totalDrain > 0.0F) {
            player.heal(totalDrain);
        }
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 1, 0.5, 0.05);
        return true;
    }
}
