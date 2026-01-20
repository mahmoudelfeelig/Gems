package com.feel.gems.power.runtime;

import com.feel.gems.bonus.BonusClaimsState;
import com.feel.gems.bonus.PrismSelectionsState;
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
import java.util.ArrayList;
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
    private static final String KEY_APPLIED_PRISM_PASSIVES = "appliedPrismPassives";
    private static final String KEY_APPLIED_TROPHY_PASSIVES = "appliedTrophyPassives";

    private GemPowers() {
    }

    public static void sync(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);

        GemId activeGem = GemPlayerState.getActiveGem(player);
        int energy = GemPlayerState.getEnergy(player);
        boolean passivesEnabled = GemPlayerState.arePassivesEnabled(player);
        boolean suppressed = AbilityRestrictions.isSuppressed(player);

        // Prism gem has special handling - uses selected passives instead of definition
        if (activeGem == GemId.PRISM) {
            syncPrismPassives(player, energy, passivesEnabled, suppressed);
            // Prism can still claim and use the global bonus pool at energy 10.
            syncBonusPassives(player, energy, passivesEnabled, suppressed);
            syncTrophyPassives(player, energy, passivesEnabled, suppressed);
            return;
        }

        // Sync gem passives
        List<Identifier> targetPassives = List.of();
        if (energy > 0 && passivesEnabled && !suppressed) {
            List<Identifier> raw = GemRegistry.definition(activeGem).passives();
            if (!raw.isEmpty()) {
                ArrayList<Identifier> filtered = new ArrayList<>(raw.size());
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

        // Sync Trophy Necklace passives (permanent stolen passives)
        syncTrophyPassives(player, energy, passivesEnabled, suppressed);
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
                player.sendMessage(net.minecraft.text.Text.translatable("gems.bonus.powers_released"), false);
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

    /**
     * Sync Prism gem passives from player's selections.
     * Prism uses custom selections instead of gem definition passives.
     */
    private static void syncPrismPassives(ServerPlayerEntity player, int energy, boolean passivesEnabled, boolean suppressed) {
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) return;

        NbtCompound data = persistentRoot(player);
        Set<Identifier> appliedPrism = readIdentifierSet(data, KEY_APPLIED_PRISM_PASSIVES);

        // If energy <= 0 or passives disabled or suppressed, remove all Prism passives
        if (energy <= 0 || !passivesEnabled || suppressed) {
            for (Identifier id : appliedPrism) {
                GemPassive passive = ModPassives.get(id);
                if (passive != null) {
                    passive.remove(player);
                }
            }
            writeIdentifierSet(data, KEY_APPLIED_PRISM_PASSIVES, Set.of());
            return;
        }

        // Get player's Prism selections
        PrismSelectionsState prismState = PrismSelectionsState.get(server);
        PrismSelectionsState.PrismSelection selection = prismState.getSelection(player.getUuid());
        List<Identifier> selectedPassives = selection.gemPassives();

        // Filter out disabled passives
        Set<Identifier> targetPrismPassives = new HashSet<>();
        for (Identifier id : selectedPassives) {
            if (!GemsDisables.isPassiveDisabledFor(player, id) && 
                !GemsDisables.isBonusPassiveDisabledFor(player, id)) {
                targetPrismPassives.add(id);
            }
        }

        // Remove passives no longer selected
        for (Identifier id : appliedPrism) {
            if (!targetPrismPassives.contains(id)) {
                GemPassive passive = ModPassives.get(id);
                if (passive != null) {
                    passive.remove(player);
                }
            }
        }

        // Apply new passives
        for (Identifier id : targetPrismPassives) {
            if (!appliedPrism.contains(id)) {
                GemPassive passive = ModPassives.get(id);
                if (passive != null) {
                    passive.apply(player);
                }
            }
        }

        writeIdentifierSet(data, KEY_APPLIED_PRISM_PASSIVES, targetPrismPassives);
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
        
        // Prism gem uses selected passives instead of definition
        if (activeGem == GemId.PRISM) {
            maintainPrismPassives(player);
            if (energy >= 10) {
                maintainBonusPassives(player);
            }
            maintainTrophyPassives(player);
            return;
        }
        
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

        maintainTrophyPassives(player);
    }

    /**
     * Maintain Prism passives - reapply status effects and call maintain on maintained passives.
     */
    private static void maintainPrismPassives(ServerPlayerEntity player) {
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) return;

        PrismSelectionsState prismState = PrismSelectionsState.get(server);
        PrismSelectionsState.PrismSelection selection = prismState.getSelection(player.getUuid());
        List<Identifier> selectedPassives = selection.gemPassives();

        for (Identifier passiveId : selectedPassives) {
            if (GemsDisables.isPassiveDisabledFor(player, passiveId) || 
                GemsDisables.isBonusPassiveDisabledFor(player, passiveId)) {
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
        
        // Prism gem uses selected passives
        if (activeGem == GemId.PRISM) {
            if (isPrismPassiveActive(player, passiveId)) {
                return true;
            }
            if (energy >= 10 && isBonusPassiveActive(player, passiveId)) {
                return true;
            }
            return isTrophyPassiveActive(player, passiveId);
        }
        
        if (GemRegistry.definition(activeGem).passives().contains(passiveId) && !GemsDisables.isPassiveDisabledFor(player, passiveId)) {
            return true;
        }
        // Check bonus passives (at energy 10)
        if (energy >= 10 && isBonusPassiveActive(player, passiveId)) {
            return true;
        }
        return isTrophyPassiveActive(player, passiveId);
    }

    /**
     * Check if a Prism passive is active for a player.
     */
    public static boolean isPrismPassiveActive(ServerPlayerEntity player, Identifier passiveId) {
        if (GemPlayerState.getEnergy(player) <= 0) {
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

        PrismSelectionsState prismState = PrismSelectionsState.get(server);
        PrismSelectionsState.PrismSelection selection = prismState.getSelection(player.getUuid());
        List<Identifier> selectedPassives = selection.gemPassives();
        return selectedPassives.contains(passiveId) && 
               !GemsDisables.isPassiveDisabledFor(player, passiveId) &&
               !GemsDisables.isBonusPassiveDisabledFor(player, passiveId);
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
        if (playerPassives.contains(passiveId) && !GemsDisables.isBonusPassiveDisabledFor(player, passiveId)) {
            return true;
        }

        // Defensive fallback: treat already-applied bonus passives as active. This keeps gameplay consistent
        // if the persistent claims state is temporarily out-of-sync (GameTests create many ephemeral players).
        NbtCompound data = persistentRoot(player);
        Set<Identifier> appliedBonus = readIdentifierSet(data, KEY_APPLIED_BONUS_PASSIVES);
        return appliedBonus.contains(passiveId) && !GemsDisables.isBonusPassiveDisabledFor(player, passiveId);
    }

    /**
     * Check if a Trophy Necklace stolen passive is active for a player.
     */
    public static boolean isTrophyPassiveActive(ServerPlayerEntity player, Identifier passiveId) {
        if (GemPlayerState.getEnergy(player) <= 0) {
            return false;
        }
        if (!GemPlayerState.arePassivesEnabled(player)) {
            return false;
        }
        if (AbilityRestrictions.isSuppressed(player)) {
            return false;
        }
        var stolen = com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.getStolenPassives(player);
        if (!stolen.contains(passiveId)) {
            return false;
        }
        return !GemsDisables.isPassiveDisabledFor(player, passiveId) && !GemsDisables.isBonusPassiveDisabledFor(player, passiveId);
    }

    /**
     * Get the list of active passive identifiers for a player.
     * Returns empty if energy is 0, passives are disabled, or the player is suppressed.
     */
    public static List<Identifier> getActivePassives(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);
        int energy = GemPlayerState.getEnergy(player);
        if (energy <= 0) {
            return List.of();
        }
        if (!GemPlayerState.arePassivesEnabled(player)) {
            return List.of();
        }
        if (AbilityRestrictions.isSuppressed(player)) {
            return List.of();
        }
        GemId activeGem = GemPlayerState.getActiveGem(player);
        
        // Prism gem uses selected passives
        if (activeGem == GemId.PRISM) {
            MinecraftServer server = player.getEntityWorld().getServer();
            if (server == null) return List.of();
            PrismSelectionsState prismState = PrismSelectionsState.get(server);
            PrismSelectionsState.PrismSelection selection = prismState.getSelection(player.getUuid());
            List<Identifier> selectedPassives = selection.gemPassives();
            ArrayList<Identifier> result = new ArrayList<>(selectedPassives.size());
            for (Identifier id : selectedPassives) {
                if (!GemsDisables.isPassiveDisabledFor(player, id) && 
                    !GemsDisables.isBonusPassiveDisabledFor(player, id)) {
                    result.add(id);
                }
            }
            if (energy >= 10) {
                BonusClaimsState claims = BonusClaimsState.get(server);
                for (Identifier id : claims.getPlayerPassives(player.getUuid())) {
                    if (!GemsDisables.isBonusPassiveDisabledFor(player, id)) {
                        result.add(id);
                    }
                }
            }
            for (Identifier id : com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.getStolenPassives(player)) {
                if (!GemsDisables.isPassiveDisabledFor(player, id) && !GemsDisables.isBonusPassiveDisabledFor(player, id)) {
                    result.add(id);
                }
            }
            return result.isEmpty() ? List.of() : List.copyOf(result);
        }
        
        List<Identifier> allPassives = GemRegistry.definition(activeGem).passives();
        if (allPassives.isEmpty()) {
            return List.of();
        }
        ArrayList<Identifier> result = new ArrayList<>(allPassives.size());
        for (Identifier id : allPassives) {
            if (!GemsDisables.isPassiveDisabledFor(player, id)) {
                result.add(id);
            }
        }
        if (energy >= 10) {
            MinecraftServer server = player.getEntityWorld().getServer();
            if (server != null) {
                for (Identifier id : BonusClaimsState.get(server).getPlayerPassives(player.getUuid())) {
                    if (!GemsDisables.isBonusPassiveDisabledFor(player, id)) {
                        result.add(id);
                    }
                }
            }
        }
        for (Identifier id : com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.getStolenPassives(player)) {
            if (!GemsDisables.isPassiveDisabledFor(player, id) && !GemsDisables.isBonusPassiveDisabledFor(player, id)) {
                result.add(id);
            }
        }
        return result.isEmpty() ? List.of() : List.copyOf(result);
    }

    private static void syncTrophyPassives(ServerPlayerEntity player, int energy, boolean passivesEnabled, boolean suppressed) {
        NbtCompound data = persistentRoot(player);
        Set<Identifier> applied = readIdentifierSet(data, KEY_APPLIED_TROPHY_PASSIVES);

        if (energy <= 0 || !passivesEnabled || suppressed) {
            for (Identifier id : applied) {
                GemPassive passive = ModPassives.get(id);
                if (passive != null) {
                    passive.remove(player);
                }
            }
            writeIdentifierSet(data, KEY_APPLIED_TROPHY_PASSIVES, Set.of());
            return;
        }

        Set<Identifier> stolen = com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.getStolenPassives(player);
        Set<Identifier> target = Set.of();
        if (!stolen.isEmpty()) {
            java.util.HashSet<Identifier> filtered = new java.util.HashSet<>();
            for (Identifier id : stolen) {
                if (!GemsDisables.isPassiveDisabledFor(player, id) && !GemsDisables.isBonusPassiveDisabledFor(player, id)) {
                    filtered.add(id);
                }
            }
            target = Set.copyOf(filtered);
        }

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

        writeIdentifierSet(data, KEY_APPLIED_TROPHY_PASSIVES, target);
    }

    private static void maintainTrophyPassives(ServerPlayerEntity player) {
        Set<Identifier> stolen = com.feel.gems.item.legendary.HuntersTrophyNecklaceItem.getStolenPassives(player);
        if (stolen.isEmpty()) {
            return;
        }
        for (Identifier passiveId : stolen) {
            if (GemsDisables.isPassiveDisabledFor(player, passiveId) || GemsDisables.isBonusPassiveDisabledFor(player, passiveId)) {
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
