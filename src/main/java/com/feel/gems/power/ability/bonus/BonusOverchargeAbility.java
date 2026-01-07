package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * Overcharge - Next ability deals double damage but costs health.
 * Implementation via event hook that checks for this tag.
 */
public final class BonusOverchargeAbility implements GemAbility {
    private static final float HEALTH_COST = 4.0f; // 2 hearts

    @Override
    public Identifier id() {
        return PowerIds.BONUS_OVERCHARGE;
    }

    @Override
    public String name() {
        return "Overcharge";
    }

    @Override
    public String description() {
        return "Your next ability deals double damage but costs 2 hearts.";
    }

    @Override
    public int cooldownTicks() {
        return 300; // 15 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        
        if (player.getHealth() <= HEALTH_COST) {
            return false; // Can't afford health cost
        }

        // Pay health cost
        player.damage(world, world.getDamageSources().magic(), HEALTH_COST);

        // Mark player for overcharge bonus
        player.addCommandTag("gems_overcharge");

        // Energy buildup particles
        for (int i = 0; i < 30; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = world.random.nextDouble() * 1.5;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            double y = player.getY() + world.random.nextDouble() * 2;
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0, 0.2, 0, 0.05);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0f, 2.0f);
        return true;
    }
}
