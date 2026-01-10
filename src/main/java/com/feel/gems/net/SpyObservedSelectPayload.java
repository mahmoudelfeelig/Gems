package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server payload to select an observed ability for Spy echo/steal.
 */
public record SpyObservedSelectPayload(Identifier abilityId) implements CustomPayload {
    public static final Id<SpyObservedSelectPayload> ID =
            new Id<>(Identifier.of(GemsMod.MOD_ID, "spy_observed_select"));

    public static final PacketCodec<RegistryByteBuf, SpyObservedSelectPayload> CODEC =
            PacketCodec.tuple(Identifier.PACKET_CODEC, SpyObservedSelectPayload::abilityId, SpyObservedSelectPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
