package com.feel.gems.power.ability.sentinel;

import com.feel.gems.state.PlayerStateManager;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.UUID;

public final class SentinelTauntRuntime {
    private static final String TAUNTED_BY_KEY = "sentinel_taunted_by";
    private static final String TAUNTED_END_KEY = "sentinel_taunted_end";

    private SentinelTauntRuntime() {}

    public static void applyTaunt(ServerPlayerEntity target, UUID taunterId, int durationTicks) {
        long endTime = target.getEntityWorld().getTime() + durationTicks;
        PlayerStateManager.setPersistent(target, TAUNTED_BY_KEY, taunterId.toString());
        PlayerStateManager.setPersistent(target, TAUNTED_END_KEY, String.valueOf(endTime));
    }

    public static UUID getTaunter(ServerPlayerEntity player) {
        String taunterStr = PlayerStateManager.getPersistent(player, TAUNTED_BY_KEY);
        if (taunterStr == null || taunterStr.isEmpty()) return null;

        String endStr = PlayerStateManager.getPersistent(player, TAUNTED_END_KEY);
        if (endStr == null) return null;

        long endTime = Long.parseLong(endStr);
        if (player.getEntityWorld().getTime() > endTime) {
            clearTaunt(player);
            return null;
        }

        try {
            return UUID.fromString(taunterStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isTaunted(ServerPlayerEntity player) {
        return getTaunter(player) != null;
    }

    public static void clearTaunt(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, TAUNTED_BY_KEY);
        PlayerStateManager.clearPersistent(player, TAUNTED_END_KEY);
    }
}
