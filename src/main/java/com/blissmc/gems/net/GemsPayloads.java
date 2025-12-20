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
        PayloadTypeRegistry.playS2C().register(StateSyncPayload.ID, StateSyncPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CooldownSnapshotPayload.ID, CooldownSnapshotPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AbilityCooldownPayload.ID, AbilityCooldownPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ExtraStatePayload.ID, ExtraStatePayload.CODEC);
        registered = true;
    }
}
