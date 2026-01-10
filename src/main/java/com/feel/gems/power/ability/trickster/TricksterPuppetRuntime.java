package com.feel.gems.power.ability.trickster;

import com.feel.gems.state.PlayerStateManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import java.util.List;
import java.util.UUID;

public final class TricksterPuppetRuntime {
    private static final String PUPPETED_BY_KEY = "trickster_puppeted_by";
    private static final String PUPPETED_END_KEY = "trickster_puppeted_end";
    
    // Command tag prefixes for mob puppeting (similar to HypnoControl pattern)
    private static final String TAG_PUPPETED = "gems_puppeted";
    private static final String TAG_PUPPETED_BY_PREFIX = "gems_puppeted_by:";
    private static final String TAG_PUPPETED_UNTIL_PREFIX = "gems_puppeted_until:";

    private TricksterPuppetRuntime() {}

    // ========== Player Puppeting ==========

    public static void setPuppeted(ServerPlayerEntity target, UUID puppeteerId, int durationTicks) {
        long endTime = target.getEntityWorld().getTime() + durationTicks;
        PlayerStateManager.setPersistent(target, PUPPETED_BY_KEY, puppeteerId.toString());
        PlayerStateManager.setPersistent(target, PUPPETED_END_KEY, String.valueOf(endTime));
        TricksterControlSync.sync(target);
    }

    public static UUID getPuppeteer(ServerPlayerEntity player) {
        String puppeteerStr = PlayerStateManager.getPersistent(player, PUPPETED_BY_KEY);
        if (puppeteerStr == null || puppeteerStr.isEmpty()) return null;

        String endStr = PlayerStateManager.getPersistent(player, PUPPETED_END_KEY);
        if (endStr == null) return null;

        long endTime = Long.parseLong(endStr);
        if (player.getEntityWorld().getTime() > endTime) {
            clearPuppeted(player);
            return null;
        }

        try {
            return UUID.fromString(puppeteerStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isPuppeted(ServerPlayerEntity player) {
        return getPuppeteer(player) != null;
    }

    public static void clearPuppeted(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, PUPPETED_BY_KEY);
        PlayerStateManager.clearPersistent(player, PUPPETED_END_KEY);
        TricksterControlSync.sync(player);
    }

    public static void tickPuppetedPlayer(ServerPlayerEntity target) {
        UUID puppeteerId = getPuppeteer(target);
        if (puppeteerId == null) {
            return;
        }
        ServerWorld world = target.getEntityWorld();
        if (world == null || world.getServer() == null) {
            return;
        }
        ServerPlayerEntity puppeteer = world.getServer().getPlayerManager().getPlayer(puppeteerId);
        if (puppeteer == null) {
            clearPuppeted(target);
            return;
        }

        // Mirror the puppeteer's movement and facing.
        target.setVelocity(puppeteer.getVelocity());
        target.velocityDirty = true;
        target.setYaw(puppeteer.getYaw());
        target.setPitch(puppeteer.getPitch());
        target.setHeadYaw(puppeteer.getHeadYaw());
    }

    // ========== Mob Puppeting ==========

    /**
     * Mark a mob as puppeted using command tags. The mob will attack its allies for the duration.
     */
    public static void setPuppetedMob(MobEntity mob, UUID puppeteerId, int durationTicks) {
        long endTime = mob.getEntityWorld().getTime() + durationTicks;
        
        // Clear any existing puppeted tags first
        clearPuppetedMobTags(mob);
        
        // Add new puppeted tags
        mob.addCommandTag(TAG_PUPPETED);
        mob.addCommandTag(TAG_PUPPETED_BY_PREFIX + puppeteerId.toString());
        mob.addCommandTag(TAG_PUPPETED_UNTIL_PREFIX + endTime);
    }

    /**
     * Check if a mob is currently puppeted.
     */
    public static boolean isMobPuppeted(MobEntity mob) {
        if (!mob.getCommandTags().contains(TAG_PUPPETED)) {
            return false;
        }
        
        long until = getMobPuppetedUntil(mob);
        if (until > 0 && mob.getEntityWorld().getTime() > until) {
            clearPuppetedMob(mob);
            return false;
        }
        
        return true;
    }

    /**
     * Get the end tick for a puppeted mob.
     */
    private static long getMobPuppetedUntil(MobEntity mob) {
        for (String tag : mob.getCommandTags()) {
            if (tag.startsWith(TAG_PUPPETED_UNTIL_PREFIX)) {
                try {
                    return Long.parseLong(tag.substring(TAG_PUPPETED_UNTIL_PREFIX.length()));
                } catch (NumberFormatException e) {
                    return 0L;
                }
            }
        }
        return 0L;
    }

    /**
     * Tick puppeted mob to maintain hostile behavior towards allies.
     */
    public static void tickPuppetedMob(MobEntity mob, ServerWorld world) {
        if (!isMobPuppeted(mob)) {
            return;
        }

        // Prefer targeting other mobs so the effect is visible and persists against vanilla AI retargeting.
        LivingEntity currentTarget = mob.getTarget();
        boolean needsNewTarget = currentTarget == null || !currentTarget.isAlive() || !(currentTarget instanceof MobEntity);
        if (!needsNewTarget) {
            return;
        }

        Box searchBox = mob.getBoundingBox().expand(16);
        List<MobEntity> nearbyMobs = world.getEntitiesByClass(
                MobEntity.class,
                searchBox,
                e -> e != mob && e.isAlive()
        );

        if (!nearbyMobs.isEmpty()) {
            MobEntity newTarget = nearbyMobs.get(world.getRandom().nextInt(nearbyMobs.size()));
            mob.setTarget(newTarget);
            return;
        }

        // Fallback: if there are no other mobs around, pick any living entity so the effect still does something.
        List<LivingEntity> nearbyLiving = world.getEntitiesByClass(
                LivingEntity.class,
                searchBox,
                e -> e != mob && e.isAlive()
        );
        if (!nearbyLiving.isEmpty()) {
            mob.setTarget(nearbyLiving.get(world.getRandom().nextInt(nearbyLiving.size())));
        }
    }

    public static void clearPuppetedMob(MobEntity mob) {
        clearPuppetedMobTags(mob);
        mob.setTarget(null);
    }

    private static void clearPuppetedMobTags(MobEntity mob) {
        mob.removeCommandTag(TAG_PUPPETED);
        // Remove any puppeted_by and puppeted_until tags (avoid mutating the tag set directly).
        for (String tag : List.copyOf(mob.getCommandTags())) {
            if (tag.startsWith(TAG_PUPPETED_BY_PREFIX) || tag.startsWith(TAG_PUPPETED_UNTIL_PREFIX)) {
                mob.removeCommandTag(tag);
            }
        }
    }
}
