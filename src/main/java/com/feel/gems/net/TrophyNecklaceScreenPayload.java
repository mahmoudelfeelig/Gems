package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C: opens the Trophy Necklace passive steal screen.
 * Contains the target's stealable passives and the player's current stolen passives.
 */
public record TrophyNecklaceScreenPayload(
        String targetName,
        List<OfferedEntry> offeredPassives,
        List<StolenEntry> stolenPassives,
        int maxStolenPassives
) implements CustomPayload {

    public record OfferedEntry(
            Identifier id,
            String name,
            String description,
            boolean alreadyStolen
    ) {}

    public record StolenEntry(
            Identifier id,
            String name,
            String description,
            boolean enabled
    ) {}

    public static final Id<TrophyNecklaceScreenPayload> ID =
            new Id<>(Identifier.of(GemsMod.MOD_ID, "trophy_necklace_screen"));

    public static final PacketCodec<RegistryByteBuf, TrophyNecklaceScreenPayload> CODEC = PacketCodec.ofStatic(
            TrophyNecklaceScreenPayload::write,
            TrophyNecklaceScreenPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, TrophyNecklaceScreenPayload payload) {
        buf.writeString(payload.targetName, 64);
        buf.writeVarInt(payload.offeredPassives.size());
        for (OfferedEntry entry : payload.offeredPassives) {
            buf.writeIdentifier(entry.id());
            buf.writeString(entry.name(), 128);
            buf.writeString(entry.description(), 512);
            buf.writeBoolean(entry.alreadyStolen());
        }
        buf.writeVarInt(payload.stolenPassives.size());
        for (StolenEntry entry : payload.stolenPassives) {
            buf.writeIdentifier(entry.id());
            buf.writeString(entry.name(), 128);
            buf.writeString(entry.description(), 512);
            buf.writeBoolean(entry.enabled());
        }
        buf.writeVarInt(payload.maxStolenPassives);
    }

    private static TrophyNecklaceScreenPayload read(RegistryByteBuf buf) {
        String targetName = buf.readString(64);
        int offeredSize = buf.readVarInt();
        List<OfferedEntry> offered = new ArrayList<>(offeredSize);
        for (int i = 0; i < offeredSize; i++) {
            Identifier id = buf.readIdentifier();
            String name = buf.readString(128);
            String description = buf.readString(512);
            boolean stolen = buf.readBoolean();
            offered.add(new OfferedEntry(id, name, description, stolen));
        }
        int stolenSize = buf.readVarInt();
        List<StolenEntry> stolen = new ArrayList<>(stolenSize);
        for (int i = 0; i < stolenSize; i++) {
            Identifier id = buf.readIdentifier();
            String name = buf.readString(128);
            String description = buf.readString(512);
            boolean enabled = buf.readBoolean();
            stolen.add(new StolenEntry(id, name, description, enabled));
        }
        int max = buf.readVarInt();
        return new TrophyNecklaceScreenPayload(targetName, offered, stolen, max);
    }
}

