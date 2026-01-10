package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record TricksterControlPayload(boolean puppeted, boolean mindGames) implements CustomPayload {
    public static final Id<TricksterControlPayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "trickster_control"));
    public static final PacketCodec<RegistryByteBuf, TricksterControlPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.BOOLEAN.cast(), TricksterControlPayload::puppeted,
                    PacketCodecs.BOOLEAN.cast(), TricksterControlPayload::mindGames,
                    TricksterControlPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
