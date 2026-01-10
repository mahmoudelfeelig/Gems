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
 * Reflection Ward - Next 3 projectiles are reflected back at attackers.
 * Implementation via event hook that checks for this tag.
 */
public final class BonusReflectionWardAbility implements GemAbility {
    private static final int REFLECT_COUNT = 3;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_REFLECTION_WARD;
    }

    @Override
    public String name() {
        return "Reflection Ward";
    }

    @Override
    public String description() {
        return "Create a ward that reflects the next 3 projectiles back at attackers.";
    }

    @Override
    public int cooldownTicks() {
        return 400; // 20 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        
        // Set reflect counter via scoreboard or tag
        player.addCommandTag("gems_reflection_ward:" + REFLECT_COUNT);

        // Shield particles
        for (int i = 0; i < 20; i++) {
            double angle = (2 * Math.PI / 20) * i;
            double x = player.getX() + Math.cos(angle) * 1.5;
            double z = player.getZ() + Math.sin(angle) * 1.5;
            world.spawnParticles(ParticleTypes.END_ROD, x, player.getY() + 1, z, 
                    2, 0.05, 0.3, 0.05, 0.01);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0f, 1.5f);
        return true;
    }
}
