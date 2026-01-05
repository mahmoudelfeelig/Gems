package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C: opens the Bonus Selection screen.
 * Contains lists of available/claimed bonus abilities and passives.
 */
public record BonusSelectionScreenPayload(
        List<BonusEntry> abilities,
        List<BonusEntry> passives,
        int maxAbilities,
        int maxPassives
) implements CustomPayload {
    
    public record BonusEntry(
            Identifier id,
            String name,
            String description,
            boolean available,
            boolean claimed
    ) {}

    public static final Id<BonusSelectionScreenPayload> ID = 
            new Id<>(Identifier.of(GemsMod.MOD_ID, "bonus_selection_screen"));

    public static final PacketCodec<RegistryByteBuf, BonusSelectionScreenPayload> CODEC = PacketCodec.ofStatic(
            BonusSelectionScreenPayload::write,
            BonusSelectionScreenPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, BonusSelectionScreenPayload payload) {
        // Abilities
        buf.writeVarInt(payload.abilities.size());
        for (BonusEntry entry : payload.abilities) {
            buf.writeIdentifier(entry.id());
            buf.writeString(entry.name(), 128);
            buf.writeString(entry.description(), 512);
            buf.writeBoolean(entry.available());
            buf.writeBoolean(entry.claimed());
        }
        
        // Passives
        buf.writeVarInt(payload.passives.size());
        for (BonusEntry entry : payload.passives) {
            buf.writeIdentifier(entry.id());
            buf.writeString(entry.name(), 128);
            buf.writeString(entry.description(), 512);
            buf.writeBoolean(entry.available());
            buf.writeBoolean(entry.claimed());
        }
        
        buf.writeVarInt(payload.maxAbilities);
        buf.writeVarInt(payload.maxPassives);
    }

    private static BonusSelectionScreenPayload read(RegistryByteBuf buf) {
        // Abilities
        int abilitiesSize = buf.readVarInt();
        List<BonusEntry> abilities = new ArrayList<>(abilitiesSize);
        for (int i = 0; i < abilitiesSize; i++) {
            Identifier id = buf.readIdentifier();
            String name = buf.readString(128);
            String description = buf.readString(512);
            boolean available = buf.readBoolean();
            boolean claimed = buf.readBoolean();
            abilities.add(new BonusEntry(id, name, description, available, claimed));
        }
        
        // Passives
        int passivesSize = buf.readVarInt();
        List<BonusEntry> passives = new ArrayList<>(passivesSize);
        for (int i = 0; i < passivesSize; i++) {
            Identifier id = buf.readIdentifier();
            String name = buf.readString(128);
            String description = buf.readString(512);
            boolean available = buf.readBoolean();
            boolean claimed = buf.readBoolean();
            passives.add(new BonusEntry(id, name, description, available, claimed));
        }
        
        int maxAbilities = buf.readVarInt();
        int maxPassives = buf.readVarInt();
        
        return new BonusSelectionScreenPayload(abilities, passives, maxAbilities, maxPassives);
    }
}
