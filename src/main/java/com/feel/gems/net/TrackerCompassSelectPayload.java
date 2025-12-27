package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S: player selects a Tracker Compass target (or clears it).
 */
public record TrackerCompassSelectPayload(Optional<UUID> target) implements CustomPayload {
    public static final Id<TrackerCompassSelectPayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "tracker_compass_select"));

    public static final PacketCodec<RegistryByteBuf, TrackerCompassSelectPayload> CODEC = PacketCodec.ofStatic(
            TrackerCompassSelectPayload::write,
            TrackerCompassSelectPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, TrackerCompassSelectPayload payload) {
        Optional<UUID> target = payload.target;
        buf.writeBoolean(target.isPresent());
        if (target.isPresent()) {
            buf.writeUuid(target.get());
        }
    }

    private static TrackerCompassSelectPayload read(RegistryByteBuf buf) {
        boolean present = buf.readBoolean();
        if (!present) {
            return new TrackerCompassSelectPayload(Optional.empty());
        }
        return new TrackerCompassSelectPayload(Optional.of(buf.readUuid()));
    }
}
