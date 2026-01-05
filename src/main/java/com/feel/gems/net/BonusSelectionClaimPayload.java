package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S: claim or unclaim a bonus ability/passive.
 */
public record BonusSelectionClaimPayload(
        Identifier powerId,
        boolean isAbility,
        boolean claim // true = claim, false = unclaim
) implements CustomPayload {

    public static final Id<BonusSelectionClaimPayload> ID = 
            new Id<>(Identifier.of(GemsMod.MOD_ID, "bonus_selection_claim"));

    public static final PacketCodec<RegistryByteBuf, BonusSelectionClaimPayload> CODEC = PacketCodec.ofStatic(
            BonusSelectionClaimPayload::write,
            BonusSelectionClaimPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, BonusSelectionClaimPayload payload) {
        buf.writeIdentifier(payload.powerId);
        buf.writeBoolean(payload.isAbility);
        buf.writeBoolean(payload.claim);
    }

    private static BonusSelectionClaimPayload read(RegistryByteBuf buf) {
        Identifier powerId = buf.readIdentifier();
        boolean isAbility = buf.readBoolean();
        boolean claim = buf.readBoolean();
        return new BonusSelectionClaimPayload(powerId, isAbility, claim);
    }
}
