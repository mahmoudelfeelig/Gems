package com.feel.gems.admin;

import com.feel.gems.power.runtime.GemAbilityCooldowns;
import com.feel.gems.state.PlayerNbt;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Small persistent admin toggles (per-player).
 */
public final class GemsAdmin {
    private static final String KEY_NO_COOLDOWNS = "gemsAdminNoCooldowns";

    private GemsAdmin() {
    }

    public static boolean noCooldowns(ServerPlayerEntity player) {
        return PlayerNbt.getBoolean(player, KEY_NO_COOLDOWNS, false);
    }

    public static void setNoCooldowns(ServerPlayerEntity player, boolean enabled) {
        PlayerNbt.putBoolean(player, KEY_NO_COOLDOWNS, enabled);
        if (enabled) {
            GemAbilityCooldowns.clearAll(player);
        }
    }
}

