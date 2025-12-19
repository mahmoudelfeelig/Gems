package com.blissmc.gems.net;

import com.blissmc.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record StateSyncPayload(int activeGemOrdinal, int energy, int maxHearts) implements CustomPayload {
    public static final Id<StateSyncPayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "state_sync"));
    public static final PacketCodec<RegistryByteBuf, StateSyncPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT.cast(),
            StateSyncPayload::activeGemOrdinal,
            PacketCodecs.VAR_INT.cast(),
            StateSyncPayload::energy,
            PacketCodecs.VAR_INT.cast(),
            StateSyncPayload::maxHearts,
            StateSyncPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
