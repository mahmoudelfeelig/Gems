package com.feel.gems.net;

import com.feel.gems.GemsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;




/**
 * S2C: extra HUD-only state (server-authoritative but not gameplay-critical).
 */
public record ExtraStatePayload(
        int activeGemOrdinal,
        int fluxChargePercent,
        boolean hasSoul,
        String soulTypeId
) implements CustomPayload {
    public static final Id<ExtraStatePayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "extra_state"));

    public static final PacketCodec<RegistryByteBuf, ExtraStatePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.VAR_INT.cast(),
            ExtraStatePayload::activeGemOrdinal,
            PacketCodecs.VAR_INT.cast(),
            ExtraStatePayload::fluxChargePercent,
            PacketCodecs.BOOLEAN.cast(),
            ExtraStatePayload::hasSoul,
            PacketCodecs.string(128),
            ExtraStatePayload::soulTypeId,
            ExtraStatePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}

