package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;


public record SpySkinshiftPayload(UUID player, Optional<UUID> target) implements CustomPayload {
    public static final Id<SpySkinshiftPayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "spy_skinshift"));
    public static final PacketCodec<RegistryByteBuf, SpySkinshiftPayload> CODEC = PacketCodec.ofStatic(
            SpySkinshiftPayload::write,
            SpySkinshiftPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, SpySkinshiftPayload payload) {
        buf.writeUuid(payload.player());
        buf.writeBoolean(payload.target().isPresent());
        payload.target().ifPresent(buf::writeUuid);
    }

    private static SpySkinshiftPayload read(RegistryByteBuf buf) {
        UUID player = buf.readUuid();
        boolean hasTarget = buf.readBoolean();
        if (!hasTarget) {
            return new SpySkinshiftPayload(player, Optional.empty());
        }
        return new SpySkinshiftPayload(player, Optional.of(buf.readUuid()));
    }
}
