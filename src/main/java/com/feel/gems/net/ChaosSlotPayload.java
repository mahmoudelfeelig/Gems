package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.List;

/**
 * Syncs all 4 chaos slot states to the client for HUD display.
 */
public record ChaosSlotPayload(List<SlotData> slots) implements CustomPayload {
    public static final Id<ChaosSlotPayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "chaos_slots"));
    
    public record SlotData(
        String abilityName,
        String abilityId,
        String passiveName,
        String passiveId,
        int remainingSeconds,
        int cooldownSeconds
    ) {
        public static final PacketCodec<RegistryByteBuf, SlotData> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, SlotData::abilityName,
            PacketCodecs.STRING, SlotData::abilityId,
            PacketCodecs.STRING, SlotData::passiveName,
            PacketCodecs.STRING, SlotData::passiveId,
            PacketCodecs.VAR_INT, SlotData::remainingSeconds,
            PacketCodecs.VAR_INT, SlotData::cooldownSeconds,
            SlotData::new
        );
        
        public boolean isActive() {
            return !abilityName.isEmpty() && remainingSeconds > 0;
        }
    }
    
    public static final PacketCodec<RegistryByteBuf, ChaosSlotPayload> CODEC = PacketCodec.tuple(
        SlotData.CODEC.collect(PacketCodecs.toList()), ChaosSlotPayload::slots,
        ChaosSlotPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
