package com.feel.gems.power.gem.chaos;

import com.feel.gems.bonus.BonusPoolRegistry;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.net.ChaosSlotPayload;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.registry.ModPassives;
import com.feel.gems.state.GemPlayerState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import java.util.*;

/**
 * Runtime for Chaos gem's independent ability slots.
 * Each slot can be activated with LAlt+1-N to get a random ability+passive.
 * The ability/passive lasts for a duration, during which the ability can be used.
 * After the duration, the slot becomes inactive until re-activated.
 */
public final class ChaosSlotRuntime {
    /** @deprecated Use {@link #slotCount()} instead. Kept for legacy compatibility. */
    @Deprecated
    public static final int SLOT_COUNT = 6;

    /**
     * Returns the configured number of Chaos slots (1-9).
     */
    public static int slotCount() {
        return GemsBalance.v().chaos().slotCount();
    }

    private static int slotDurationTicks() {
        return Math.max(1, GemsBalance.v().chaos().slotDurationTicks());
    }

    private static int slotAbilityCooldownTicks() {
        return Math.max(0, GemsBalance.v().chaos().slotAbilityCooldownTicks());
    }
    
    // Maps player UUID -> their chaos slot states
    private static final Map<UUID, ChaosPlayerState> playerStates = new HashMap<>();
    
    public record SlotState(
            Identifier abilityId,
            String abilityName,
            Identifier passiveId,
            String passiveName,
            long activationTick,
            long lastAbilityUseTick
    ) {
        public static SlotState inactive() {
            return new SlotState(null, "", null, "", 0, 0);
        }
        
        public boolean isActive(long currentTick) {
            return abilityId != null && (currentTick - activationTick) < slotDurationTicks();
        }
        
        public int remainingTicks(long currentTick) {
            if (abilityId == null) return 0;
            return (int) Math.max(0, slotDurationTicks() - (currentTick - activationTick));
        }
        
        public boolean canUseAbility(long currentTick) {
            return isActive(currentTick) && (currentTick - lastAbilityUseTick) >= slotAbilityCooldownTicks();
        }
        
        public int abilityCooldownRemaining(long currentTick) {
            return (int) Math.max(0, slotAbilityCooldownTicks() - (currentTick - lastAbilityUseTick));
        }
        
        public SlotState withAbilityUsed(long tick) {
            return new SlotState(abilityId, abilityName, passiveId, passiveName, activationTick, tick);
        }
    }
    
    public record ChaosPlayerState(SlotState[] slots) {
        public ChaosPlayerState() {
            this(createEmptySlots());
        }

        private static SlotState[] createEmptySlots() {
            SlotState[] slots = new SlotState[slotCount()];
            Arrays.fill(slots, SlotState.inactive());
            return slots;
        }
        
        public SlotState getSlot(int index) {
            if (index < 0 || index >= slotCount()) return SlotState.inactive();
            // Handle dynamic slot count changes gracefully
            if (index >= slots.length) return SlotState.inactive();
            return slots[index];
        }
        
        public ChaosPlayerState withSlot(int index, SlotState state) {
            int count = slotCount();
            SlotState[] newSlots = new SlotState[count];
            for (int i = 0; i < count; i++) {
                if (i < slots.length) {
                    newSlots[i] = slots[i];
                } else {
                    newSlots[i] = SlotState.inactive();
                }
            }
            if (index >= 0 && index < count) {
                newSlots[index] = state;
            }
            return new ChaosPlayerState(newSlots);
        }
    }

    private ChaosSlotRuntime() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ChaosSlotRuntime::tick);
    }

    public static void tick(MinecraftServer server) {
        long currentTick = server.getOverworld().getTime();
        
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            GemPlayerState.initIfNeeded(player);
            if (GemPlayerState.getActiveGem(player) != GemId.CHAOS) {
                ChaosPlayerState removed = playerStates.remove(player.getUuid());
                if (removed != null) {
                    // Remove all passive effects when switching away
                    for (int i = 0; i < slotCount(); i++) {
                        SlotState slot = removed.getSlot(i);
                        if (slot.passiveId != null) {
                            GemPassive passive = ModPassives.get(slot.passiveId);
                            if (passive != null) {
                                passive.remove(player);
                            }
                        }
                    }
                    // Clear client state
                    syncAllToClient(player, new ChaosPlayerState(), currentTick);
                }
                continue;
            }
            
            int energy = GemPlayerState.getEnergy(player);
            if (energy < 1) {
                continue;
            }
            
            ChaosPlayerState state = playerStates.computeIfAbsent(player.getUuid(), k -> new ChaosPlayerState());
            
            // Check for expired slots and remove passives
            boolean changed = false;
            for (int i = 0; i < slotCount(); i++) {
                SlotState slot = state.getSlot(i);
                if (slot.abilityId != null && !slot.isActive(currentTick)) {
                    // Slot expired - remove passive
                    if (slot.passiveId != null) {
                        GemPassive passive = ModPassives.get(slot.passiveId);
                        if (passive != null) {
                            passive.remove(player);
                        }
                    }
                    state = state.withSlot(i, SlotState.inactive());
                    changed = true;
                    player.sendMessage(Text.translatable("gems.chaos.slot_expired", i + 1).formatted(Formatting.GRAY), true);
                }
            }
            
            if (changed) {
                playerStates.put(player.getUuid(), state);
            }
            
            // Sync to client every second
            if (currentTick % 20 == 0) {
                syncAllToClient(player, state, currentTick);
            }
        }
    }

    /**
     * Activate a chaos slot - either use the ability if active, or roll new ability/passive if inactive.
     */
    public static boolean activateSlot(ServerPlayerEntity player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slotCount()) {
            return false;
        }
        
        long currentTick = player.getEntityWorld().getTime();
        ChaosPlayerState state = playerStates.computeIfAbsent(player.getUuid(), k -> new ChaosPlayerState());
        SlotState slot = state.getSlot(slotIndex);
        
        if (slot.isActive(currentTick)) {
            // Slot is active - try to use the ability
            return useSlotAbility(player, slotIndex, state, slot, currentTick);
        } else {
            // Slot is inactive - roll new ability and passive
            rollNewSlot(player, slotIndex, state, currentTick);
            return true;
        }
    }
    
    private static boolean useSlotAbility(ServerPlayerEntity player, int slotIndex, ChaosPlayerState state, SlotState slot, long currentTick) {
        if (!slot.canUseAbility(currentTick)) {
            int remaining = slot.abilityCooldownRemaining(currentTick);
            int seconds = (remaining + 19) / 20;
            player.sendMessage(Text.translatable("gems.chaos.slot_on_cooldown", slotIndex + 1, seconds).formatted(Formatting.RED), true);
            return false;
        }
        
        GemAbility ability = ModAbilities.get(slot.abilityId);
        if (ability == null) {
            player.sendMessage(Text.translatable("gems.chaos.ability_not_found").formatted(Formatting.RED), true);
            return false;
        }
        
        boolean success = ability.activate(player);
        if (success) {
            SlotState newSlot = slot.withAbilityUsed(currentTick);
            ChaosPlayerState newState = state.withSlot(slotIndex, newSlot);
            playerStates.put(player.getUuid(), newState);
            syncAllToClient(player, newState, currentTick);
        }
        return success;
    }
    
    private static void rollNewSlot(ServerPlayerEntity player, int slotIndex, ChaosPlayerState state, long currentTick) {
        // Remove old passive if any
        SlotState oldSlot = state.getSlot(slotIndex);
        if (oldSlot.passiveId != null) {
            GemPassive oldPassive = ModPassives.get(oldSlot.passiveId);
            if (oldPassive != null) {
                oldPassive.remove(player);
            }
        }
        
        // Pick random ability and passive
        Identifier newAbilityId = pickRandomAbility(player.getRandom());
        Identifier newPassiveId = pickRandomPassive(player.getRandom());
        
        String abilityName = newAbilityId != null ? getAbilityName(newAbilityId) : "None";
        String passiveName = newPassiveId != null ? getPassiveName(newPassiveId) : "None";
        
        SlotState newSlot = new SlotState(newAbilityId, abilityName, newPassiveId, passiveName, currentTick, 0);
        ChaosPlayerState newState = state.withSlot(slotIndex, newSlot);
        playerStates.put(player.getUuid(), newState);
        
        // Apply new passive
        if (newPassiveId != null) {
            GemPassive passive = ModPassives.get(newPassiveId);
            if (passive != null) {
                passive.apply(player);
            }
        }
        
        // Notify player
        player.sendMessage(Text.translatable("gems.chaos.slot_rolled", slotIndex + 1).formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), false);
        player.sendMessage(Text.translatable("gems.chaos.ability_label", abilityName).formatted(Formatting.AQUA), false);
        player.sendMessage(Text.translatable("gems.chaos.passive_label", passiveName).formatted(Formatting.GREEN), false);
        player.sendMessage(Text.translatable("gems.chaos.duration_label").formatted(Formatting.GRAY), false);
        
        syncAllToClient(player, newState, currentTick);
    }

    /**
     * Sync all slot states to the client.
     */
    public static void syncAllToClient(ServerPlayerEntity player, ChaosPlayerState state, long currentTick) {
        List<ChaosSlotPayload.SlotData> slots = new ArrayList<>();
        for (int i = 0; i < slotCount(); i++) {
            SlotState slot = state.getSlot(i);
            slots.add(new ChaosSlotPayload.SlotData(
                slot.abilityName,
                slot.abilityId != null ? slot.abilityId.toString() : "",
                slot.passiveName,
                slot.remainingTicks(currentTick) / 20,
                slot.abilityCooldownRemaining(currentTick) / 20
            ));
        }
        ServerPlayNetworking.send(player, new ChaosSlotPayload(slots));
    }

    private static Identifier pickRandomAbility(net.minecraft.util.math.random.Random random) {
        List<Identifier> allAbilities = new ArrayList<>();
        for (GemId gemId : GemId.values()) {
            if (gemId == GemId.CHAOS || gemId == GemId.VOID || gemId == GemId.PRISM) {
                continue;
            }
            try {
                GemDefinition def = GemRegistry.definition(gemId);
                for (Identifier id : def.abilities()) {
                    // Skip blacklisted abilities that have dependencies
                    if (!BonusPoolRegistry.isChaosAbilityBlacklisted(id)) {
                        allAbilities.add(id);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        
        if (allAbilities.isEmpty()) {
            return null;
        }
        return allAbilities.get(random.nextInt(allAbilities.size()));
    }

    private static Identifier pickRandomPassive(net.minecraft.util.math.random.Random random) {
        List<Identifier> allPassives = new ArrayList<>();
        for (GemId gemId : GemId.values()) {
            if (gemId == GemId.CHAOS || gemId == GemId.VOID || gemId == GemId.PRISM) {
                continue;
            }
            try {
                GemDefinition def = GemRegistry.definition(gemId);
                for (Identifier id : def.passives()) {
                    // Skip blacklisted passives that have dependencies
                    if (!BonusPoolRegistry.isChaosPassiveBlacklisted(id)) {
                        allPassives.add(id);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        
        if (allPassives.isEmpty()) {
            return null;
        }
        return allPassives.get(random.nextInt(allPassives.size()));
    }

    private static String getAbilityName(Identifier id) {
        GemAbility ability = ModAbilities.get(id);
        return ability != null ? ability.name() : id.getPath();
    }

    private static String getPassiveName(Identifier id) {
        GemPassive passive = ModPassives.get(id);
        return passive != null ? passive.name() : id.getPath();
    }

    /**
     * Get the current state for a player.
     */
    public static ChaosPlayerState getState(UUID playerUuid) {
        return playerStates.getOrDefault(playerUuid, new ChaosPlayerState());
    }

    /**
     * Clear all state when player switches away from Chaos gem.
     */
    public static void clearState(ServerPlayerEntity player) {
        ChaosPlayerState state = playerStates.remove(player.getUuid());
        if (state != null) {
            for (int i = 0; i < slotCount(); i++) {
                SlotState slot = state.getSlot(i);
                if (slot.passiveId != null) {
                    GemPassive passive = ModPassives.get(slot.passiveId);
                    if (passive != null) {
                        passive.remove(player);
                    }
                }
            }
        }
    }
}
