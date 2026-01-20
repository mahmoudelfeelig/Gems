package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C: syncs stolen passives (Trophy Necklace) and stolen abilities (Spy).
 */
public record StolenStatePayload(List<Identifier> stolenPassives, List<Identifier> stolenAbilities) implements CustomPayload {
    public static final Id<StolenStatePayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "stolen_state"));
    public static final PacketCodec<RegistryByteBuf, StolenStatePayload> CODEC = PacketCodec.ofStatic(
            StolenStatePayload::write,
            StolenStatePayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, StolenStatePayload payload) {
        buf.writeVarInt(payload.stolenPassives().size());
        for (Identifier id : payload.stolenPassives()) {
            buf.writeString(id.toString(), 128);
        }
        buf.writeVarInt(payload.stolenAbilities().size());
        for (Identifier id : payload.stolenAbilities()) {
            buf.writeString(id.toString(), 128);
        }
    }

    private static StolenStatePayload read(RegistryByteBuf buf) {
        int passives = buf.readVarInt();
        List<Identifier> stolenPassives = new ArrayList<>(passives);
        for (int i = 0; i < passives; i++) {
            stolenPassives.add(Identifier.of(buf.readString(128)));
        }
        int abilities = buf.readVarInt();
        List<Identifier> stolenAbilities = new ArrayList<>(abilities);
        for (int i = 0; i < abilities; i++) {
            stolenAbilities.add(Identifier.of(buf.readString(128)));
        }
        return new StolenStatePayload(stolenPassives, stolenAbilities);
    }
}
