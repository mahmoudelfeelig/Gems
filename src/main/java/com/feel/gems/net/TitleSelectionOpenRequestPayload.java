package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S: client requests opening the title selection screen.
 */
public record TitleSelectionOpenRequestPayload() implements CustomPayload {
    public static final TitleSelectionOpenRequestPayload INSTANCE = new TitleSelectionOpenRequestPayload();

    public static final Id<TitleSelectionOpenRequestPayload> ID =
            new Id<>(Identifier.of(GemsMod.MOD_ID, "title_selection_open"));

    public static final PacketCodec<RegistryByteBuf, TitleSelectionOpenRequestPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {},
            buf -> INSTANCE
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
