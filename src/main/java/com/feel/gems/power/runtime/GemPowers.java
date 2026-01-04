package com.feel.gems.power.runtime;

import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.config.GemsDisables;
import com.feel.gems.power.api.GemMaintainedPassive;
import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.passive.StatusEffectPassive;
import com.feel.gems.power.registry.ModPassives;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;




public final class GemPowers {
    private static final String KEY_APPLIED_PASSIVES = "appliedPassives";

    private GemPowers() {
    }

    public static void sync(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);

        GemId activeGem = GemPlayerState.getActiveGem(player);
        int energy = GemPlayerState.getEnergy(player);
        boolean passivesEnabled = GemPlayerState.arePassivesEnabled(player);
        boolean suppressed = AbilityRestrictions.isSuppressed(player);

        List<Identifier> targetPassives = List.of();
        if (energy > 0 && passivesEnabled && !suppressed) {
            List<Identifier> raw = GemRegistry.definition(activeGem).passives();
            if (!raw.isEmpty()) {
                java.util.ArrayList<Identifier> filtered = new java.util.ArrayList<>(raw.size());
                for (Identifier id : raw) {
                    if (!GemsDisables.isPassiveDisabledFor(player, id)) {
                        filtered.add(id);
                    }
                }
                targetPassives = List.copyOf(filtered);
            }
        }

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
        if (!GemPlayerState.arePassivesEnabled(player)) {
            return;
        }
        if (AbilityRestrictions.isSuppressed(player)) {
            return;
        }

        GemId activeGem = GemPlayerState.getActiveGem(player);
        GemDefinition def = GemRegistry.definition(activeGem);
        NbtCompound data = persistentRoot(player);
        if (readIdentifierSet(data, KEY_APPLIED_PASSIVES).isEmpty() && !def.passives().isEmpty()) {
            sync(player);
        }
        for (Identifier passiveId : def.passives()) {
            if (GemsDisables.isPassiveDisabledFor(player, passiveId)) {
                continue;
            }
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
        if (!GemPlayerState.arePassivesEnabled(player)) {
            return false;
        }
        if (AbilityRestrictions.isSuppressed(player)) {
            return false;
        }
        GemId activeGem = GemPlayerState.getActiveGem(player);
        return GemRegistry.definition(activeGem).passives().contains(passiveId) && !GemsDisables.isPassiveDisabledFor(player, passiveId);
    }

    private static NbtCompound persistentRoot(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }

    private static Set<Identifier> readIdentifierSet(NbtCompound root, String key) {
        NbtList list = root.getList(key).orElse(null);
        if (list == null || list.isEmpty()) {
            return Set.of();
        }
        Set<Identifier> result = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            String raw = list.getString(i, "");
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
