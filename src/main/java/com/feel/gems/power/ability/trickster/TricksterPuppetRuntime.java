package com.feel.gems.power.ability.trickster;

import com.feel.gems.state.PlayerStateManager;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.UUID;

public final class TricksterPuppetRuntime {
    private static final String PUPPETED_BY_KEY = "trickster_puppeted_by";
    private static final String PUPPETED_END_KEY = "trickster_puppeted_end";

    private TricksterPuppetRuntime() {}

    public static void setPuppeted(ServerPlayerEntity target, UUID puppeteerId, int durationTicks) {
        long endTime = target.getEntityWorld().getTime() + durationTicks;
        PlayerStateManager.setPersistent(target, PUPPETED_BY_KEY, puppeteerId.toString());
        PlayerStateManager.setPersistent(target, PUPPETED_END_KEY, String.valueOf(endTime));
    }

    public static UUID getPuppeteer(ServerPlayerEntity player) {
        String puppeteerStr = PlayerStateManager.getPersistent(player, PUPPETED_BY_KEY);
        if (puppeteerStr == null || puppeteerStr.isEmpty()) return null;

        String endStr = PlayerStateManager.getPersistent(player, PUPPETED_END_KEY);
        if (endStr == null) return null;

        long endTime = Long.parseLong(endStr);
        if (player.getEntityWorld().getTime() > endTime) {
            clearPuppeted(player);
            return null;
        }

        try {
            return UUID.fromString(puppeteerStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isPuppeted(ServerPlayerEntity player) {
        return getPuppeteer(player) != null;
    }

    public static void clearPuppeted(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, PUPPETED_BY_KEY);
        PlayerStateManager.clearPersistent(player, PUPPETED_END_KEY);
    }
}
