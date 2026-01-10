package com.feel.gems.power.ability.trickster;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * Mirage - create 3 illusory copies that mirror your movements for 10s.
 * Clones take one hit to dispel.
 */
public final class TricksterMirageAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TRICKSTER_MIRAGE;
    }

    @Override
    public String name() {
        return "Mirage";
    }

    @Override
    public String description() {
        return "Create 3 illusory copies that mirror your movements for 10s.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().trickster().mirageCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int durationTicks = GemsBalance.v().trickster().mirageDurationTicks();
        int cloneCount = GemsBalance.v().trickster().mirageCloneCount();

        Vec3d center = player.getEntityPos();
        long endTime = world.getTime() + durationTicks;

        // Create clones in a circle around the player
        TricksterMirageRuntime.createMirages(player, center, cloneCount, endTime);

        // Visual effects
        for (int i = 0; i < cloneCount; i++) {
            double angle = (2 * Math.PI * i) / cloneCount;
            double offsetX = Math.cos(angle) * 2.0;
            double offsetZ = Math.sin(angle) * 2.0;
            Vec3d clonePos = center.add(offsetX, 0, offsetZ);

            AbilityFeedback.burstAt(world, clonePos.add(0, 1, 0), ParticleTypes.LARGE_SMOKE, 18, 0.5D);
            AbilityFeedback.burstAt(world, clonePos.add(0, 1.2, 0), ParticleTypes.ENCHANT, 10, 0.6D);
            AbilityFeedback.burstAt(world, clonePos.add(0, 0.8, 0), ParticleTypes.END_ROD, 8, 0.4D);
        }

        AbilityFeedback.burstAt(world, center.add(0, 1.0, 0), ParticleTypes.PORTAL, 25, 0.8D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 1.0F, 1.0F);
        return true;
    }
}
