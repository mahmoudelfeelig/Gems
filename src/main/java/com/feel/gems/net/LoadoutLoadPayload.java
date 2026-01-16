package com.feel.gems.net;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemId;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S payload to load (apply) a saved loadout preset by index.
 */
public record LoadoutLoadPayload(GemId gem, int index) implements CustomPayload {
    public static final Id<LoadoutLoadPayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "loadout_load"));
    public static final PacketCodec<RegistryByteBuf, LoadoutLoadPayload> CODEC = PacketCodec.ofStatic(
            LoadoutLoadPayload::write,
            LoadoutLoadPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, LoadoutLoadPayload payload) {
        buf.writeString(payload.gem().name(), 32);
        buf.writeVarInt(payload.index());
    }

    private static LoadoutLoadPayload read(RegistryByteBuf buf) {
        String gemName = buf.readString(32);
        GemId gem = GemId.valueOf(gemName);
        int index = buf.readVarInt();
        return new LoadoutLoadPayload(gem, index);
    }
}
