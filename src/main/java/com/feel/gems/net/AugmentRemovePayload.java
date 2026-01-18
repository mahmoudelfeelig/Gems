package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S: remove an augment from a gem item by index.
 */
public record AugmentRemovePayload(boolean mainHand, int index) implements CustomPayload {
    public static final Id<AugmentRemovePayload> ID =
            new Id<>(Identifier.of(GemsMod.MOD_ID, "augment_remove"));

    public static final PacketCodec<RegistryByteBuf, AugmentRemovePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.BOOLEAN.cast(), AugmentRemovePayload::mainHand,
            PacketCodecs.VAR_INT, AugmentRemovePayload::index,
            AugmentRemovePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
