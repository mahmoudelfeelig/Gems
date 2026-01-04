package com.feel.gems.power.ability.sentinel;

import com.feel.gems.state.PlayerStateManager;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.UUID;

public final class SentinelInterventionRuntime {
    private static final String PROTECTING_KEY = "sentinel_intervention_protecting";
    private static final String PROTECTED_BY_KEY = "sentinel_intervention_protected_by";

    private SentinelInterventionRuntime() {}

    public static void setProtecting(ServerPlayerEntity sentinel, UUID allyId) {
        // Sentinel is protecting the ally
        PlayerStateManager.setPersistent(sentinel, PROTECTING_KEY, allyId.toString());
    }

    public static UUID getProtectedAlly(ServerPlayerEntity sentinel) {
        String allyStr = PlayerStateManager.getPersistent(sentinel, PROTECTING_KEY);
        if (allyStr == null || allyStr.isEmpty()) return null;
        try {
            return UUID.fromString(allyStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isProtecting(ServerPlayerEntity sentinel) {
        return getProtectedAlly(sentinel) != null;
    }

    public static void consumeProtection(ServerPlayerEntity sentinel) {
        PlayerStateManager.clearPersistent(sentinel, PROTECTING_KEY);
    }

    /**
     * Called when the protected ally would take damage.
     * Returns the sentinel who should take the damage instead, or null if no protection.
     */
    public static ServerPlayerEntity getProtector(ServerPlayerEntity ally) {
        // Search for a sentinel protecting this ally
        // This would need to iterate through online players - simplified here
        return null; // Actual implementation would use a reverse lookup
    }

    public static void clearProtection(ServerPlayerEntity sentinel) {
        PlayerStateManager.clearPersistent(sentinel, PROTECTING_KEY);
    }
}
