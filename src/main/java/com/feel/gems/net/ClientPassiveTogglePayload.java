package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;




public record ClientPassiveTogglePayload(boolean enabled) implements CustomPayload {
    public static final Id<ClientPassiveTogglePayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "client_passive_toggle"));
    public static final PacketCodec<RegistryByteBuf, ClientPassiveTogglePayload> CODEC = PacketCodec.ofStatic(
            ClientPassiveTogglePayload::write,
            ClientPassiveTogglePayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, ClientPassiveTogglePayload payload) {
        buf.writeBoolean(payload.enabled());
    }

    private static ClientPassiveTogglePayload read(RegistryByteBuf buf) {
        return new ClientPassiveTogglePayload(buf.readBoolean());
    }
}
