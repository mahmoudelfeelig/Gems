package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C: syncs the ordered ability list for the active gem.
 */
public record AbilityOrderSyncPayload(int activeGemOrdinal, List<Identifier> abilityOrder) implements CustomPayload {
    public static final Id<AbilityOrderSyncPayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "ability_order_sync"));
    public static final PacketCodec<RegistryByteBuf, AbilityOrderSyncPayload> CODEC = PacketCodec.ofStatic(
            AbilityOrderSyncPayload::write,
            AbilityOrderSyncPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, AbilityOrderSyncPayload payload) {
        buf.writeVarInt(payload.activeGemOrdinal());
        buf.writeVarInt(payload.abilityOrder().size());
        for (Identifier id : payload.abilityOrder()) {
            buf.writeString(id.toString(), 128);
        }
    }

    private static AbilityOrderSyncPayload read(RegistryByteBuf buf) {
        int gemOrdinal = buf.readVarInt();
        int count = buf.readVarInt();
        List<Identifier> order = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            order.add(Identifier.of(buf.readString(128)));
        }
        return new AbilityOrderSyncPayload(gemOrdinal, order);
    }
}
