package com.feel.gems.power.ability.duelist;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;

public final class DuelistMirrorMatchAbility implements GemAbility {
    public static final String DUEL_PARTNER_KEY = "duelist_mirror_match_partner";
    public static final String DUEL_END_TIME_KEY = "duelist_mirror_match_end";
    public static final String DUEL_ORIGIN_X_KEY = "duelist_mirror_match_origin_x";
    public static final String DUEL_ORIGIN_Y_KEY = "duelist_mirror_match_origin_y";
    public static final String DUEL_ORIGIN_Z_KEY = "duelist_mirror_match_origin_z";
    public static final String DISGUISE_SKIN_KEY = "duelist_mirror_disguise_skin";
    public static final String DISGUISE_NAME_KEY = "duelist_mirror_disguise_name";

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
        return "Force a target into a 1v1 duel; copies your skin and name onto them.";
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
        HitResult hit = player.raycast(range, 0.0F, false);
        if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof ServerPlayerEntity target)) {
            AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        int durationTicks = GemsBalance.v().duelist().mirrorMatchDurationTicks();
        long endTime = world.getTime() + durationTicks;

        // Store duel state for both players
        Vec3d playerOrigin = player.getEntityPos();
        Vec3d targetOrigin = target.getEntityPos();

        // Player state
        PlayerStateManager.setPersistent(player, DUEL_PARTNER_KEY, target.getUuidAsString());
        PlayerStateManager.setPersistent(player, DUEL_END_TIME_KEY, String.valueOf(endTime));
        PlayerStateManager.setPersistent(player, DUEL_ORIGIN_X_KEY, String.valueOf(playerOrigin.x));
        PlayerStateManager.setPersistent(player, DUEL_ORIGIN_Y_KEY, String.valueOf(playerOrigin.y));
        PlayerStateManager.setPersistent(player, DUEL_ORIGIN_Z_KEY, String.valueOf(playerOrigin.z));

        // Target state (they get disguised as the caster)
        PlayerStateManager.setPersistent(target, DUEL_PARTNER_KEY, player.getUuidAsString());
        PlayerStateManager.setPersistent(target, DUEL_END_TIME_KEY, String.valueOf(endTime));
        PlayerStateManager.setPersistent(target, DUEL_ORIGIN_X_KEY, String.valueOf(targetOrigin.x));
        PlayerStateManager.setPersistent(target, DUEL_ORIGIN_Y_KEY, String.valueOf(targetOrigin.y));
        PlayerStateManager.setPersistent(target, DUEL_ORIGIN_Z_KEY, String.valueOf(targetOrigin.z));

        // Store disguise info on target (they look like the caster)
        PlayerStateManager.setPersistent(target, DISGUISE_SKIN_KEY, player.getUuidAsString());
        PlayerStateManager.setPersistent(target, DISGUISE_NAME_KEY, player.getName().getString());

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
        PlayerStateManager.clearPersistent(player, DUEL_ORIGIN_X_KEY);
        PlayerStateManager.clearPersistent(player, DUEL_ORIGIN_Y_KEY);
        PlayerStateManager.clearPersistent(player, DUEL_ORIGIN_Z_KEY);
        PlayerStateManager.clearPersistent(player, DISGUISE_SKIN_KEY);
        PlayerStateManager.clearPersistent(player, DISGUISE_NAME_KEY);
    }
}
