package com.feel.gems.power.gem.chaos;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.power.registry.ModPassives;
import com.feel.gems.state.GemPlayerState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import java.util.*;

/**
 * Runtime for Chaos gem's random ability/passive rotation.
 * Every 5 minutes, assigns a random ability and passive from any gem.
 */
public final class ChaosRotationRuntime {
    private static final int ROTATION_TICKS = 6000; // 5 minutes
    private static final int ABILITY_COOLDOWN_TICKS = 200; // 10 seconds between uses
    
    // Maps player UUID -> their current chaos state
    private static final Map<UUID, ChaosState> playerStates = new HashMap<>();
    
    public record ChaosState(
            Identifier currentAbility,
            Identifier currentPassive,
            long lastRotationTick,
            long lastAbilityUseTick
    ) {
        public boolean canUseAbility(long currentTick) {
            return currentTick - lastAbilityUseTick >= ABILITY_COOLDOWN_TICKS;
        }

        public ChaosState withAbilityUsed(long tick) {
            return new ChaosState(currentAbility, currentPassive, lastRotationTick, tick);
        }

        public ChaosState withRotation(Identifier ability, Identifier passive, long tick) {
            return new ChaosState(ability, passive, tick, lastAbilityUseTick);
        }
    }

    private ChaosRotationRuntime() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ChaosRotationRuntime::tick);
    }

    public static void tick(MinecraftServer server) {
        long currentTick = server.getOverworld().getTime();
        
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (GemPlayerState.getActiveGem(player) != GemId.CHAOS) {
                playerStates.remove(player.getUuid());
                continue;
            }
            
            int energy = GemPlayerState.getEnergy(player);
            if (energy < 1) {
                continue; // No passives at 0 energy
            }
            
            ChaosState state = playerStates.get(player.getUuid());
            if (state == null || currentTick - state.lastRotationTick >= ROTATION_TICKS) {
                rotate(player, currentTick);
            }
        }
    }

    private static void rotate(ServerPlayerEntity player, long currentTick) {
        UUID uuid = player.getUuid();
        ChaosState oldState = playerStates.get(uuid);
        
        // Remove old passive effects
        if (oldState != null && oldState.currentPassive != null) {
            GemPassive oldPassive = ModPassives.get(oldState.currentPassive);
            if (oldPassive != null) {
                oldPassive.remove(player);
            }
        }
        
        // Pick random ability and passive from all gems
        Identifier newAbility = pickRandomAbility(player.getRandom());
        Identifier newPassive = pickRandomPassive(player.getRandom());
        
        ChaosState newState = new ChaosState(newAbility, newPassive, currentTick, 
                oldState != null ? oldState.lastAbilityUseTick : 0);
        playerStates.put(uuid, newState);
        
        // Apply new passive
        if (newPassive != null) {
            GemPassive passive = ModPassives.get(newPassive);
            if (passive != null) {
                passive.apply(player);
            }
        }
        
        // Notify player
        String abilityName = newAbility != null ? getAbilityName(newAbility) : "None";
        String passiveName = newPassive != null ? getPassiveName(newPassive) : "None";
        
        player.sendMessage(Text.literal("⚡ Chaos Rotation! ⚡").formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD), false);
        player.sendMessage(Text.literal("  Ability: " + abilityName).formatted(Formatting.AQUA), false);
        player.sendMessage(Text.literal("  Passive: " + passiveName).formatted(Formatting.GREEN), false);
    }

    private static Identifier pickRandomAbility(net.minecraft.util.math.random.Random random) {
        List<Identifier> allAbilities = new ArrayList<>();
        for (GemId gemId : GemId.values()) {
            if (gemId == GemId.CHAOS || gemId == GemId.VOID || gemId == GemId.PRISM) {
                continue; // Skip special gems
            }
            try {
                GemDefinition def = GemRegistry.definition(gemId);
                allAbilities.addAll(def.abilities());
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
                continue; // Skip special gems
            }
            try {
                GemDefinition def = GemRegistry.definition(gemId);
                allPassives.addAll(def.passives());
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
     * Get the current chaos state for a player.
     */
    public static ChaosState getState(UUID playerUuid) {
        return playerStates.get(playerUuid);
    }

    /**
     * Try to activate the current chaos ability.
     */
    public static boolean activateAbility(ServerPlayerEntity player) {
        ChaosState state = playerStates.get(player.getUuid());
        if (state == null || state.currentAbility == null) {
            return false;
        }
        
        long currentTick = player.getEntityWorld().getTime();
        if (!state.canUseAbility(currentTick)) {
            int remainingTicks = (int)(ABILITY_COOLDOWN_TICKS - (currentTick - state.lastAbilityUseTick));
            int remainingSeconds = (remainingTicks + 19) / 20;
            player.sendMessage(Text.literal("Chaos ability on cooldown: " + remainingSeconds + "s").formatted(Formatting.RED), true);
            return false;
        }
        
        GemAbility ability = ModAbilities.get(state.currentAbility);
        if (ability == null) {
            return false;
        }
        
        boolean success = ability.activate(player);
        if (success) {
            playerStates.put(player.getUuid(), state.withAbilityUsed(currentTick));
        }
        return success;
    }

    /**
     * Clear state when player switches away from Chaos gem.
     */
    public static void clearState(ServerPlayerEntity player) {
        ChaosState state = playerStates.remove(player.getUuid());
        if (state != null && state.currentPassive != null) {
            GemPassive passive = ModPassives.get(state.currentPassive);
            if (passive != null) {
                passive.remove(player);
            }
        }
    }
}
