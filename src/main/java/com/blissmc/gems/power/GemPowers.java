package com.blissmc.gems.power;

import com.blissmc.gems.core.GemDefinition;
import com.blissmc.gems.core.GemId;
import com.blissmc.gems.core.GemRegistry;
import com.blissmc.gems.state.GemPlayerState;
import com.blissmc.gems.state.GemsPersistentDataHolder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GemPowers {
    private static final String KEY_APPLIED_PASSIVES = "appliedPassives";

    private GemPowers() {
    }

    public static void sync(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);

        GemId activeGem = GemPlayerState.getActiveGem(player);
        int energy = GemPlayerState.getEnergy(player);

        List<Identifier> targetPassives = (energy > 0)
                ? GemRegistry.definition(activeGem).passives()
                : List.of();

        NbtCompound data = persistentRoot(player);
        Set<Identifier> applied = readIdentifierSet(data, KEY_APPLIED_PASSIVES);
        Set<Identifier> target = new HashSet<>(targetPassives);

        for (Identifier id : applied) {
            if (!target.contains(id)) {
                GemPassive passive = ModPassives.get(id);
                if (passive != null) {
                    passive.remove(player);
                }
            }
        }

        for (Identifier id : target) {
            if (!applied.contains(id)) {
                GemPassive passive = ModPassives.get(id);
                if (passive != null) {
                    passive.apply(player);
                }
            }
        }

        writeIdentifierSet(data, KEY_APPLIED_PASSIVES, target);
    }

    public static void maintain(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);
        if (GemPlayerState.getEnergy(player) <= 0) {
            return;
        }

        GemId activeGem = GemPlayerState.getActiveGem(player);
        GemDefinition def = GemRegistry.definition(activeGem);
        for (Identifier passiveId : def.passives()) {
            GemPassive passive = ModPassives.get(passiveId);
            if (passive instanceof StatusEffectPassive) {
                passive.apply(player);
            }
            if (passive instanceof GemMaintainedPassive maintained) {
                maintained.maintain(player);
            }
        }
    }

    public static boolean isPassiveActive(ServerPlayerEntity player, Identifier passiveId) {
        GemPlayerState.initIfNeeded(player);
        if (GemPlayerState.getEnergy(player) <= 0) {
            return false;
        }
        GemId activeGem = GemPlayerState.getActiveGem(player);
        return GemRegistry.definition(activeGem).passives().contains(passiveId);
    }

    private static NbtCompound persistentRoot(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }

    private static Set<Identifier> readIdentifierSet(NbtCompound root, String key) {
        if (!root.contains(key, NbtElement.LIST_TYPE)) {
            return Set.of();
        }
        NbtList list = root.getList(key, NbtElement.STRING_TYPE);
        Set<Identifier> result = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            String raw = list.getString(i);
            Identifier id = Identifier.tryParse(raw);
            if (id != null) {
                result.add(id);
            }
        }
        return result;
    }

    private static void writeIdentifierSet(NbtCompound root, String key, Set<Identifier> ids) {
        NbtList list = new NbtList();
        for (Identifier id : ids) {
            list.add(NbtString.of(id.toString()));
        }
        root.put(key, list);
    }
}
