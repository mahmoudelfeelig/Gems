package com.feel.gems.client;

import com.feel.gems.client.screen.BonusSelectionScreen;
import com.feel.gems.client.screen.SummonerLoadoutScreen;
import com.feel.gems.client.screen.TrackerCompassScreen;
import com.feel.gems.core.GemId;
import com.feel.gems.net.AbilityCooldownPayload;
import com.feel.gems.net.BonusSelectionScreenPayload;
import com.feel.gems.net.ChaosSlotPayload;
import com.feel.gems.net.ChaosStatePayload;
import com.feel.gems.net.CooldownSnapshotPayload;
import com.feel.gems.net.ExtraStatePayload;
import com.feel.gems.net.ServerDisablesPayload;
import com.feel.gems.net.StateSyncPayload;
import com.feel.gems.net.SummonerLoadoutScreenPayload;
import com.feel.gems.net.TrackerCompassScreenPayload;
import com.feel.gems.net.SpySkinshiftPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;




public final class ClientNetworking {
    private ClientNetworking() {
    }

    public static void register() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            ClientGemState.reset();
            ClientCooldowns.reset();
            ClientExtraState.reset();
            ClientAbilitySelection.reset();
            ClientChaosState.reset();
            ClientDisguiseState.reset();
            ClientDisables.reset();
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

        ClientPlayNetworking.registerGlobalReceiver(SummonerLoadoutScreenPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    MinecraftClient client = context.client();
                    if (client != null) {
                        client.setScreen(new SummonerLoadoutScreen(payload));
                    }
                }));

        ClientPlayNetworking.registerGlobalReceiver(TrackerCompassScreenPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    MinecraftClient client = context.client();
                    if (client != null) {
                        client.setScreen(new TrackerCompassScreen(payload));
                    }
                }));
        ClientPlayNetworking.registerGlobalReceiver(SpySkinshiftPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientDisguiseState.update(payload.player(), payload.target()))
        );

        ClientPlayNetworking.registerGlobalReceiver(ServerDisablesPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientDisables.update(payload))
        );

        // Legacy chaos state (kept for compatibility)
        ClientPlayNetworking.registerGlobalReceiver(ChaosStatePayload.ID, (payload, context) -> {
            // Old system - ignore
        });
        
        // New chaos slots system
        ClientPlayNetworking.registerGlobalReceiver(ChaosSlotPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientChaosState.update(payload))
        );

        // Bonus selection screen
        ClientPlayNetworking.registerGlobalReceiver(BonusSelectionScreenPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    MinecraftClient client = context.client();
                    if (client != null) {
                        client.setScreen(new BonusSelectionScreen(payload));
                    }
                }));
    }

    private static GemId safeGemId(int ordinal) {
        GemId[] values = GemId.values();
        if (ordinal < 0 || ordinal >= values.length) {
            return GemId.ASTRA;
        }
        return values[ordinal];
    }
}
