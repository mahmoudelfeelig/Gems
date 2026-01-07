package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C: syncs the Prism player's selected abilities to the client for HUD display.
 */
public record PrismAbilitiesSyncPayload(
        List<PrismAbilityInfo> abilities
) implements CustomPayload {
    
    public record PrismAbilityInfo(
            Identifier id,
            String name,
            int remainingCooldownTicks
    ) {}

    public static final Id<PrismAbilitiesSyncPayload> ID = 
            new Id<>(Identifier.of(GemsMod.MOD_ID, "prism_abilities_sync"));

    public static final PacketCodec<RegistryByteBuf, PrismAbilitiesSyncPayload> CODEC = PacketCodec.ofStatic(
            PrismAbilitiesSyncPayload::write,
            PrismAbilitiesSyncPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, PrismAbilitiesSyncPayload payload) {
        buf.writeVarInt(payload.abilities.size());
        for (PrismAbilityInfo info : payload.abilities) {
            buf.writeIdentifier(info.id());
            buf.writeString(info.name(), 128);
            buf.writeVarInt(info.remainingCooldownTicks());
        }
    }

    private static PrismAbilitiesSyncPayload read(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<PrismAbilityInfo> abilities = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Identifier id = buf.readIdentifier();
            String name = buf.readString(128);
            int cooldown = buf.readVarInt();
            abilities.add(new PrismAbilityInfo(id, name, cooldown));
        }
        return new PrismAbilitiesSyncPayload(abilities);
    }
}
