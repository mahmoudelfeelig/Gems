package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S: player selects a title to display (or clears it with an empty id).
 */
public record TitleSelectionSelectPayload(String titleId) implements CustomPayload {
    public static final Id<TitleSelectionSelectPayload> ID =
            new Id<>(Identifier.of(GemsMod.MOD_ID, "title_selection_select"));

    public static final PacketCodec<RegistryByteBuf, TitleSelectionSelectPayload> CODEC = PacketCodec.ofStatic(
            TitleSelectionSelectPayload::write,
            TitleSelectionSelectPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, TitleSelectionSelectPayload payload) {
        String raw = payload.titleId == null ? "" : payload.titleId;
        buf.writeString(raw, 128);
    }

    private static TitleSelectionSelectPayload read(RegistryByteBuf buf) {
        return new TitleSelectionSelectPayload(buf.readString(128));
    }
}
