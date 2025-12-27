package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;




public enum SoulReleasePayload implements CustomPayload {
    INSTANCE;

    public static final Id<SoulReleasePayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "soul_release"));
    public static final PacketCodec<RegistryByteBuf, SoulReleasePayload> CODEC = PacketCodec.unit(INSTANCE);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

