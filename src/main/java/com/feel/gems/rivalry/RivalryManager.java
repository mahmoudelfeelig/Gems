package com.feel.gems.rivalry;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.net.RivalrySyncPayload;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsNbt;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Manages the Rivalry system where players are assigned targets for bonus damage.
 */
public final class RivalryManager {
    private static final String KEY_RIVALRY_TARGET = "rivalryTarget";

    private RivalryManager() {
    }

    // ========== Target Assignment ==========

    /**
     * Assign a new rivalry target to a player.
     * Called on spawn/respawn.
     */
    public static void assignTarget(ServerPlayerEntity player) {
        UUID newTarget = selectRandomTarget(player);
        setTarget(player, newTarget);

        String targetName = "";
        if (newTarget != null) {
            MinecraftServer server = player.getEntityWorld().getServer();
            if (server != null) {
                ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(newTarget);
                targetName = targetPlayer != null ? targetPlayer.getGameProfile().name() : "Unknown";
            }
            player.sendMessage(
                    Text.translatable("gems.rivalry.target_assigned", targetName).formatted(Formatting.GOLD),
                    false
            );
        }

        // Sync to client for HUD
        syncToClient(player, targetName);
    }

    /**
     * Reroll a new rivalry target (called after killing current target).
     */
    public static void rerollTarget(ServerPlayerEntity player) {
        assignTarget(player);
    }

    /**
     * Clear the player's rivalry target.
     */
    public static void clearTarget(ServerPlayerEntity player) {
        setTarget(player, null);
    }

    // ========== Target Queries ==========

    /**
     * Get the player's current rivalry target UUID, or null if none.
     */
    public static UUID getTarget(ServerPlayerEntity player) {
        var nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        return GemsNbt.getUuid(nbt, KEY_RIVALRY_TARGET);
    }

    /**
     * Check if a player has a rivalry target.
     */
    public static boolean hasTarget(ServerPlayerEntity player) {
        return getTarget(player) != null;
    }

    /**
     * Check if the victim is the attacker's rivalry target.
     */
    public static boolean isRivalryTarget(ServerPlayerEntity attacker, ServerPlayerEntity victim) {
        UUID target = getTarget(attacker);
        return target != null && target.equals(victim.getUuid());
    }

    /**
     * Get the name of the player's current target, or null if none/offline.
     */
    public static String getTargetName(ServerPlayerEntity player) {
        UUID target = getTarget(player);
        if (target == null) {
            return null;
        }
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return null;
        }
        ServerPlayerEntity targetPlayer = server.getPlayerManager().getPlayer(target);
        return targetPlayer != null ? targetPlayer.getGameProfile().name() : null;
    }

    // ========== Damage Bonus ==========

    /**
     * Get the damage multiplier when attacking rivalry target.
     */
    public static float getDamageMultiplier() {
        return (float) GemsBalance.v().rivalry().damageMultiplier();
    }

    /**
     * Apply rivalry damage bonus if applicable.
     * Returns the modified damage.
     */
    public static float applyRivalryBonus(ServerPlayerEntity attacker, ServerPlayerEntity victim, float baseDamage) {
        if (!isRivalryTarget(attacker, victim)) {
            return baseDamage;
        }
        return baseDamage * getDamageMultiplier();
    }

    // ========== Kill Handler ==========

    /**
     * Called when a player kills another player.
     * Rerolls target if the killed player was the rivalry target.
     */
    public static void onPlayerKill(ServerPlayerEntity killer, ServerPlayerEntity victim) {
        if (isRivalryTarget(killer, victim)) {
            killer.sendMessage(
                    Text.translatable("gems.rivalry.target_killed", victim.getGameProfile().name())
                            .formatted(Formatting.GREEN),
                    false
            );
            rerollTarget(killer);
        }
    }

    // ========== Internal Helpers ==========

    private static void setTarget(ServerPlayerEntity player, UUID target) {
        var nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        GemsNbt.putUuid(nbt, KEY_RIVALRY_TARGET, target);
    }

    private static void syncToClient(ServerPlayerEntity player, String targetName) {
        if (GemsBalance.v().rivalry().showInHud()) {
            ServerPlayNetworking.send(player, new RivalrySyncPayload(targetName != null ? targetName : ""));
        }
    }

    private static UUID selectRandomTarget(ServerPlayerEntity player) {
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return null;
        }

        List<ServerPlayerEntity> candidates = new ArrayList<>();
        for (ServerPlayerEntity other : server.getPlayerManager().getPlayerList()) {
            // Cannot target self
            if (other.getUuid().equals(player.getUuid())) {
                continue;
            }
            candidates.add(other);
        }

        if (candidates.isEmpty()) {
            return null;
        }

        // Random selection
        int index = player.getRandom().nextInt(candidates.size());
        return candidates.get(index).getUuid();
    }
}
