package com.feel.gems.bonus;

import com.feel.gems.GemsMod;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.registry.ModPassives;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;
import java.util.*;

/**
 * Tracks Prism gem ability/passive selections per player.
 * Prism gem at energy 10/10 can pick:
 * - Up to 3 abilities from normal gems
 * - Up to 2 abilities from the bonus pool
 * - Up to 3 passives from normal gems
 * - Up to 2 passives from the bonus pool
 * Total: 5 abilities, 5 passives max
 */
public final class PrismSelectionsState extends PersistentState {
    private static final String STATE_ID = GemsMod.MOD_ID + "_prism_selections";
    
    // Maps player UUID -> their Prism selections
    private final Map<UUID, PrismSelection> selections;

    public record PrismSelection(
            List<Identifier> gemAbilities,      // Max 3 from normal gems
            List<Identifier> bonusAbilities,    // Max 2 from bonus pool
            List<Identifier> gemPassives,       // Max 3 from normal gems
            List<Identifier> bonusPassives      // Max 2 from bonus pool
    ) {
        public static final Codec<PrismSelection> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Identifier.CODEC.listOf().optionalFieldOf("gemAbilities", List.of()).forGetter(PrismSelection::gemAbilities),
                Identifier.CODEC.listOf().optionalFieldOf("bonusAbilities", List.of()).forGetter(PrismSelection::bonusAbilities),
                Identifier.CODEC.listOf().optionalFieldOf("gemPassives", List.of()).forGetter(PrismSelection::gemPassives),
                Identifier.CODEC.listOf().optionalFieldOf("bonusPassives", List.of()).forGetter(PrismSelection::bonusPassives)
        ).apply(instance, PrismSelection::new));

        public static PrismSelection empty() {
            return new PrismSelection(List.of(), List.of(), List.of(), List.of());
        }

        public List<Identifier> allAbilities() {
            List<Identifier> all = new ArrayList<>();
            all.addAll(gemAbilities);
            all.addAll(bonusAbilities);
            return all;
        }

        public List<Identifier> allPassives() {
            List<Identifier> all = new ArrayList<>();
            all.addAll(gemPassives);
            all.addAll(bonusPassives);
            return all;
        }

        public int totalAbilities() {
            return gemAbilities.size() + bonusAbilities.size();
        }

        public int totalPassives() {
            return gemPassives.size() + bonusPassives.size();
        }
    }

    static final Codec<PrismSelectionsState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Uuids.CODEC, PrismSelection.CODEC)
                    .optionalFieldOf("selections", Map.of())
                    .forGetter(state -> state.selections)
    ).apply(instance, PrismSelectionsState::new));

    private static final PersistentStateType<PrismSelectionsState> TYPE =
            new PersistentStateType<>(STATE_ID, PrismSelectionsState::new, CODEC, DataFixTypes.SAVED_DATA_MAP_DATA);

    public PrismSelectionsState() {
        this(Map.of());
    }

    public PrismSelectionsState(Map<UUID, PrismSelection> selections) {
        this.selections = new HashMap<>(selections == null ? Map.of() : selections);
    }

    public static PrismSelectionsState get(MinecraftServer server) {
        PersistentStateManager manager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        return manager.getOrCreate(TYPE);
    }

    public PrismSelection getSelection(UUID playerUuid) {
        return selections.getOrDefault(playerUuid, PrismSelection.empty());
    }

    /**
     * Try to add a gem ability to player's Prism selection.
     * @return true if added, false if limit reached or blacklisted
     */
    public boolean addGemAbility(UUID playerUuid, Identifier abilityId) {
        if (BonusPoolRegistry.isBlacklisted(abilityId)) {
            return false;
        }
        if (ModAbilities.get(abilityId) == null) {
            return false;
        }
        
        PrismSelection current = getSelection(playerUuid);
        if (current.gemAbilities.size() >= 3) {
            return false;
        }
        if (current.gemAbilities.contains(abilityId) || current.bonusAbilities.contains(abilityId)) {
            return false;
        }
        
        List<Identifier> newGemAbilities = new ArrayList<>(current.gemAbilities);
        newGemAbilities.add(abilityId);
        selections.put(playerUuid, new PrismSelection(newGemAbilities, current.bonusAbilities, current.gemPassives, current.bonusPassives));
        markDirty();
        return true;
    }

    /**
     * Try to add a bonus ability to player's Prism selection.
     * @return true if added, false if limit reached or not a bonus ability
     */
    public boolean addBonusAbility(UUID playerUuid, Identifier abilityId) {
        if (!BonusPoolRegistry.isBonusAbility(abilityId)) {
            return false;
        }
        
        PrismSelection current = getSelection(playerUuid);
        if (current.bonusAbilities.size() >= 2) {
            return false;
        }
        if (current.bonusAbilities.contains(abilityId) || current.gemAbilities.contains(abilityId)) {
            return false;
        }
        
        List<Identifier> newBonusAbilities = new ArrayList<>(current.bonusAbilities);
        newBonusAbilities.add(abilityId);
        selections.put(playerUuid, new PrismSelection(current.gemAbilities, newBonusAbilities, current.gemPassives, current.bonusPassives));
        markDirty();
        return true;
    }

    /**
     * Try to add a gem passive to player's Prism selection.
     * @return true if added, false if limit reached or blacklisted
     */
    public boolean addGemPassive(UUID playerUuid, Identifier passiveId) {
        if (BonusPoolRegistry.isBlacklisted(passiveId)) {
            return false;
        }
        if (ModPassives.get(passiveId) == null) {
            return false;
        }
        
        PrismSelection current = getSelection(playerUuid);
        if (current.gemPassives.size() >= 3) {
            return false;
        }
        if (current.gemPassives.contains(passiveId) || current.bonusPassives.contains(passiveId)) {
            return false;
        }
        
        List<Identifier> newGemPassives = new ArrayList<>(current.gemPassives);
        newGemPassives.add(passiveId);
        selections.put(playerUuid, new PrismSelection(current.gemAbilities, current.bonusAbilities, newGemPassives, current.bonusPassives));
        markDirty();
        return true;
    }

    /**
     * Try to add a bonus passive to player's Prism selection.
     * @return true if added, false if limit reached or not a bonus passive
     */
    public boolean addBonusPassive(UUID playerUuid, Identifier passiveId) {
        if (!BonusPoolRegistry.isBonusPassive(passiveId)) {
            return false;
        }
        
        PrismSelection current = getSelection(playerUuid);
        if (current.bonusPassives.size() >= 2) {
            return false;
        }
        if (current.bonusPassives.contains(passiveId) || current.gemPassives.contains(passiveId)) {
            return false;
        }
        
        List<Identifier> newBonusPassives = new ArrayList<>(current.bonusPassives);
        newBonusPassives.add(passiveId);
        selections.put(playerUuid, new PrismSelection(current.gemAbilities, current.bonusAbilities, current.gemPassives, newBonusPassives));
        markDirty();
        return true;
    }

    /**
     * Clear all selections for a player.
     */
    public void clearSelections(UUID playerUuid) {
        if (selections.remove(playerUuid) != null) {
            markDirty();
        }
    }

    /**
     * Remove a specific ability from player's selections.
     */
    public void removeAbility(UUID playerUuid, Identifier abilityId) {
        PrismSelection current = getSelection(playerUuid);
        List<Identifier> newGemAbilities = new ArrayList<>(current.gemAbilities);
        List<Identifier> newBonusAbilities = new ArrayList<>(current.bonusAbilities);
        
        boolean changed = newGemAbilities.remove(abilityId) || newBonusAbilities.remove(abilityId);
        if (changed) {
            selections.put(playerUuid, new PrismSelection(newGemAbilities, newBonusAbilities, current.gemPassives, current.bonusPassives));
            markDirty();
        }
    }

    /**
     * Remove a specific passive from player's selections.
     */
    public void removePassive(UUID playerUuid, Identifier passiveId) {
        PrismSelection current = getSelection(playerUuid);
        List<Identifier> newGemPassives = new ArrayList<>(current.gemPassives);
        List<Identifier> newBonusPassives = new ArrayList<>(current.bonusPassives);
        
        boolean changed = newGemPassives.remove(passiveId) || newBonusPassives.remove(passiveId);
        if (changed) {
            selections.put(playerUuid, new PrismSelection(current.gemAbilities, current.bonusAbilities, newGemPassives, newBonusPassives));
            markDirty();
        }
    }
}
