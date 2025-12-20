package com.feel.gems.net;

import com.feel.gems.power.GemAbilities;
import com.feel.gems.power.SoulSystem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ServerAbilityNetworking {
    private ServerAbilityNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ActivateAbilityPayload.ID, (payload, context) ->
                context.server().execute(() -> GemAbilities.activateByIndex(context.player(), payload.abilityIndex())));

        ServerPlayNetworking.registerGlobalReceiver(SoulReleasePayload.ID, (payload, context) ->
                context.server().execute(() -> SoulSystem.release(context.player())));
    }
}
