package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server request to open the Spy observed abilities screen.
 */
public record SpyObservedOpenRequestPayload() implements CustomPayload {
    public static final SpyObservedOpenRequestPayload INSTANCE = new SpyObservedOpenRequestPayload();

    public static final Id<SpyObservedOpenRequestPayload> ID =
            new Id<>(Identifier.of(GemsMod.MOD_ID, "spy_observed_open"));

    public static final PacketCodec<PacketByteBuf, SpyObservedOpenRequestPayload> CODEC =
            PacketCodec.unit(INSTANCE);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
