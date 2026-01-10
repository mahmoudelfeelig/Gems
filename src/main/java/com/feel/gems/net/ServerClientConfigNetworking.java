package com.feel.gems.net;

import com.feel.gems.state.GemPlayerState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;




public final class ServerClientConfigNetworking {
    private ServerClientConfigNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ClientPassiveTogglePayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    var player = context.player();
                    GemPlayerState.initIfNeeded(player);
                    GemPlayerState.setPassivesEnabled(player, payload.enabled());
                    GemStateSync.send(player);
                }));
    }
}
