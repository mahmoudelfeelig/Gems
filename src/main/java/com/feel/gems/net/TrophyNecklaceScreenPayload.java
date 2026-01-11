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
 * Contains the target's stealable passives and which ones the player already stole permanently.
 */
public record TrophyNecklaceScreenPayload(
        String targetName,
        List<PassiveEntry> passives,
        int maxStolenPassives
) implements CustomPayload {

    public record PassiveEntry(
            Identifier id,
            String name,
            String description,
            boolean stolen
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
        buf.writeVarInt(payload.passives.size());
        for (PassiveEntry entry : payload.passives) {
            buf.writeIdentifier(entry.id());
            buf.writeString(entry.name(), 128);
            buf.writeString(entry.description(), 512);
            buf.writeBoolean(entry.stolen());
        }
        buf.writeVarInt(payload.maxStolenPassives);
    }

    private static TrophyNecklaceScreenPayload read(RegistryByteBuf buf) {
        String targetName = buf.readString(64);
        int size = buf.readVarInt();
        List<PassiveEntry> passives = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Identifier id = buf.readIdentifier();
            String name = buf.readString(128);
            String description = buf.readString(512);
            boolean stolen = buf.readBoolean();
            passives.add(new PassiveEntry(id, name, description, stolen));
        }
        int max = buf.readVarInt();
        return new TrophyNecklaceScreenPayload(targetName, passives, max);
    }
}

