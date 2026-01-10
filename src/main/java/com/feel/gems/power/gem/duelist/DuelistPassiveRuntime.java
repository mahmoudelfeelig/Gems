package com.feel.gems.power.gem.duelist;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import java.util.List;

/**
 * Duelist passive runtime implementations.
 */
public final class DuelistPassiveRuntime {
    private static final String RIPOSTE_END_KEY = "duelist_riposte_end";

    private DuelistPassiveRuntime() {}

    // ========== Riposte ==========
    // After a successful block (parry), your next attack within 2s deals 50% bonus damage.

    public static void triggerRiposte(ServerPlayerEntity player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.DUELIST_RIPOSTE)) {
            return;
        }
        int windowTicks = GemsBalance.v().duelist().riposteWindowTicks();
        long endTime = player.getEntityWorld().getTime() + windowTicks;
        PlayerStateManager.setPersistent(player, RIPOSTE_END_KEY, String.valueOf(endTime));
    }

    public static boolean consumeRiposte(ServerPlayerEntity player) {
        String endStr = PlayerStateManager.getPersistent(player, RIPOSTE_END_KEY);
        if (endStr == null || endStr.isEmpty()) return false;

        long endTime = Long.parseLong(endStr);
        if (player.getEntityWorld().getTime() > endTime) {
            clearRiposte(player);
            return false;
        }

        clearRiposte(player);
        return true;
    }

    public static void clearRiposte(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, RIPOSTE_END_KEY);
    }

    public static float getRiposteDamageMultiplier() {
        return GemsBalance.v().duelist().riposteBonusDamageMultiplier();
    }

    // ========== Duelist's Focus ==========
    // Deal 25% more damage in 1v1 combat when no other players are within 15 blocks.

    public static boolean isIn1v1Combat(ServerPlayerEntity attacker, ServerPlayerEntity target) {
        if (!GemPowers.isPassiveActive(attacker, PowerIds.DUELIST_FOCUS)) {
            return false;
        }

        int range = GemsBalance.v().duelist().focusRadiusBlocks();
        Box searchBox = attacker.getBoundingBox().expand(range);
        if (!(attacker.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }

        List<ServerPlayerEntity> nearbyPlayers = world.getEntitiesByClass(
            ServerPlayerEntity.class,
            searchBox,
            p -> p != attacker && p != target && p.isAlive()
        );

        return nearbyPlayers.isEmpty();
    }

    public static float getFocusDamageMultiplier() {
        return GemsBalance.v().duelist().focusBonusDamageMultiplier();
    }

    // ========== Combat Stance ==========
    // While holding a sword, gain 10% movement speed.
    // This is handled via status effect application in the passive tick.
}
