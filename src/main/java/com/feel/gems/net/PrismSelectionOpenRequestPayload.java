package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client -> Server request to open the Prism selection screen.
 */
public record PrismSelectionOpenRequestPayload() implements CustomPayload {
    public static final PrismSelectionOpenRequestPayload INSTANCE = new PrismSelectionOpenRequestPayload();
    
    public static final Id<PrismSelectionOpenRequestPayload> ID = 
            new Id<>(Identifier.of(GemsMod.MOD_ID, "prism_selection_open"));
    
    public static final PacketCodec<PacketByteBuf, PrismSelectionOpenRequestPayload> CODEC = 
            PacketCodec.unit(INSTANCE);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
