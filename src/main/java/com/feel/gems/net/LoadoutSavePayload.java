package com.feel.gems.net;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemId;
import com.feel.gems.loadout.GemLoadout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S payload to save a new loadout preset.
 */
public record LoadoutSavePayload(
        GemId gem,
        String name,
        List<Identifier> abilityOrder,
        boolean passivesEnabled,
        GemLoadout.HudPosition hudPosition,
        boolean showCooldowns,
        boolean showEnergy,
        boolean compactMode
) implements CustomPayload {
    public static final Id<LoadoutSavePayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "loadout_save"));
    public static final PacketCodec<RegistryByteBuf, LoadoutSavePayload> CODEC = PacketCodec.ofStatic(
            LoadoutSavePayload::write,
            LoadoutSavePayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    /**
     * Convert to a GemLoadout for saving.
     */
    public GemLoadout toLoadout() {
        return new GemLoadout(
                name,
                gem,
                abilityOrder,
                passivesEnabled,
                new GemLoadout.HudLayout(hudPosition, showCooldowns, showEnergy, compactMode)
        );
    }

    private static void write(RegistryByteBuf buf, LoadoutSavePayload payload) {
        buf.writeString(payload.gem().name(), 32);
        buf.writeString(payload.name(), GemLoadout.MAX_NAME_LENGTH);
        buf.writeVarInt(payload.abilityOrder().size());
        for (Identifier id : payload.abilityOrder()) {
            buf.writeString(id.toString(), 128);
        }
        buf.writeBoolean(payload.passivesEnabled());
        buf.writeVarInt(payload.hudPosition().ordinal());
        buf.writeBoolean(payload.showCooldowns());
        buf.writeBoolean(payload.showEnergy());
        buf.writeBoolean(payload.compactMode());
    }

    private static LoadoutSavePayload read(RegistryByteBuf buf) {
        GemId gem = GemId.valueOf(buf.readString(32));
        String name = buf.readString(GemLoadout.MAX_NAME_LENGTH);
        int abilityCount = buf.readVarInt();
        List<Identifier> abilities = new ArrayList<>(abilityCount);
        for (int i = 0; i < abilityCount; i++) {
            abilities.add(Identifier.of(buf.readString(128)));
        }
        boolean passivesEnabled = buf.readBoolean();
        int posOrdinal = buf.readVarInt();
        GemLoadout.HudPosition pos = GemLoadout.HudPosition.values()[
                Math.min(posOrdinal, GemLoadout.HudPosition.values().length - 1)
        ];
        boolean showCooldowns = buf.readBoolean();
        boolean showEnergy = buf.readBoolean();
        boolean compactMode = buf.readBoolean();
        return new LoadoutSavePayload(gem, name, abilities, passivesEnabled, pos, showCooldowns, showEnergy, compactMode);
    }
}
