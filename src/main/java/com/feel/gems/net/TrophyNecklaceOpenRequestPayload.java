package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server request to open the Trophy Necklace screen.
 */
public record TrophyNecklaceOpenRequestPayload() implements CustomPayload {
    public static final TrophyNecklaceOpenRequestPayload INSTANCE = new TrophyNecklaceOpenRequestPayload();

    public static final Id<TrophyNecklaceOpenRequestPayload> ID =
            new Id<>(Identifier.of(GemsMod.MOD_ID, "trophy_necklace_open"));

    public static final PacketCodec<PacketByteBuf, TrophyNecklaceOpenRequestPayload> CODEC =
            PacketCodec.unit(INSTANCE);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
