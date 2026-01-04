package com.feel.gems.power.ability.trickster;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Puppet Master - briefly control an enemy's movement for 3s (they walk where you aim).
 */
public final class TricksterPuppetMasterAbility implements GemAbility {
    public static final String PUPPET_TARGET_KEY = "trickster_puppet_target";
    public static final String PUPPET_END_KEY = "trickster_puppet_end";

    @Override
    public Identifier id() {
        return PowerIds.TRICKSTER_PUPPET_MASTER;
    }

    @Override
    public String name() {
        return "Puppet Master";
    }

    @Override
    public String description() {
        return "Briefly control an enemy's movement for 3s.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().trickster().puppetMasterCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int range = GemsBalance.v().trickster().puppetMasterRangeBlocks();
        int durationTicks = GemsBalance.v().trickster().puppetMasterDurationTicks();

        // Raycast to find target
        HitResult hit = player.raycast(range, 0.0F, false);
        if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof ServerPlayerEntity target)) {
            AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        long endTime = world.getTime() + durationTicks;
        PlayerStateManager.setPersistent(player, PUPPET_TARGET_KEY, target.getUuidAsString());
        PlayerStateManager.setPersistent(player, PUPPET_END_KEY, String.valueOf(endTime));

        // Mark target as being puppeted
        TricksterPuppetRuntime.setPuppeted(target, player.getUuid(), durationTicks);

        AbilityFeedback.burstAt(world, target.getEntityPos().add(0, 2, 0), ParticleTypes.ENCHANT, 25, 0.8D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_VEX_CHARGE, 1.0F, 0.6F);
        AbilityFeedback.sound(target, SoundEvents.ENTITY_VEX_CHARGE, 1.0F, 0.6F);
        return true;
    }

    public static ServerPlayerEntity getPuppetTarget(ServerPlayerEntity puppeteer) {
        String targetStr = PlayerStateManager.getPersistent(puppeteer, PUPPET_TARGET_KEY);
        if (targetStr == null || targetStr.isEmpty()) return null;

        String endStr = PlayerStateManager.getPersistent(puppeteer, PUPPET_END_KEY);
        if (endStr == null) return null;

        long endTime = Long.parseLong(endStr);
        if (puppeteer.getEntityWorld().getTime() > endTime) {
            clearPuppet(puppeteer);
            return null;
        }

        try {
            java.util.UUID targetId = java.util.UUID.fromString(targetStr);
            return puppeteer.getEntityWorld().getServer().getPlayerManager().getPlayer(targetId);
        } catch (Exception e) {
            return null;
        }
    }

    public static void clearPuppet(ServerPlayerEntity puppeteer) {
        PlayerStateManager.clearPersistent(puppeteer, PUPPET_TARGET_KEY);
        PlayerStateManager.clearPersistent(puppeteer, PUPPET_END_KEY);
    }
}
