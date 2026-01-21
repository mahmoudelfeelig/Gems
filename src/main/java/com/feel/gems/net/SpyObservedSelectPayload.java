package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server payload to select an observed ability for Spy echo/steal, or a stolen cast target.
 */
public record SpyObservedSelectPayload(int tab, Identifier abilityId) implements CustomPayload {
    public static final Id<SpyObservedSelectPayload> ID =
            new Id<>(Identifier.of(GemsMod.MOD_ID, "spy_observed_select"));

    public static final int TAB_ECHO = 0;
    public static final int TAB_STEAL = 1;
    public static final int TAB_STOLEN_CAST = 2;

    public static final PacketCodec<RegistryByteBuf, SpyObservedSelectPayload> CODEC = PacketCodec.tuple(
            net.minecraft.network.codec.PacketCodecs.VAR_INT.cast(),
            SpyObservedSelectPayload::tab,
            Identifier.PACKET_CODEC,
            SpyObservedSelectPayload::abilityId,
            SpyObservedSelectPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
