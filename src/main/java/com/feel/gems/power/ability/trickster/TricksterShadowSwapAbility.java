package com.feel.gems.power.ability.trickster;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.util.GemsTeleport;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * Shadow Swap - instantly swap places with your shadow clone (must have clone active).
 */
public final class TricksterShadowSwapAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TRICKSTER_SHADOW_SWAP;
    }

    @Override
    public String name() {
        return "Shadow Swap";
    }

    @Override
    public String description() {
        return "Instantly swap places with your shadow clone.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().trickster().shadowSwapCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();

        // Check if player has an active shadow clone
        Vec3d clonePos = TricksterShadowCloneRuntime.getClonePosition(player);
        if (clonePos == null) {
            // No clone - create one at current position, then dash forward
            Vec3d currentPos = player.getEntityPos();
            int cloneDuration = GemsBalance.v().trickster().shadowSwapCloneDurationTicks();
            TricksterShadowCloneRuntime.createClone(player, currentPos, cloneDuration);

            // Dash forward slightly
            Vec3d dir = player.getRotationVec(1.0F).normalize();
            double dashDist = 5.0;
            Vec3d newPos = currentPos.add(dir.multiply(dashDist));
            GemsTeleport.teleport(player, world, newPos.x, newPos.y, newPos.z, player.getYaw(), player.getPitch());

            AbilityFeedback.burstAt(world, currentPos.add(0, 1, 0), ParticleTypes.LARGE_SMOKE, 20, 0.5D);
            AbilityFeedback.burstAt(world, newPos.add(0, 1, 0), ParticleTypes.LARGE_SMOKE, 20, 0.5D);
            AbilityFeedback.sound(player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.2F);
            return true;
        }

        // Swap with existing clone
        Vec3d playerPos = player.getEntityPos();
        float playerYaw = player.getYaw();
        float playerPitch = player.getPitch();

        GemsTeleport.teleport(player, world, clonePos.x, clonePos.y, clonePos.z, playerYaw, playerPitch);

        // Move clone to player's old position
        TricksterShadowCloneRuntime.moveClone(player, playerPos);

        AbilityFeedback.burstAt(world, playerPos.add(0, 1, 0), ParticleTypes.LARGE_SMOKE, 25, 0.6D);
        AbilityFeedback.burstAt(world, clonePos.add(0, 1, 0), ParticleTypes.LARGE_SMOKE, 25, 0.6D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.8F);
        return true;
    }
}
