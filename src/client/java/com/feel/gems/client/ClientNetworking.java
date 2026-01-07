package com.feel.gems.client;

import com.feel.gems.bonus.BonusAbilityRuntime;
import com.feel.gems.client.screen.BonusSelectionScreen;
import com.feel.gems.client.screen.PrismSelectionScreen;
import com.feel.gems.client.screen.SummonerLoadoutScreen;
import com.feel.gems.client.screen.TrackerCompassScreen;
import com.feel.gems.core.GemId;
import com.feel.gems.net.AbilityCooldownPayload;
import com.feel.gems.net.BonusAbilitiesSyncPayload;
import com.feel.gems.net.BonusSelectionScreenPayload;
import com.feel.gems.net.ChaosSlotPayload;
import com.feel.gems.net.ChaosStatePayload;
import com.feel.gems.net.CooldownSnapshotPayload;
import com.feel.gems.net.ExtraStatePayload;
import com.feel.gems.net.PrismAbilitiesSyncPayload;
import com.feel.gems.net.PrismSelectionScreenPayload;
import com.feel.gems.net.ServerDisablesPayload;
import com.feel.gems.net.StateSyncPayload;
import com.feel.gems.net.SummonerLoadoutScreenPayload;
import com.feel.gems.net.TrackerCompassScreenPayload;
import com.feel.gems.net.SpySkinshiftPayload;
import com.feel.gems.net.payloads.ShadowCloneSyncPayload;
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
            ClientBonusState.reset();
            ClientShadowCloneState.reset();
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
                context.client().execute(() -> {
                    int abilityIndex = payload.abilityIndex();
                    // Check if this is a bonus ability cooldown (offset by 100)
                    if (abilityIndex >= BonusAbilityRuntime.BONUS_ABILITY_INDEX_OFFSET) {
                        int bonusSlot = abilityIndex - BonusAbilityRuntime.BONUS_ABILITY_INDEX_OFFSET;
                        ClientBonusState.setCooldown(bonusSlot, payload.cooldownTicks());
                    } else {
                        ClientCooldowns.setCooldown(
                                safeGemId(payload.activeGemOrdinal()),
                                abilityIndex,
                                payload.cooldownTicks()
                        );
                    }
                }));

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

        // Bonus abilities sync (for HUD)
        ClientPlayNetworking.registerGlobalReceiver(BonusAbilitiesSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientBonusState.update(payload))
        );

        // Prism abilities sync (for HUD)
        ClientPlayNetworking.registerGlobalReceiver(PrismAbilitiesSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientPrismState.update(payload))
        );

        // Shadow clone owner sync (for skin rendering)
        ClientPlayNetworking.registerGlobalReceiver(ShadowCloneSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> ClientShadowCloneState.setOwner(payload.entityId(), payload.ownerUuid()))
        );

        // Prism selection screen
        ClientPlayNetworking.registerGlobalReceiver(PrismSelectionScreenPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    MinecraftClient client = context.client();
                    if (client != null) {
                        client.setScreen(new PrismSelectionScreen(payload));
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
