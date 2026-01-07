package com.feel.gems.power.ability.trickster;

import com.feel.gems.state.PlayerStateManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;

import java.util.List;

public final class TricksterMindGamesRuntime {
    private static final String MIND_GAMES_END_KEY = "trickster_mind_games_end";
    
    // Command tag prefixes for mob mind games (similar to HypnoControl pattern)
    private static final String TAG_CONFUSED = "gems_confused";
    private static final String TAG_CONFUSED_UNTIL_PREFIX = "gems_confused_until:";

    private TricksterMindGamesRuntime() {}

    // ========== Player Mind Games ==========

    public static void applyMindGames(ServerPlayerEntity player, int durationTicks) {
        long endTime = player.getEntityWorld().getTime() + durationTicks;
        PlayerStateManager.setPersistent(player, MIND_GAMES_END_KEY, String.valueOf(endTime));
    }

    public static boolean hasReversedControls(ServerPlayerEntity player) {
        String endStr = PlayerStateManager.getPersistent(player, MIND_GAMES_END_KEY);
        if (endStr == null) return false;

        long endTime = Long.parseLong(endStr);
        if (player.getEntityWorld().getTime() > endTime) {
            clearMindGames(player);
            return false;
        }

        return true;
    }

    public static void clearMindGames(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, MIND_GAMES_END_KEY);
    }

    // ========== Mob Mind Games ==========

    /**
     * Apply mind games effect to a mob using command tags, making it confused and attack random targets.
     */
    public static void applyMindGamesMob(MobEntity mob, int durationTicks) {
        long endTime = mob.getEntityWorld().getTime() + durationTicks;
        
        // Clear any existing confused tags first
        clearConfusedMobTags(mob);
        
        // Add new confused tags
        mob.addCommandTag(TAG_CONFUSED);
        mob.addCommandTag(TAG_CONFUSED_UNTIL_PREFIX + endTime);
    }

    /**
     * Check if a mob is currently under mind games effect.
     */
    public static boolean isMobConfused(MobEntity mob) {
        if (!mob.getCommandTags().contains(TAG_CONFUSED)) {
            return false;
        }
        
        long until = getMobConfusedUntil(mob);
        if (until > 0 && mob.getEntityWorld().getTime() > until) {
            clearMindGamesMob(mob);
            return false;
        }
        
        return true;
    }

    /**
     * Get the end tick for a confused mob.
     */
    private static long getMobConfusedUntil(MobEntity mob) {
        for (String tag : mob.getCommandTags()) {
            if (tag.startsWith(TAG_CONFUSED_UNTIL_PREFIX)) {
                try {
                    return Long.parseLong(tag.substring(TAG_CONFUSED_UNTIL_PREFIX.length()));
                } catch (NumberFormatException e) {
                    return 0L;
                }
            }
        }
        return 0L;
    }

    /**
     * Tick confused mob to maintain random hostile behavior.
     * Called periodically to switch the mob's target to random nearby entities.
     */
    public static void tickConfusedMob(MobEntity mob, ServerWorld world) {
        if (!isMobConfused(mob)) {
            return;
        }

        // Every few ticks, potentially change target to a random entity
        if (world.getTime() % 20 == 0) { // Check every second
            Box searchBox = mob.getBoundingBox().expand(16);
            List<LivingEntity> nearbyEntities = world.getEntitiesByClass(
                LivingEntity.class,
                searchBox,
                e -> e != mob && e.isAlive()
            );

            if (!nearbyEntities.isEmpty() && world.getRandom().nextFloat() < 0.5f) {
                // 50% chance to switch to a random target
                LivingEntity randomTarget = nearbyEntities.get(world.getRandom().nextInt(nearbyEntities.size()));
                mob.setTarget(randomTarget);
            }
        }
    }

    public static void clearMindGamesMob(MobEntity mob) {
        clearConfusedMobTags(mob);
    }

    private static void clearConfusedMobTags(MobEntity mob) {
        mob.removeCommandTag(TAG_CONFUSED);
        mob.getCommandTags().removeIf(tag -> tag.startsWith(TAG_CONFUSED_UNTIL_PREFIX));
    }
}
