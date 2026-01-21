package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S: update a Trophy Necklace passive (steal/enable/disable).
 */
public record TrophyNecklaceClaimPayload(
        Identifier passiveId,
        Action action
) implements CustomPayload {

    public enum Action {
        STEAL,
        ENABLE,
        DISABLE
    }

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
        buf.writeVarInt(payload.action.ordinal());
    }

    private static TrophyNecklaceClaimPayload read(RegistryByteBuf buf) {
        Identifier id = buf.readIdentifier();
        int raw = buf.readVarInt();
        Action action = Action.STEAL;
        Action[] values = Action.values();
        if (raw >= 0 && raw < values.length) {
            action = values[raw];
        }
        return new TrophyNecklaceClaimPayload(id, action);
    }
}

