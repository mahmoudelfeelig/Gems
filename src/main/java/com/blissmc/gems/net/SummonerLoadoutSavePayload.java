package com.feel.gems.net;

import com.feel.gems.GemsMod;
import com.feel.gems.power.SummonerLoadouts;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * C2S: player submits a Summoner loadout edit.
 */
public record SummonerLoadoutSavePayload(
        List<SummonerLoadouts.Entry> slot1,
        List<SummonerLoadouts.Entry> slot2,
        List<SummonerLoadouts.Entry> slot3,
        List<SummonerLoadouts.Entry> slot4,
        List<SummonerLoadouts.Entry> slot5
) implements CustomPayload {
    public static final Id<SummonerLoadoutSavePayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "summoner_loadout_save"));

    public static final PacketCodec<RegistryByteBuf, SummonerLoadoutSavePayload> CODEC = PacketCodec.ofStatic(
            SummonerLoadoutSavePayload::write,
            SummonerLoadoutSavePayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

        private static void write(RegistryByteBuf buf, SummonerLoadoutSavePayload payload) {
                writeEntries(buf, payload.slot1);
                writeEntries(buf, payload.slot2);
                writeEntries(buf, payload.slot3);
                writeEntries(buf, payload.slot4);
                writeEntries(buf, payload.slot5);
        }

        private static SummonerLoadoutSavePayload read(RegistryByteBuf buf) {
                List<SummonerLoadouts.Entry> s1 = readEntries(buf);
                List<SummonerLoadouts.Entry> s2 = readEntries(buf);
                List<SummonerLoadouts.Entry> s3 = readEntries(buf);
                List<SummonerLoadouts.Entry> s4 = readEntries(buf);
                List<SummonerLoadouts.Entry> s5 = readEntries(buf);
                return new SummonerLoadoutSavePayload(s1, s2, s3, s4, s5);
        }

        private static void writeEntries(RegistryByteBuf buf, List<SummonerLoadouts.Entry> entries) {
                buf.writeVarInt(entries.size());
                for (SummonerLoadouts.Entry entry : entries) {
                        buf.writeString(entry.entityId(), 256);
                        buf.writeVarInt(entry.count());
                }
        }

        private static List<SummonerLoadouts.Entry> readEntries(RegistryByteBuf buf) {
                int size = buf.readVarInt();
                List<SummonerLoadouts.Entry> out = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                        String id = buf.readString(256);
                        int count = buf.readVarInt();
                        out.add(new SummonerLoadouts.Entry(id, count));
                }
                return out;
        }
}