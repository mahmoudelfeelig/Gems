package com.feel.gems.power.ability.sentinel;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import java.util.*;

public final class SentinelShieldWallRuntime {
    private static final Map<UUID, WallData> ACTIVE_WALLS = new HashMap<>();

    private SentinelShieldWallRuntime() {}

    public static void createWall(ServerPlayerEntity owner, BlockPos basePos, Direction perpendicular, int width, int height, long endTime) {
        Set<BlockPos> wallPositions = new HashSet<>();
        for (int w = -width / 2; w <= width / 2; w++) {
            for (int h = 0; h < height; h++) {
                wallPositions.add(basePos.offset(perpendicular, w).up(h));
            }
        }

        ACTIVE_WALLS.put(owner.getUuid(), new WallData(owner.getUuid(), wallPositions, endTime, owner.getEntityWorld().getRegistryKey().getValue().toString()));
    }

    public static boolean isInWall(Vec3d position, String worldId) {
        BlockPos blockPos = BlockPos.ofFloored(position);
        for (WallData wall : ACTIVE_WALLS.values()) {
            if (!wall.worldId.equals(worldId)) continue;
            if (wall.positions.contains(blockPos)) {
                return true;
            }
        }
        return false;
    }

    public static UUID getWallOwner(Vec3d position, String worldId) {
        BlockPos blockPos = BlockPos.ofFloored(position);
        for (WallData wall : ACTIVE_WALLS.values()) {
            if (!wall.worldId.equals(worldId)) continue;
            if (wall.positions.contains(blockPos)) {
                return wall.ownerId;
            }
        }
        return null;
    }

    public static void tick(long currentTime, String worldId) {
        ACTIVE_WALLS.entrySet().removeIf(entry -> 
            entry.getValue().worldId.equals(worldId) && currentTime > entry.getValue().endTime
        );
    }

    public static void clearWall(UUID ownerId) {
        ACTIVE_WALLS.remove(ownerId);
    }

    public record WallData(UUID ownerId, Set<BlockPos> positions, long endTime, String worldId) {}
}
