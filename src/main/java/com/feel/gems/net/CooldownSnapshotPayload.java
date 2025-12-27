package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;




/**
 * S2C: sends a snapshot of remaining cooldown ticks for the player's active gem abilities.
 */
public record CooldownSnapshotPayload(int activeGemOrdinal, java.util.List<Integer> remainingAbilityCooldownTicks) implements CustomPayload {
    public static final Id<CooldownSnapshotPayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "cooldown_snapshot"));

    public static final PacketCodec<RegistryByteBuf, CooldownSnapshotPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT.cast(),
            CooldownSnapshotPayload::activeGemOrdinal,
            PacketCodecs.collection((int size) -> (java.util.List<Integer>) new java.util.ArrayList<Integer>(size), PacketCodecs.VAR_INT).cast(),
            CooldownSnapshotPayload::remainingAbilityCooldownTicks,
            CooldownSnapshotPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
