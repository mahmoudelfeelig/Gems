package com.feel.gems.power.gem.hunter;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import java.util.UUID;

public final class HunterPreyMarkRuntime {
    private static final String MARK_TARGET_KEY = "hunter_prey_mark_target";
    private static final String MARK_END_KEY = "hunter_prey_mark_end";

    private HunterPreyMarkRuntime() {}

    public static void applyMark(ServerPlayerEntity hunter, LivingEntity target) {
        if (!GemPowers.isPassiveActive(hunter, PowerIds.HUNTER_PREY_MARK)) {
            return;
        }

        int durationTicks = GemsBalance.v().hunter().preyMarkDurationTicks();
        long endTime = hunter.getEntityWorld().getTime() + durationTicks;

        PlayerStateManager.setPersistent(hunter, MARK_TARGET_KEY, target.getUuidAsString());
        PlayerStateManager.setPersistent(hunter, MARK_END_KEY, String.valueOf(endTime));

        // Apply glowing to target if tracker's eye is active
        if (GemPowers.isPassiveActive(hunter, PowerIds.HUNTER_TRACKERS_EYE)) {
            int range = GemsBalance.v().hunter().trackersEyeRangeBlocks();
            if (range <= 0 || hunter.squaredDistanceTo(target) <= (double) range * range) {
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, durationTicks, 0, false, false));
            }
        }
    }

    public static LivingEntity getMarkedTarget(ServerPlayerEntity hunter) {
        String targetStr = PlayerStateManager.getPersistent(hunter, MARK_TARGET_KEY);
        if (targetStr == null || targetStr.isEmpty()) return null;

        String endStr = PlayerStateManager.getPersistent(hunter, MARK_END_KEY);
        if (endStr == null) return null;

        long endTime = Long.parseLong(endStr);
        if (hunter.getEntityWorld().getTime() > endTime) {
            clearMark(hunter);
            return null;
        }

        try {
            UUID targetId = UUID.fromString(targetStr);
            MinecraftServer server = hunter.getEntityWorld().getServer();
            if (server == null) return null;
            var entity = SummonerSummons.findEntity(server, targetId);
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                return null;
            }
            return living;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isMarked(ServerPlayerEntity hunter, LivingEntity target) {
        LivingEntity marked = getMarkedTarget(hunter);
        return marked != null && marked.getUuid().equals(target.getUuid());
    }

    public static float getDamageMultiplier() {
        return GemsBalance.v().hunter().preyMarkBonusDamageMultiplier();
    }

    public static void tickTrackersEye(ServerPlayerEntity hunter) {
        if (!GemPowers.isPassiveActive(hunter, PowerIds.HUNTER_TRACKERS_EYE)) {
            return;
        }
        LivingEntity marked = getMarkedTarget(hunter);
        if (marked == null) {
            return;
        }
        if (marked instanceof ServerPlayerEntity markedPlayer
                && GemPowers.isPassiveActive(markedPlayer, PowerIds.SPY_FALSE_SIGNATURE)) {
            return;
        }
        int range = GemsBalance.v().hunter().trackersEyeRangeBlocks();
        if (range > 0 && hunter.squaredDistanceTo(marked) > (double) range * range) {
            return;
        }
        marked.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0, false, false));
    }

    public static void clearMark(ServerPlayerEntity hunter) {
        PlayerStateManager.clearPersistent(hunter, MARK_TARGET_KEY);
        PlayerStateManager.clearPersistent(hunter, MARK_END_KEY);
    }
}
