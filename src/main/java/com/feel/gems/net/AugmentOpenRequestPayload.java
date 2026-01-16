package com.feel.gems.net;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemId;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S: request to open the Augment management screen for a gem.
 */
public record AugmentOpenRequestPayload(GemId gem) implements CustomPayload {
    public static final Id<AugmentOpenRequestPayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "augment_open_request"));
    public static final PacketCodec<RegistryByteBuf, AugmentOpenRequestPayload> CODEC = PacketCodec.ofStatic(
            AugmentOpenRequestPayload::write,
            AugmentOpenRequestPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, AugmentOpenRequestPayload payload) {
        buf.writeString(payload.gem().name(), 32);
    }

    private static AugmentOpenRequestPayload read(RegistryByteBuf buf) {
        String gemName = buf.readString(32);
        return new AugmentOpenRequestPayload(GemId.valueOf(gemName));
    }
}
