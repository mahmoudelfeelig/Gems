package com.feel.gems.net;

import com.feel.gems.GemsMod;
import com.feel.gems.loadout.GemLoadout;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C: syncs the current HUD layout settings.
 */
public record HudLayoutPayload(
        GemLoadout.HudPosition position,
        boolean showCooldowns,
        boolean showEnergy,
        boolean compactMode
) implements CustomPayload {
    public static final Id<HudLayoutPayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "hud_layout"));
    public static final PacketCodec<RegistryByteBuf, HudLayoutPayload> CODEC = PacketCodec.ofStatic(
            HudLayoutPayload::write,
            HudLayoutPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, HudLayoutPayload payload) {
        buf.writeVarInt(payload.position().ordinal());
        buf.writeBoolean(payload.showCooldowns());
        buf.writeBoolean(payload.showEnergy());
        buf.writeBoolean(payload.compactMode());
    }

    private static HudLayoutPayload read(RegistryByteBuf buf) {
        int posOrdinal = buf.readVarInt();
        GemLoadout.HudPosition pos = GemLoadout.HudPosition.values()[
                Math.min(posOrdinal, GemLoadout.HudPosition.values().length - 1)
        ];
        boolean showCooldowns = buf.readBoolean();
        boolean showEnergy = buf.readBoolean();
        boolean compactMode = buf.readBoolean();
        return new HudLayoutPayload(pos, showCooldowns, showEnergy, compactMode);
    }
}
