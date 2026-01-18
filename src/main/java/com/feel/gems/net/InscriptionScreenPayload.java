package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C: opens the inscription management screen for a legendary item.
 */
public record InscriptionScreenPayload(
        String itemKey,
        boolean mainHand,
        List<Entry> inscriptions,
        int maxSlots
) implements CustomPayload {
    public record Entry(
            String id,
            String name,
            String description,
            String rarity,
            float magnitude
    ) {}

    public static final Id<InscriptionScreenPayload> ID =
            new Id<>(Identifier.of(GemsMod.MOD_ID, "inscription_screen"));

    public static final PacketCodec<RegistryByteBuf, InscriptionScreenPayload> CODEC = PacketCodec.ofStatic(
            InscriptionScreenPayload::write,
            InscriptionScreenPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, InscriptionScreenPayload payload) {
        buf.writeString(payload.itemKey(), 64);
        buf.writeBoolean(payload.mainHand());
        buf.writeVarInt(payload.inscriptions().size());
        for (Entry entry : payload.inscriptions()) {
            buf.writeString(entry.id(), 64);
            buf.writeString(entry.name(), 128);
            buf.writeString(entry.description(), 256);
            buf.writeString(entry.rarity(), 16);
            buf.writeFloat(entry.magnitude());
        }
        buf.writeVarInt(payload.maxSlots());
    }

    private static InscriptionScreenPayload read(RegistryByteBuf buf) {
        String itemKey = buf.readString(64);
        boolean mainHand = buf.readBoolean();
        int size = buf.readVarInt();
        List<Entry> inscriptions = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String id = buf.readString(64);
            String name = buf.readString(128);
            String description = buf.readString(256);
            String rarity = buf.readString(16);
            float magnitude = buf.readFloat();
            inscriptions.add(new Entry(id, name, description, rarity, magnitude));
        }
        int maxSlots = buf.readVarInt();
        return new InscriptionScreenPayload(itemKey, mainHand, inscriptions, maxSlots);
    }
}
