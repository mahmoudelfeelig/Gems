package com.feel.gems.net;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemId;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S payload to delete a saved loadout preset by index.
 */
public record LoadoutDeletePayload(GemId gem, int index) implements CustomPayload {
    public static final Id<LoadoutDeletePayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "loadout_delete"));
    public static final PacketCodec<RegistryByteBuf, LoadoutDeletePayload> CODEC = PacketCodec.ofStatic(
            LoadoutDeletePayload::write,
            LoadoutDeletePayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, LoadoutDeletePayload payload) {
        buf.writeString(payload.gem().name(), 32);
        buf.writeVarInt(payload.index());
    }

    private static LoadoutDeletePayload read(RegistryByteBuf buf) {
        String gemName = buf.readString(32);
        GemId gem = GemId.valueOf(gemName);
        int index = buf.readVarInt();
        return new LoadoutDeletePayload(gem, index);
    }
}
