package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S: client requests opening the bounty board screen.
 */
public record BountyBoardOpenRequestPayload() implements CustomPayload {
    public static final BountyBoardOpenRequestPayload INSTANCE = new BountyBoardOpenRequestPayload();

    public static final Id<BountyBoardOpenRequestPayload> ID =
            new Id<>(Identifier.of(GemsMod.MOD_ID, "bounty_board_open"));

    public static final PacketCodec<RegistryByteBuf, BountyBoardOpenRequestPayload> CODEC = PacketCodec.ofStatic(
            (buf, payload) -> {},
            buf -> INSTANCE
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
