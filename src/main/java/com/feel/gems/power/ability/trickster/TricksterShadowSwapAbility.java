package com.feel.gems.power.ability.trickster;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.util.GemsTeleport;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * Shadow Swap - instantly swap places with the target you're looking at.
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
        return "Instantly swap places with the entity you're looking at.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().trickster().shadowSwapCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();

        // Find target entity
        double range = GemsBalance.v().trickster().shadowSwapRangeBlocks();
        LivingEntity target = Targeting.raycastLiving(player, range);
        if (target == null) {
            player.sendMessage(net.minecraft.text.Text.translatable("gems.message.no_target_in_range").formatted(net.minecraft.util.Formatting.RED), true);
            return false;
        }

        Vec3d playerPos = player.getEntityPos();
        Vec3d targetPos = target.getEntityPos();
        float playerYaw = player.getYaw();
        float playerPitch = player.getPitch();

        // Teleport player to target's position
        GemsTeleport.teleport(player, world, targetPos.x, targetPos.y, targetPos.z, playerYaw, playerPitch);

        // Teleport target to player's old position
        if (target instanceof ServerPlayerEntity targetPlayer) {
            if (!VoidImmunity.canBeTargeted(player, targetPlayer)) {
                // Teleport player back to original position since swap failed
                GemsTeleport.teleport(player, world, playerPos.x, playerPos.y, playerPos.z, playerYaw, playerPitch);
                player.sendMessage(net.minecraft.text.Text.translatable("gems.message.target_immune").formatted(net.minecraft.util.Formatting.RED), true);
                return false;
            }
            GemsTeleport.teleport(targetPlayer, world, playerPos.x, playerPos.y, playerPos.z, targetPlayer.getYaw(), targetPlayer.getPitch());
        } else {
            target.teleport(world, playerPos.x, playerPos.y, playerPos.z, java.util.Set.of(), target.getYaw(), target.getPitch(), false);
        }

        AbilityFeedback.burstAt(world, playerPos.add(0, 1, 0), ParticleTypes.LARGE_SMOKE, 25, 0.6D);
        AbilityFeedback.burstAt(world, targetPos.add(0, 1, 0), ParticleTypes.LARGE_SMOKE, 25, 0.6D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.8F);
        return true;
    }
}
