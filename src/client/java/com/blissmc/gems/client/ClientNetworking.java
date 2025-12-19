package com.blissmc.gems.client;

import com.blissmc.gems.core.GemId;
import com.blissmc.gems.net.StateSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientNetworking {
    private ClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(StateSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientGemState.update(
                        safeGemId(payload.activeGemOrdinal()),
                        payload.energy(),
                        payload.maxHearts()
                )));
    }

    private static GemId safeGemId(int ordinal) {
        GemId[] values = GemId.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return GemId.ASTRA;
        }
        return values[ordinal];
    }
}
