package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C: opens the title selection screen with progress data.
 */
public record TitleSelectionScreenPayload(List<Entry> entries) implements CustomPayload {
    public record Entry(
            String id,
            int gemOrdinal,
            String displayKey,
            int usage,
            int threshold,
            boolean unlocked,
            boolean selected,
            boolean forcedSelected
    ) {
    }

    public static final Id<TitleSelectionScreenPayload> ID =
            new Id<>(Identifier.of(GemsMod.MOD_ID, "title_selection_screen"));

    public static final PacketCodec<RegistryByteBuf, TitleSelectionScreenPayload> CODEC = PacketCodec.ofStatic(
            TitleSelectionScreenPayload::write,
            TitleSelectionScreenPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, TitleSelectionScreenPayload payload) {
        List<Entry> entries = payload.entries();
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buf.writeString(entry.id(), 128);
            buf.writeVarInt(entry.gemOrdinal());
            buf.writeString(entry.displayKey(), 128);
            buf.writeVarInt(entry.usage());
            buf.writeVarInt(entry.threshold());
            buf.writeBoolean(entry.unlocked());
            buf.writeBoolean(entry.selected());
            buf.writeBoolean(entry.forcedSelected());
        }
    }

    private static TitleSelectionScreenPayload read(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String id = buf.readString(128);
            int gemOrdinal = buf.readVarInt();
            String displayKey = buf.readString(128);
            int usage = buf.readVarInt();
            int threshold = buf.readVarInt();
            boolean unlocked = buf.readBoolean();
            boolean selected = buf.readBoolean();
            boolean forcedSelected = buf.readBoolean();
            entries.add(new Entry(id, gemOrdinal, displayKey, usage, threshold, unlocked, selected, forcedSelected));
        }
        return new TitleSelectionScreenPayload(entries);
    }
}
