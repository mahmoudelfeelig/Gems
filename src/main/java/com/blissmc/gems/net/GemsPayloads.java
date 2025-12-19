package com.blissmc.gems.net;

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
        PayloadTypeRegistry.playS2C().register(StateSyncPayload.ID, StateSyncPayload.CODEC);
        registered = true;
    }
}
