package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S: request to consume 1 charge item for the Flux gem.
 */
public enum FluxChargePayload implements CustomPayload {
    INSTANCE;

    public static final Id<FluxChargePayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "flux_charge"));
    public static final PacketCodec<RegistryByteBuf, FluxChargePayload> CODEC = PacketCodec.unit(INSTANCE);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

