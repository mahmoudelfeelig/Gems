package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S: client requests opening the bonus selection screen.
 */
public record BonusSelectionOpenRequestPayload() implements CustomPayload {

    public static final BonusSelectionOpenRequestPayload INSTANCE = new BonusSelectionOpenRequestPayload();

    public static final Id<BonusSelectionOpenRequestPayload> ID = 
            new Id<>(Identifier.of(GemsMod.MOD_ID, "bonus_selection_open"));

    public static final PacketCodec<RegistryByteBuf, BonusSelectionOpenRequestPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {},
            buf -> INSTANCE
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
