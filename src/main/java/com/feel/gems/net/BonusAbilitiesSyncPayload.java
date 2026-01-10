package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C: syncs the player's claimed bonus abilities to the client for HUD display.
 */
public record BonusAbilitiesSyncPayload(
        List<BonusAbilityInfo> abilities
) implements CustomPayload {
    
    public record BonusAbilityInfo(
            Identifier id,
            String name,
            int remainingCooldownTicks
    ) {}

    public static final Id<BonusAbilitiesSyncPayload> ID = 
            new Id<>(Identifier.of(GemsMod.MOD_ID, "bonus_abilities_sync"));

    public static final PacketCodec<RegistryByteBuf, BonusAbilitiesSyncPayload> CODEC = PacketCodec.ofStatic(
            BonusAbilitiesSyncPayload::write,
            BonusAbilitiesSyncPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, BonusAbilitiesSyncPayload payload) {
        buf.writeVarInt(payload.abilities.size());
        for (BonusAbilityInfo info : payload.abilities) {
            buf.writeIdentifier(info.id());
            buf.writeString(info.name(), 128);
            buf.writeVarInt(info.remainingCooldownTicks());
        }
    }

    private static BonusAbilitiesSyncPayload read(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<BonusAbilityInfo> abilities = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            Identifier id = buf.readIdentifier();
            String name = buf.readString(128);
            int cooldown = buf.readVarInt();
            abilities.add(new BonusAbilityInfo(id, name, cooldown));
        }
        return new BonusAbilitiesSyncPayload(abilities);
    }
}
