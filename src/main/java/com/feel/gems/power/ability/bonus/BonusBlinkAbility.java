package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.feel.gems.util.GemsTeleport;

/**
 * Blink - Instant micro-teleport (5 blocks) with minimal cooldown.
 */
public final class BonusBlinkAbility implements GemAbility {
    private static final double BLINK_DISTANCE = 5.0;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_BLINK;
    }

    @Override
    public String name() {
        return "Blink";
    }

    @Override
    public String description() {
        return "Instantly blink 5 blocks in your facing direction.";
    }

    @Override
    public int cooldownTicks() {
        return 60; // 3 seconds - short cooldown
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        Vec3d start = player.getEntityPos();
        Vec3d direction = player.getRotationVector();
        Vec3d end = start.add(direction.multiply(BLINK_DISTANCE));

        // Particles at start
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, start.x, start.y + 1, start.z, 
                15, 0.3, 0.5, 0.3, 0.05);

        // Teleport
        GemsTeleport.teleport(player, world, end.x, end.y, end.z, player.getYaw(), player.getPitch());

        // Particles at end
        world.spawnParticles(ParticleTypes.PORTAL, end.x, end.y + 1, end.z, 
                15, 0.3, 0.5, 0.3, 0.05);

        world.playSound(null, end.x, end.y, end.z,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.5f, 1.5f);
        return true;
    }
}
