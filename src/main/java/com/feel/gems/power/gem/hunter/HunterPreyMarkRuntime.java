package com.feel.gems.power.gem.hunter;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import java.util.UUID;

public final class HunterPreyMarkRuntime {
    private static final String MARK_TARGET_KEY = "hunter_prey_mark_target";
    private static final String MARK_END_KEY = "hunter_prey_mark_end";

    private HunterPreyMarkRuntime() {}

    public static void applyMark(ServerPlayerEntity hunter, ServerPlayerEntity target) {
        if (!GemPowers.isPassiveActive(hunter, PowerIds.HUNTER_PREY_MARK)) {
            return;
        }

        int durationTicks = GemsBalance.v().hunter().preyMarkDurationTicks();
        long endTime = hunter.getEntityWorld().getTime() + durationTicks;

        PlayerStateManager.setPersistent(hunter, MARK_TARGET_KEY, target.getUuidAsString());
        PlayerStateManager.setPersistent(hunter, MARK_END_KEY, String.valueOf(endTime));

        // Apply glowing to target if tracker's eye is active
        if (GemPowers.isPassiveActive(hunter, PowerIds.HUNTER_TRACKERS_EYE)) {
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, durationTicks, 0, false, false));
        }
    }

    public static ServerPlayerEntity getMarkedTarget(ServerPlayerEntity hunter) {
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
            return server.getPlayerManager().getPlayer(targetId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isMarked(ServerPlayerEntity hunter, ServerPlayerEntity target) {
        ServerPlayerEntity marked = getMarkedTarget(hunter);
        return marked != null && marked.getUuid().equals(target.getUuid());
    }

    public static float getDamageMultiplier() {
        return GemsBalance.v().hunter().preyMarkBonusDamageMultiplier();
    }

    public static void clearMark(ServerPlayerEntity hunter) {
        PlayerStateManager.clearPersistent(hunter, MARK_TARGET_KEY);
        PlayerStateManager.clearPersistent(hunter, MARK_END_KEY);
    }
}
