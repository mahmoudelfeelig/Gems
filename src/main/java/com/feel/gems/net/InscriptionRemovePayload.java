package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S: remove a legendary inscription by index from the selected hand.
 */
public record InscriptionRemovePayload(boolean mainHand, int index) implements CustomPayload {
    public static final Id<InscriptionRemovePayload> ID =
            new Id<>(Identifier.of(GemsMod.MOD_ID, "inscription_remove"));

    public static final PacketCodec<net.minecraft.network.RegistryByteBuf, InscriptionRemovePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN.cast(), InscriptionRemovePayload::mainHand,
            PacketCodecs.VAR_INT, InscriptionRemovePayload::index,
            InscriptionRemovePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
