package com.feel.gems.power.ability.duelist;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public final class DuelistMirrorMatchAbility implements GemAbility {
    public static final String DUEL_PARTNER_KEY = "duelist_mirror_match_partner";
    public static final String DUEL_END_TIME_KEY = "duelist_mirror_match_end";

    @Override
    public Identifier id() {
        return PowerIds.DUELIST_MIRROR_MATCH;
    }

    @Override
    public String name() {
        return "Mirror Match";
    }

    @Override
    public String description() {
        return "Swap skins and names with a targeted player for a short duration.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().duelist().mirrorMatchCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int range = GemsBalance.v().duelist().mirrorMatchRangeBlocks();

        // Raycast to find target
        ServerPlayerEntity target = Targeting.raycastPlayer(player, range);
        if (target == null) {
            AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        if (!VoidImmunity.canBeTargeted(player, target)) {
            AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        int durationTicks = GemsBalance.v().duelist().mirrorMatchDurationTicks();
        long endTime = world.getTime() + durationTicks;

        // Store mirror state for both players
        PlayerStateManager.setPersistent(player, DUEL_PARTNER_KEY, target.getUuidAsString());
        PlayerStateManager.setPersistent(player, DUEL_END_TIME_KEY, String.valueOf(endTime));

        // Target state
        PlayerStateManager.setPersistent(target, DUEL_PARTNER_KEY, player.getUuidAsString());
        PlayerStateManager.setPersistent(target, DUEL_END_TIME_KEY, String.valueOf(endTime));

        DuelistMirrorMatchRuntime.start(player, target);

        // Visual effects
        AbilityFeedback.burstAt(world, player.getEntityPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.ENCHANT, 30, 1.0D);
        AbilityFeedback.burstAt(world, target.getEntityPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.ENCHANT, 30, 1.0D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 1.0F, 1.0F);
        AbilityFeedback.sound(target, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 1.0F, 1.0F);

        return true;
    }

    public static boolean isInDuel(ServerPlayerEntity player) {
        String partner = PlayerStateManager.getPersistent(player, DUEL_PARTNER_KEY);
        if (partner == null || partner.isEmpty()) {
            return false;
        }
        String endTimeStr = PlayerStateManager.getPersistent(player, DUEL_END_TIME_KEY);
        if (endTimeStr == null) {
            return false;
        }
        long endTime = Long.parseLong(endTimeStr);
        return player.getEntityWorld().getTime() < endTime;
    }

    public static void clearDuel(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, DUEL_PARTNER_KEY);
        PlayerStateManager.clearPersistent(player, DUEL_END_TIME_KEY);
    }
}
