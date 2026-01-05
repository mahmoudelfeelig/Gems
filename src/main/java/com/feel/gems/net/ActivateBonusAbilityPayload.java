package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server: Activate a claimed bonus ability by index (0 or 1).
 */
public record ActivateBonusAbilityPayload(int slotIndex) implements CustomPayload {
    public static final Id<ActivateBonusAbilityPayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "activate_bonus_ability"));
    public static final PacketCodec<RegistryByteBuf, ActivateBonusAbilityPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.VAR_INT.cast(), ActivateBonusAbilityPayload::slotIndex, ActivateBonusAbilityPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
