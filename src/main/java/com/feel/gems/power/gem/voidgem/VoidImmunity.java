package com.feel.gems.power.gem.voidgem;

import com.feel.gems.core.GemId;
import com.feel.gems.state.GemPlayerState;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Utility for checking Void gem immunity.
 * The Void gem's only passive makes the player immune to all gem abilities and passives from other players.
 */
public final class VoidImmunity {
    
    private VoidImmunity() {
    }

    /**
     * Check if a target player has Void immunity active.
     * Void immunity requires:
     * - Active gem is VOID
     * - Energy level >= 1 (to have passives active)
     */
    public static boolean hasImmunity(ServerPlayerEntity player) {
        if (player == null) {
            return false;
        }
        GemId activeGem = GemPlayerState.getActiveGem(player);
        if (activeGem != GemId.VOID) {
            return false;
        }
        int energy = GemPlayerState.getEnergy(player);
        return energy >= 1; // Passives unlock at level 1
    }

    /**
     * Check if an ability/passive from sourcePlayer should be blocked when targeting targetPlayer.
     * Returns true if the effect should be blocked (target has Void immunity).
     * 
     * @param sourcePlayer The player using the ability/passive (can be null for environmental effects)
     * @param targetPlayer The player being targeted
     * @return true if the effect should be blocked
     */
    public static boolean shouldBlockEffect(ServerPlayerEntity sourcePlayer, ServerPlayerEntity targetPlayer) {
        if (targetPlayer == null) {
            return false;
        }
        if (sourcePlayer == null) {
            return false; // Environmental effects aren't blocked
        }
        if (sourcePlayer.equals(targetPlayer)) {
            return false; // Self-targeting abilities aren't blocked
        }
        return hasImmunity(targetPlayer);
    }
}
