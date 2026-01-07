package com.feel.gems.power.ability.sentinel;

import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import java.util.*;

public final class SentinelLockdownRuntime {
    private static final Map<UUID, ZoneData> ACTIVE_ZONES = new HashMap<>();
    /** Duration of root/slowness effect applied each tick (slightly longer than tick interval for continuity) */
    private static final int ROOT_DURATION_TICKS = 25;
    /** Slowness amplifier for rooted mobs (255 = complete immobilization) */
    private static final int ROOT_SLOWNESS_AMPLIFIER = 255;

    private SentinelLockdownRuntime() {}

    public static void createZone(UUID ownerId, Vec3d center, int radius, long endTime, String worldId) {
        ACTIVE_ZONES.put(ownerId, new ZoneData(ownerId, center, radius, endTime, worldId));
    }

    public static boolean isInLockdownZone(Vec3d position, UUID playerId, String worldId) {
        for (ZoneData zone : ACTIVE_ZONES.values()) {
            if (!zone.worldId.equals(worldId)) continue;
            if (zone.ownerId.equals(playerId)) continue; // Owner is immune

            double distSq = position.squaredDistanceTo(zone.center);
            if (distSq <= zone.radius * zone.radius) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get the zone a position is in, or null if not in any zone.
     */
    public static ZoneData getZoneAt(Vec3d position, UUID excludeOwner, String worldId) {
        for (ZoneData zone : ACTIVE_ZONES.values()) {
            if (!zone.worldId.equals(worldId)) continue;
            if (excludeOwner != null && zone.ownerId.equals(excludeOwner)) continue;

            double distSq = position.squaredDistanceTo(zone.center);
            if (distSq <= zone.radius * zone.radius) {
                return zone;
            }
        }
        return null;
    }

    /**
     * Tick the lockdown zones - removes expired zones and roots hostile mobs.
     */
    public static void tick(long currentTime, String worldId, ServerWorld world) {
        // Remove expired zones
        ACTIVE_ZONES.entrySet().removeIf(entry -> 
            entry.getValue().worldId.equals(worldId) && currentTime > entry.getValue().endTime
        );
        
        // Apply rooting effects to hostile mobs in active zones
        for (ZoneData zone : ACTIVE_ZONES.values()) {
            if (!zone.worldId.equals(worldId)) continue;
            
            ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(zone.ownerId);
            
            // Create box around zone center
            Box zoneBox = new Box(
                    zone.center.x - zone.radius, zone.center.y - zone.radius, zone.center.z - zone.radius,
                    zone.center.x + zone.radius, zone.center.y + zone.radius, zone.center.z + zone.radius
            );
            
            // Find entities in the zone
            for (Entity entity : world.getOtherEntities(null, zoneBox)) {
                // Check if actually within radius (box is larger than sphere)
                if (entity.getEntityPos().squaredDistanceTo(zone.center) > zone.radius * zone.radius) {
                    continue;
                }
                
                // Root hostile mobs
                if (entity instanceof HostileEntity hostile) {
                    // Apply maximum slowness to root the mob in place
                    hostile.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.SLOWNESS, 
                            ROOT_DURATION_TICKS, 
                            ROOT_SLOWNESS_AMPLIFIER, 
                            true, false, false));
                    // Also prevent jumping
                    hostile.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.JUMP_BOOST, 
                            ROOT_DURATION_TICKS, 
                            128, // Negative jump boost (makes jumping nearly impossible)
                            true, false, false));
                    
                    // Spawn root particles occasionally
                    if (world.random.nextInt(20) == 0) {
                        world.spawnParticles(ParticleTypes.ENCHANTED_HIT, 
                                hostile.getX(), hostile.getY() + 0.2, hostile.getZ(), 
                                3, 0.3, 0.1, 0.3, 0.01);
                    }
                }
                
                // Apply ability restriction to enemy players (already handled elsewhere for ability blocking)
                // Just add visual feedback here
                if (entity instanceof ServerPlayerEntity targetPlayer) {
                    if (owner != null && !GemTrust.isTrusted(owner, targetPlayer) && !targetPlayer.getUuid().equals(zone.ownerId)) {
                        // Spawn subtle particles to show they're in lockdown
                        if (world.random.nextInt(40) == 0) {
                            world.spawnParticles(ParticleTypes.ENCHANTED_HIT,
                                    targetPlayer.getX(), targetPlayer.getY() + 1, targetPlayer.getZ(),
                                    2, 0.2, 0.3, 0.2, 0.01);
                        }
                    }
                }
            }
        }
    }

    /**
     * Backwards-compatible tick method for callers that don't pass ServerWorld.
     */
    public static void tick(long currentTime, String worldId) {
        ACTIVE_ZONES.entrySet().removeIf(entry -> 
            entry.getValue().worldId.equals(worldId) && currentTime > entry.getValue().endTime
        );
    }

    public static void clearZone(UUID ownerId) {
        ACTIVE_ZONES.remove(ownerId);
    }
    
    public static Collection<ZoneData> getActiveZones() {
        return Collections.unmodifiableCollection(ACTIVE_ZONES.values());
    }

    public record ZoneData(UUID ownerId, Vec3d center, int radius, long endTime, String worldId) {}
}
