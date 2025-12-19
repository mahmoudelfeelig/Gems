package com.blissmc.gems.net;

import com.blissmc.gems.power.GemAbilities;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ServerAbilityNetworking {
    private ServerAbilityNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ActivateAbilityPayload.ID, (payload, context) ->
                context.server().execute(() -> GemAbilities.activateByIndex(context.player(), payload.abilityIndex())));
    }
}
