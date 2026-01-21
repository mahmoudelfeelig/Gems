package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S: request to place or raise a bounty on a target.
 */
public record BountyPlacePayload(UUID targetId, int hearts, int energy) implements CustomPayload {
    public static final Id<BountyPlacePayload> ID =
            new Id<>(Identifier.of(GemsMod.MOD_ID, "bounty_place"));

    public static final PacketCodec<RegistryByteBuf, BountyPlacePayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {
                buf.writeUuid(payload.targetId());
                buf.writeVarInt(payload.hearts());
                buf.writeVarInt(payload.energy());
            },
            buf -> new BountyPlacePayload(buf.readUuid(), buf.readVarInt(), buf.readVarInt())
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
