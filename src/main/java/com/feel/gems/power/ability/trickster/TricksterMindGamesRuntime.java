package com.feel.gems.power.ability.trickster;

import com.feel.gems.state.PlayerStateManager;
import net.minecraft.server.network.ServerPlayerEntity;

public final class TricksterMindGamesRuntime {
    private static final String MIND_GAMES_END_KEY = "trickster_mind_games_end";

    private TricksterMindGamesRuntime() {}

    public static void applyMindGames(ServerPlayerEntity player, int durationTicks) {
        long endTime = player.getEntityWorld().getTime() + durationTicks;
        PlayerStateManager.setPersistent(player, MIND_GAMES_END_KEY, String.valueOf(endTime));
    }

    public static boolean hasReversedControls(ServerPlayerEntity player) {
        String endStr = PlayerStateManager.getPersistent(player, MIND_GAMES_END_KEY);
        if (endStr == null) return false;

        long endTime = Long.parseLong(endStr);
        if (player.getEntityWorld().getTime() > endTime) {
            clearMindGames(player);
            return false;
        }

        return true;
    }

    public static void clearMindGames(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, MIND_GAMES_END_KEY);
    }
}
