package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server -> Client payload containing observed Spy abilities and counts.
 */
public record SpyObservedScreenPayload(List<ObservedEntry> observed, Identifier selectedId) implements CustomPayload {
    public record ObservedEntry(Identifier id, String name, int count, boolean canEcho, boolean canSteal) {}

    public static final Id<SpyObservedScreenPayload> ID =
            new Id<>(Identifier.of(GemsMod.MOD_ID, "spy_observed_screen"));

    public static final PacketCodec<RegistryByteBuf, SpyObservedScreenPayload> CODEC = PacketCodec.ofStatic(
            SpyObservedScreenPayload::write,
            SpyObservedScreenPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, SpyObservedScreenPayload payload) {
        buf.writeVarInt(payload.observed.size());
        for (ObservedEntry entry : payload.observed) {
            buf.writeIdentifier(entry.id());
            buf.writeString(entry.name());
            buf.writeVarInt(entry.count());
            buf.writeBoolean(entry.canEcho());
            buf.writeBoolean(entry.canSteal());
        }
        if (payload.selectedId != null) {
            buf.writeBoolean(true);
            buf.writeIdentifier(payload.selectedId);
        } else {
            buf.writeBoolean(false);
        }
    }

    private static SpyObservedScreenPayload read(RegistryByteBuf buf) {
        int count = buf.readVarInt();
        List<ObservedEntry> observed = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Identifier id = buf.readIdentifier();
            String name = buf.readString();
            int witnessed = buf.readVarInt();
            boolean canEcho = buf.readBoolean();
            boolean canSteal = buf.readBoolean();
            observed.add(new ObservedEntry(id, name, witnessed, canEcho, canSteal));
        }
        Identifier selectedId = null;
        if (buf.readBoolean()) {
            selectedId = buf.readIdentifier();
        }
        return new SpyObservedScreenPayload(observed, selectedId);
    }
}
