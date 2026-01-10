package com.feel.gems.net;

import com.feel.gems.GemsMod;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * S2C: informs clients about server-side disables so UIs can hide disabled content.
 *
 * <p>Server remains authoritative; clients must never assume this is security-sensitive.</p>
 */
public record ServerDisablesPayload(
        List<Integer> disabledGemOrdinals,
        List<String> disabledAbilityIds,
        List<String> disabledPassiveIds,
        List<String> disabledBonusAbilityIds,
        List<String> disabledBonusPassiveIds
) implements CustomPayload {
    public static final Id<ServerDisablesPayload> ID = new Id<>(Identifier.of(GemsMod.MOD_ID, "server_disables"));

    public static final PacketCodec<RegistryByteBuf, ServerDisablesPayload> CODEC = PacketCodec.ofStatic(
            ServerDisablesPayload::write,
            ServerDisablesPayload::read
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    private static void write(RegistryByteBuf buf, ServerDisablesPayload payload) {
        List<Integer> gems = payload.disabledGemOrdinals == null ? List.of() : payload.disabledGemOrdinals;
        buf.writeVarInt(gems.size());
        for (int ordinal : gems) {
            buf.writeVarInt(ordinal);
        }

        writeStringList(buf, payload.disabledAbilityIds);
        writeStringList(buf, payload.disabledPassiveIds);
        writeStringList(buf, payload.disabledBonusAbilityIds);
        writeStringList(buf, payload.disabledBonusPassiveIds);
    }

    private static ServerDisablesPayload read(RegistryByteBuf buf) {
        int gemCount = buf.readVarInt();
        List<Integer> disabledGems = new ArrayList<>(gemCount);
        for (int i = 0; i < gemCount; i++) {
            disabledGems.add(buf.readVarInt());
        }
        List<String> abilities = readStringList(buf);
        List<String> passives = readStringList(buf);
        List<String> bonusAbilities = readStringList(buf);
        List<String> bonusPassives = readStringList(buf);
        return new ServerDisablesPayload(disabledGems, abilities, passives, bonusAbilities, bonusPassives);
    }

    private static void writeStringList(RegistryByteBuf buf, List<String> list) {
        List<String> safe = list == null ? List.of() : list;
        buf.writeVarInt(safe.size());
        for (String s : safe) {
            buf.writeString(s == null ? "" : s, 256);
        }
    }

    private static List<String> readStringList(RegistryByteBuf buf) {
        int size = buf.readVarInt();
        List<String> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            out.add(buf.readString(256));
        }
        return out;
    }
}

