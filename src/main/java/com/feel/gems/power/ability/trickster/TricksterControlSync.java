package com.feel.gems.power.ability.trickster;

import com.feel.gems.net.TricksterControlPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class TricksterControlSync {
    private TricksterControlSync() {
    }

    public static void sync(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        boolean puppeted = TricksterPuppetRuntime.isPuppeted(player);
        boolean mindGames = TricksterMindGamesRuntime.hasReversedControls(player);
        ServerPlayNetworking.send(player, new TricksterControlPayload(puppeted, mindGames));
    }
}
