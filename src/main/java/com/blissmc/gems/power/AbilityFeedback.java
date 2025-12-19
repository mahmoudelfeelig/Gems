package com.blissmc.gems.power;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.Vec3d;

public final class AbilityFeedback {
    private AbilityFeedback() {
    }

    public static void sound(ServerPlayerEntity player, SoundEvent sound, float volume, float pitch) {
        ServerWorld world = player.getServerWorld();
        world.playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundCategory.PLAYERS, volume, pitch);
    }

    public static void sound(ServerPlayerEntity player, RegistryEntry.Reference<SoundEvent> sound, float volume, float pitch) {
        sound(player, sound.value(), volume, pitch);
    }

    public static void burst(ServerPlayerEntity player, ParticleEffect particle, int count, double spread) {
        burstAt(player.getServerWorld(), player.getPos().add(0.0D, 1.0D, 0.0D), particle, count, spread);
    }

    public static void burstAt(ServerWorld world, Vec3d pos, ParticleEffect particle, int count, double spread) {
        if (count <= 0) {
            return;
        }
        world.spawnParticles(
                particle,
                pos.x,
                pos.y,
                pos.z,
                count,
                spread,
                spread,
                spread,
                0.01D
        );
    }

    public static void beam(ServerWorld world, Vec3d from, Vec3d to, ParticleEffect particle, int steps) {
        if (steps <= 0) {
            return;
        }
        double dx = (to.x - from.x) / steps;
        double dy = (to.y - from.y) / steps;
        double dz = (to.z - from.z) / steps;

        for (int i = 0; i <= steps; i++) {
            double x = from.x + dx * i;
            double y = from.y + dy * i;
            double z = from.z + dz * i;
            world.spawnParticles(particle, x, y, z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }

    public static void ring(ServerWorld world, Vec3d center, double radius, ParticleEffect particle, int points) {
        if (points <= 0 || radius <= 0.0D) {
            return;
        }
        double step = (Math.PI * 2.0D) / points;
        for (int i = 0; i < points; i++) {
            double a = i * step;
            double x = center.x + Math.cos(a) * radius;
            double z = center.z + Math.sin(a) * radius;
            world.spawnParticles(particle, x, center.y, z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
        }
    }
}
