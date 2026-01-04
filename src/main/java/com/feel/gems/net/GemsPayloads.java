package com.feel.gems.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;




public final class GemsPayloads {
    private static boolean registered = false;

    private GemsPayloads() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        PayloadTypeRegistry.playC2S().register(ActivateAbilityPayload.ID, ActivateAbilityPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SoulReleasePayload.ID, SoulReleasePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(FluxChargePayload.ID, FluxChargePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ClientPassiveTogglePayload.ID, ClientPassiveTogglePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SummonerLoadoutOpenRequestPayload.ID, SummonerLoadoutOpenRequestPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SummonerLoadoutSavePayload.ID, SummonerLoadoutSavePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TrackerCompassSelectPayload.ID, TrackerCompassSelectPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StateSyncPayload.ID, StateSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CooldownSnapshotPayload.ID, CooldownSnapshotPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AbilityCooldownPayload.ID, AbilityCooldownPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ExtraStatePayload.ID, ExtraStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SummonerLoadoutScreenPayload.ID, SummonerLoadoutScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(TrackerCompassScreenPayload.ID, TrackerCompassScreenPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SpySkinshiftPayload.ID, SpySkinshiftPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerDisablesPayload.ID, ServerDisablesPayload.CODEC);
        registered = true;
    }
}
