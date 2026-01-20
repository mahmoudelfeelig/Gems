package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Syncs rivalry target information to the client for HUD display.
 */
public record RivalrySyncPayload(String targetName) implements CustomPayload {
    public static final Id<RivalrySyncPayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "rivalry_sync"));
    public static final PacketCodec<RegistryByteBuf, RivalrySyncPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.STRING.cast(), RivalrySyncPayload::targetName, RivalrySyncPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
