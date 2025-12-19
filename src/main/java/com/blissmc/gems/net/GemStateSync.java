package com.blissmc.gems.net;

import com.blissmc.gems.core.GemId;
import com.blissmc.gems.state.GemPlayerState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class GemStateSync {
    private GemStateSync() {
    }

    public static void send(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);

        GemId active = GemPlayerState.getActiveGem(player);
        ServerPlayNetworking.send(player, new StateSyncPayload(
                active.ordinal(),
                GemPlayerState.getEnergy(player),
                GemPlayerState.getMaxHearts(player)
        ));
    }
}
