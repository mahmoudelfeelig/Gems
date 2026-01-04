package com.feel.gems.power.ability.sentinel;

import net.minecraft.util.math.Vec3d;
import java.util.*;

public final class SentinelLockdownRuntime {
    private static final Map<UUID, ZoneData> ACTIVE_ZONES = new HashMap<>();

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

    public static void tick(long currentTime, String worldId) {
        ACTIVE_ZONES.entrySet().removeIf(entry -> 
            entry.getValue().worldId.equals(worldId) && currentTime > entry.getValue().endTime
        );
    }

    public static void clearZone(UUID ownerId) {
        ACTIVE_ZONES.remove(ownerId);
    }

    public record ZoneData(UUID ownerId, Vec3d center, int radius, long endTime, String worldId) {}
}
