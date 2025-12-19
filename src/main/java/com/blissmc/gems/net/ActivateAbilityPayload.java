package com.blissmc.gems.net;

import com.blissmc.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ActivateAbilityPayload(int abilityIndex) implements CustomPayload {
    public static final Id<ActivateAbilityPayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "activate_ability"));
    public static final PacketCodec<RegistryByteBuf, ActivateAbilityPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.VAR_INT.cast(), ActivateAbilityPayload::abilityIndex, ActivateAbilityPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
