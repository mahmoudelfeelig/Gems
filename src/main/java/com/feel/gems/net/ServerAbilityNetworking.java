package com.feel.gems.net;

import com.feel.gems.core.GemId;
import com.feel.gems.power.FluxCharge;
import com.feel.gems.power.GemAbilities;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.power.SoulSystem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;

public final class ServerAbilityNetworking {
    private ServerAbilityNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(ActivateAbilityPayload.ID, (payload, context) ->
                context.server().execute(() -> GemAbilities.activateByIndex(context.player(), payload.abilityIndex())));

        ServerPlayNetworking.registerGlobalReceiver(SoulReleasePayload.ID, (payload, context) ->
                context.server().execute(() -> SoulSystem.release(context.player())));

        ServerPlayNetworking.registerGlobalReceiver(FluxChargePayload.ID, (payload, context) ->
                context.server().execute(() -> {
                    var player = context.player();
                    GemPlayerState.initIfNeeded(player);
                    if (GemPlayerState.getActiveGem(player) != GemId.FLUX) {
                        player.sendMessage(Text.literal("Flux Charge: active gem is not Flux."), true);
                        return;
                    }
                    if (FluxCharge.tryConsumeChargeItem(player)) {
                        GemStateSync.send(player);
                    }
                }));
    }
}
