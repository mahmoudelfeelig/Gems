package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server payload to claim/release a power for Prism gem.
 */
public record PrismSelectionClaimPayload(
        Identifier powerId,
        boolean isAbility,      // true = ability, false = passive
        boolean isBonus,        // true = from bonus pool, false = from normal gems
        boolean claim           // true = claim, false = release
) implements CustomPayload {

    public static final Id<PrismSelectionClaimPayload> ID =
            new Id<>(Identifier.of(GemsMod.MOD_ID, "prism_selection_claim"));

    public static final PacketCodec<RegistryByteBuf, PrismSelectionClaimPayload> CODEC = PacketCodec.ofStatic(
            PrismSelectionClaimPayload::write,
            PrismSelectionClaimPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, PrismSelectionClaimPayload payload) {
        buf.writeIdentifier(payload.powerId);
        buf.writeBoolean(payload.isAbility);
        buf.writeBoolean(payload.isBonus);
        buf.writeBoolean(payload.claim);
    }

    private static PrismSelectionClaimPayload read(RegistryByteBuf buf) {
        Identifier powerId = buf.readIdentifier();
        boolean isAbility = buf.readBoolean();
        boolean isBonus = buf.readBoolean();
        boolean claim = buf.readBoolean();
        return new PrismSelectionClaimPayload(powerId, isAbility, isBonus, claim);
    }
}
