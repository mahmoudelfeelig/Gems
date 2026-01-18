package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S: request to open the inscription screen for a legendary item in-hand.
 */
public record InscriptionOpenRequestPayload(boolean mainHand) implements CustomPayload {
    public static final Id<InscriptionOpenRequestPayload> ID =
            new Id<>(Identifier.of(GemsMod.MOD_ID, "inscription_open_request"));

    public static final PacketCodec<RegistryByteBuf, InscriptionOpenRequestPayload> CODEC = PacketCodec.ofStatic(
            InscriptionOpenRequestPayload::write,
            InscriptionOpenRequestPayload::read
    );

    public static final InscriptionOpenRequestPayload MAIN_HAND = new InscriptionOpenRequestPayload(true);
    public static final InscriptionOpenRequestPayload OFF_HAND = new InscriptionOpenRequestPayload(false);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, InscriptionOpenRequestPayload payload) {
        buf.writeBoolean(payload.mainHand());
    }

    private static InscriptionOpenRequestPayload read(RegistryByteBuf buf) {
        return new InscriptionOpenRequestPayload(buf.readBoolean());
    }
}
