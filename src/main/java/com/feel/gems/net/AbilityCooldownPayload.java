package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;




/**
 * S2C: informs the client that an ability started a cooldown.
 * Client uses this for HUD feedback only; server remains authoritative.
 */
public record AbilityCooldownPayload(int activeGemOrdinal, int abilityIndex, int cooldownTicks) implements CustomPayload {
    public static final Id<AbilityCooldownPayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "ability_cooldown"));
    public static final PacketCodec<RegistryByteBuf, AbilityCooldownPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT.cast(),
            AbilityCooldownPayload::activeGemOrdinal,
            PacketCodecs.VAR_INT.cast(),
            AbilityCooldownPayload::abilityIndex,
            PacketCodecs.VAR_INT.cast(),
            AbilityCooldownPayload::cooldownTicks,
            AbilityCooldownPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

