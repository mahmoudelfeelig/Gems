package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C: opens the bounty board screen with current bounties and placement data.
 */
public record BountyBoardPayload(
        UUID viewerId,
        int maxAdditionalHearts,
        int maxAdditionalEnergy,
        List<Entry> bounties,
        List<PlayerEntry> players
) implements CustomPayload {
    public record Entry(
            UUID targetId,
            String targetName,
            UUID placerId,
            String placerName,
            int hearts,
            int energy
    ) {}

    public record PlayerEntry(UUID uuid, String name) {}

    public static final Id<BountyBoardPayload> ID =
            new Id<>(Identifier.of(GemsMod.MOD_ID, "bounty_board"));

    public static final PacketCodec<RegistryByteBuf, BountyBoardPayload> CODEC = PacketCodec.ofStatic(
            BountyBoardPayload::write,
            BountyBoardPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, BountyBoardPayload payload) {
        buf.writeUuid(payload.viewerId());
        buf.writeVarInt(payload.maxAdditionalHearts());
        buf.writeVarInt(payload.maxAdditionalEnergy());

        List<Entry> entries = payload.bounties();
        buf.writeVarInt(entries.size());
        for (Entry entry : entries) {
            buf.writeUuid(entry.targetId());
            buf.writeString(entry.targetName(), 128);
            buf.writeUuid(entry.placerId());
            buf.writeString(entry.placerName(), 128);
            buf.writeVarInt(entry.hearts());
            buf.writeVarInt(entry.energy());
        }

        List<PlayerEntry> players = payload.players();
        buf.writeVarInt(players.size());
        for (PlayerEntry player : players) {
            buf.writeUuid(player.uuid());
            buf.writeString(player.name(), 128);
        }
    }

    private static BountyBoardPayload read(RegistryByteBuf buf) {
        UUID viewer = buf.readUuid();
        int maxHearts = buf.readVarInt();
        int maxEnergy = buf.readVarInt();

        int entryCount = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i++) {
            UUID targetId = buf.readUuid();
            String targetName = buf.readString(128);
            UUID placerId = buf.readUuid();
            String placerName = buf.readString(128);
            int hearts = buf.readVarInt();
            int energy = buf.readVarInt();
            entries.add(new Entry(targetId, targetName, placerId, placerName, hearts, energy));
        }

        int playerCount = buf.readVarInt();
        List<PlayerEntry> players = new ArrayList<>(playerCount);
        for (int i = 0; i < playerCount; i++) {
            UUID uuid = buf.readUuid();
            String name = buf.readString(128);
            players.add(new PlayerEntry(uuid, name));
        }

        return new BountyBoardPayload(viewer, maxHearts, maxEnergy, entries, players);
    }
}
