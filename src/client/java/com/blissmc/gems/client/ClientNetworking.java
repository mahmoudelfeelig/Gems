package com.feel.gems.client;

import com.feel.gems.core.GemId;
import com.feel.gems.net.AbilityCooldownPayload;
import com.feel.gems.net.CooldownSnapshotPayload;
import com.feel.gems.net.ExtraStatePayload;
import com.feel.gems.net.StateSyncPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class ClientNetworking {
    private ClientNetworking() {
    }

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            ClientGemState.reset();
            ClientCooldowns.reset();
            ClientExtraState.reset();
            ClientAbilitySelection.reset();
        }));

        ClientPlayNetworking.registerGlobalReceiver(StateSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    GemId gem = safeGemId(payload.activeGemOrdinal());
                    ClientGemState.update(gem, payload.energy(), payload.maxHearts());
                    ClientCooldowns.clearIfGemChanged(gem);
                }));

        ClientPlayNetworking.registerGlobalReceiver(CooldownSnapshotPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientCooldowns.applySnapshot(
                        safeGemId(payload.activeGemOrdinal()),
                        payload.remainingAbilityCooldownTicks()
                )));

        ClientPlayNetworking.registerGlobalReceiver(AbilityCooldownPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientCooldowns.setCooldown(
                        safeGemId(payload.activeGemOrdinal()),
                        payload.abilityIndex(),
                        payload.cooldownTicks()
                )));

        ClientPlayNetworking.registerGlobalReceiver(ExtraStatePayload.ID, (payload, context) ->
                context.client().execute(() -> ClientExtraState.update(
                        payload.fluxChargePercent(),
                        payload.hasSoul(),
                        payload.soulTypeId()
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
