package com.feel.gems.net;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemId;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S: request to open the loadout presets manager for a gem.
 */
public record LoadoutOpenRequestPayload(GemId gem) implements CustomPayload {
    public static final Id<LoadoutOpenRequestPayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "loadout_open_request"));
    public static final PacketCodec<RegistryByteBuf, LoadoutOpenRequestPayload> CODEC = PacketCodec.ofStatic(
            LoadoutOpenRequestPayload::write,
            LoadoutOpenRequestPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, LoadoutOpenRequestPayload payload) {
        buf.writeString(payload.gem().name(), 32);
    }

    private static LoadoutOpenRequestPayload read(RegistryByteBuf buf) {
        String gemName = buf.readString(32);
        return new LoadoutOpenRequestPayload(GemId.valueOf(gemName));
    }
}
