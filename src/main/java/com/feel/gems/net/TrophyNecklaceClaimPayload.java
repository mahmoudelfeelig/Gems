package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S: steal (or unsteal) a passive via the Trophy Necklace UI.
 */
public record TrophyNecklaceClaimPayload(
        Identifier passiveId,
        boolean steal
) implements CustomPayload {

    public static final Id<TrophyNecklaceClaimPayload> ID =
            new Id<>(Identifier.of(GemsMod.MOD_ID, "trophy_necklace_claim"));

    public static final PacketCodec<RegistryByteBuf, TrophyNecklaceClaimPayload> CODEC = PacketCodec.ofStatic(
            TrophyNecklaceClaimPayload::write,
            TrophyNecklaceClaimPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, TrophyNecklaceClaimPayload payload) {
        buf.writeIdentifier(payload.passiveId);
        buf.writeBoolean(payload.steal);
    }

    private static TrophyNecklaceClaimPayload read(RegistryByteBuf buf) {
        Identifier id = buf.readIdentifier();
        boolean steal = buf.readBoolean();
        return new TrophyNecklaceClaimPayload(id, steal);
    }
}

