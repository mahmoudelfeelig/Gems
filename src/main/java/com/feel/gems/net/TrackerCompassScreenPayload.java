package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C: opens the Tracker Compass target picker.
 */
public record TrackerCompassScreenPayload(List<Entry> entries) implements CustomPayload {
    public record Entry(UUID uuid, String name, boolean online) {
    }

    public static final Id<TrackerCompassScreenPayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "tracker_compass_screen"));

    public static final PacketCodec<RegistryByteBuf, TrackerCompassScreenPayload> CODEC = PacketCodec.ofStatic(
            TrackerCompassScreenPayload::write,
            TrackerCompassScreenPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, TrackerCompassScreenPayload payload) {
        List<Entry> entries = payload.entries;
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buf.writeUuid(entry.uuid());
            buf.writeString(entry.name(), 128);
            buf.writeBoolean(entry.online());
        }
    }

    private static TrackerCompassScreenPayload read(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUuid();
            String name = buf.readString(128);
            boolean online = buf.readBoolean();
            entries.add(new Entry(uuid, name, online));
        }
        return new TrackerCompassScreenPayload(entries);
    }
}
