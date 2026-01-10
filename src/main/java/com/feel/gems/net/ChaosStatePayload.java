package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Syncs the current chaos ability/passive names and IDs to the client for HUD display.
 */
public record ChaosStatePayload(String abilityName, String abilityId, String passiveName, int rotationSecondsRemaining) implements CustomPayload {
    public static final Id<ChaosStatePayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "chaos_state"));
    public static final PacketCodec<RegistryByteBuf, ChaosStatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, ChaosStatePayload::abilityName,
            PacketCodecs.STRING, ChaosStatePayload::abilityId,
            PacketCodecs.STRING, ChaosStatePayload::passiveName,
            PacketCodecs.VAR_INT, ChaosStatePayload::rotationSecondsRemaining,
            ChaosStatePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
