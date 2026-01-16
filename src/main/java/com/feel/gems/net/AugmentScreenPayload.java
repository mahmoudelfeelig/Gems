package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C: opens the Augment management screen for a gem.
 */
public record AugmentScreenPayload(
        String gemId,
        List<AugmentEntry> augments,
        int maxSlots
) implements CustomPayload {
    public record AugmentEntry(
            String id,
            String name,
            String description,
            String rarity,
            float magnitude
    ) {}

    public static final Id<AugmentScreenPayload> ID =
            new Id<>(Identifier.of(GemsMod.MOD_ID, "augment_screen"));

    public static final PacketCodec<RegistryByteBuf, AugmentScreenPayload> CODEC = PacketCodec.ofStatic(
            AugmentScreenPayload::write,
            AugmentScreenPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, AugmentScreenPayload payload) {
        buf.writeString(payload.gemId(), 32);
        buf.writeVarInt(payload.augments().size());
        for (AugmentEntry entry : payload.augments()) {
            buf.writeString(entry.id(), 64);
            buf.writeString(entry.name(), 128);
            buf.writeString(entry.description(), 256);
            buf.writeString(entry.rarity(), 16);
            buf.writeFloat(entry.magnitude());
        }
        buf.writeVarInt(payload.maxSlots());
    }

    private static AugmentScreenPayload read(RegistryByteBuf buf) {
        String gemId = buf.readString(32);
        int size = buf.readVarInt();
        List<AugmentEntry> augments = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String id = buf.readString(64);
            String name = buf.readString(128);
            String description = buf.readString(256);
            String rarity = buf.readString(16);
            float magnitude = buf.readFloat();
            augments.add(new AugmentEntry(id, name, description, rarity, magnitude));
        }
        int maxSlots = buf.readVarInt();
        return new AugmentScreenPayload(gemId, augments, maxSlots);
    }
}
