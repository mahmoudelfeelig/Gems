package com.blissmc.gems.net;

import com.blissmc.gems.core.GemId;
import com.blissmc.gems.power.FluxCharge;
import com.blissmc.gems.power.SoulSystem;
import com.blissmc.gems.state.GemPlayerState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public final class GemExtraStateSync {
    private GemExtraStateSync() {
    }

    public static void send(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);
        GemId active = GemPlayerState.getActiveGem(player);

        int flux = FluxCharge.get(player);
        boolean hasSoul = SoulSystem.hasSoul(player);
        String soulType = hasSoul ? SoulSystem.soulType(player) : "";

        ServerPlayNetworking.send(player, new ExtraStatePayload(active.ordinal(), flux, hasSoul, soulType));
    }
}

