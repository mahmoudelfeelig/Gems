package com.feel.gems.power.runtime;

import com.feel.gems.bonus.BonusClaimsState;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;




public final class GemPowers {
    private static final String KEY_APPLIED_PASSIVES = "appliedPassives";
    private static final String KEY_APPLIED_BONUS_PASSIVES = "appliedBonusPassives";

    private GemPowers() {
    }

    public static void sync(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);

        GemId activeGem = GemPlayerState.getActiveGem(player);
        int energy = GemPlayerState.getEnergy(player);
        boolean passivesEnabled = GemPlayerState.arePassivesEnabled(player);
        boolean suppressed = AbilityRestrictions.isSuppressed(player);

        // Sync gem passives
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

        // Sync bonus passives (only at energy 10)
        syncBonusPassives(player, energy, passivesEnabled, suppressed);
    }

    /**
     * Sync bonus passives for a player. Called from sync() and on login.
     * Releases all bonus claims if energy drops below 10.
     */
    private static void syncBonusPassives(ServerPlayerEntity player, int energy, boolean passivesEnabled, boolean suppressed) {
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) return;

        BonusClaimsState claims = BonusClaimsState.get(server);
        NbtCompound data = persistentRoot(player);
        Set<Identifier> appliedBonus = readIdentifierSet(data, KEY_APPLIED_BONUS_PASSIVES);

        // If energy < 10, release all bonus claims and remove passives
        if (energy < 10) {
            Set<Identifier> playerPassives = claims.getPlayerPassives(player.getUuid());
            Set<Identifier> playerAbilities = claims.getPlayerAbilities(player.getUuid());
            
            if (!playerPassives.isEmpty() || !playerAbilities.isEmpty()) {
                // Remove applied bonus passives
                for (Identifier id : appliedBonus) {
                    GemPassive passive = ModPassives.get(id);
                    if (passive != null) {
                        passive.remove(player);
                    }
                }
                writeIdentifierSet(data, KEY_APPLIED_BONUS_PASSIVES, Set.of());
                
                // Release all claims
                claims.releaseAllClaims(player.getUuid());
                player.sendMessage(net.minecraft.text.Text.literal("Your bonus powers have been released (energy below 10)."), false);
            }
            return;
        }

        // Energy is 10 - sync bonus passives
        Set<Identifier> targetBonusPassives = Set.of();
        if (passivesEnabled && !suppressed) {
            Set<Identifier> claimed = claims.getPlayerPassives(player.getUuid());
            if (!claimed.isEmpty()) {
                java.util.HashSet<Identifier> filtered = new java.util.HashSet<>();
                for (Identifier id : claimed) {
                    if (!GemsDisables.isBonusPassiveDisabledFor(player, id)) {
                        filtered.add(id);
                    }
                }
                targetBonusPassives = Set.copyOf(filtered);
            }
        }

        // Remove passives no longer in target
        for (Identifier id : appliedBonus) {
            if (!targetBonusPassives.contains(id)) {
                GemPassive passive = ModPassives.get(id);
                if (passive != null) {
                    passive.remove(player);
                }
            }
        }

        // Apply new passives
        for (Identifier id : targetBonusPassives) {
            if (!appliedBonus.contains(id)) {
                GemPassive passive = ModPassives.get(id);
                if (passive != null) {
                    passive.apply(player);
                }
            }
        }

        writeIdentifierSet(data, KEY_APPLIED_BONUS_PASSIVES, targetBonusPassives);
    }

    public static void maintain(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);
        int energy = GemPlayerState.getEnergy(player);
        if (energy <= 0) {
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

        // Maintain bonus passives (at energy 10)
        if (energy >= 10) {
            maintainBonusPassives(player);
        }
    }

    /**
     * Maintain bonus passives - reapply status effects and call maintain on maintained passives.
     */
    private static void maintainBonusPassives(ServerPlayerEntity player) {
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) return;

        BonusClaimsState claims = BonusClaimsState.get(server);
        Set<Identifier> playerPassives = claims.getPlayerPassives(player.getUuid());

        for (Identifier passiveId : playerPassives) {
            if (GemsDisables.isBonusPassiveDisabledFor(player, passiveId)) {
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
        int energy = GemPlayerState.getEnergy(player);
        if (energy <= 0) {
            return false;
        }
        if (!GemPlayerState.arePassivesEnabled(player)) {
            return false;
        }
        if (AbilityRestrictions.isSuppressed(player)) {
            return false;
        }
        GemId activeGem = GemPlayerState.getActiveGem(player);
        if (GemRegistry.definition(activeGem).passives().contains(passiveId) && !GemsDisables.isPassiveDisabledFor(player, passiveId)) {
            return true;
        }
        // Check bonus passives (at energy 10)
        if (energy >= 10) {
            return isBonusPassiveActive(player, passiveId);
        }
        return false;
    }

    /**
     * Check if a bonus passive is active for a player.
     */
    public static boolean isBonusPassiveActive(ServerPlayerEntity player, Identifier passiveId) {
        if (GemPlayerState.getEnergy(player) < 10) {
            return false;
        }
        if (!GemPlayerState.arePassivesEnabled(player)) {
            return false;
        }
        if (AbilityRestrictions.isSuppressed(player)) {
            return false;
        }
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) return false;

        BonusClaimsState claims = BonusClaimsState.get(server);
        Set<Identifier> playerPassives = claims.getPlayerPassives(player.getUuid());
        return playerPassives.contains(passiveId) && !GemsDisables.isBonusPassiveDisabledFor(player, passiveId);
    }

    /**
     * Get the list of active passive identifiers for a player.
     * Returns empty if energy is 0, passives are disabled, or the player is suppressed.
     */
    public static List<Identifier> getActivePassives(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);
        if (GemPlayerState.getEnergy(player) <= 0) {
            return List.of();
        }
        if (!GemPlayerState.arePassivesEnabled(player)) {
            return List.of();
        }
        if (AbilityRestrictions.isSuppressed(player)) {
            return List.of();
        }
        GemId activeGem = GemPlayerState.getActiveGem(player);
        List<Identifier> allPassives = GemRegistry.definition(activeGem).passives();
        if (allPassives.isEmpty()) {
            return List.of();
        }
        java.util.ArrayList<Identifier> result = new java.util.ArrayList<>(allPassives.size());
        for (Identifier id : allPassives) {
            if (!GemsDisables.isPassiveDisabledFor(player, id)) {
                result.add(id);
            }
        }
        return List.copyOf(result);
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
