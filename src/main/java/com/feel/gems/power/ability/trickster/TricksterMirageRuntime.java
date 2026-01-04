package com.feel.gems.power.ability.trickster;

import com.feel.gems.state.PlayerStateManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.util.*;

public final class TricksterMirageRuntime {
    private static final String MIRAGE_COUNT_KEY = "trickster_mirage_count";
    private static final String MIRAGE_END_KEY = "trickster_mirage_end";
    private static final String MIRAGE_BASE_X_KEY = "trickster_mirage_base_x";
    private static final String MIRAGE_BASE_Z_KEY = "trickster_mirage_base_z";

    private TricksterMirageRuntime() {}

    public static void createMirages(ServerPlayerEntity player, Vec3d center, int count, long endTime) {
        PlayerStateManager.setPersistent(player, MIRAGE_COUNT_KEY, String.valueOf(count));
        PlayerStateManager.setPersistent(player, MIRAGE_END_KEY, String.valueOf(endTime));
        PlayerStateManager.setPersistent(player, MIRAGE_BASE_X_KEY, String.valueOf(center.x));
        PlayerStateManager.setPersistent(player, MIRAGE_BASE_Z_KEY, String.valueOf(center.z));
    }

    public static int getMirageCount(ServerPlayerEntity player) {
        String endStr = PlayerStateManager.getPersistent(player, MIRAGE_END_KEY);
        if (endStr == null) return 0;

        long endTime = Long.parseLong(endStr);
        if (player.getEntityWorld().getTime() > endTime) {
            clearMirages(player);
            return 0;
        }

        String countStr = PlayerStateManager.getPersistent(player, MIRAGE_COUNT_KEY);
        return countStr != null ? Integer.parseInt(countStr) : 0;
    }

    public static List<Vec3d> getMiragePositions(ServerPlayerEntity player) {
        int count = getMirageCount(player);
        if (count == 0) return Collections.emptyList();

        Vec3d playerPos = player.getEntityPos();
        List<Vec3d> positions = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            double offsetX = Math.cos(angle) * 2.0;
            double offsetZ = Math.sin(angle) * 2.0;
            positions.add(playerPos.add(offsetX, 0, offsetZ));
        }

        return positions;
    }

    public static void destroyOneMirage(ServerPlayerEntity player) {
        int count = getMirageCount(player);
        if (count <= 0) return;

        count--;
        if (count == 0) {
            clearMirages(player);
        } else {
            PlayerStateManager.setPersistent(player, MIRAGE_COUNT_KEY, String.valueOf(count));
        }
    }

    public static boolean hasMirages(ServerPlayerEntity player) {
        return getMirageCount(player) > 0;
    }

    public static void clearMirages(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, MIRAGE_COUNT_KEY);
        PlayerStateManager.clearPersistent(player, MIRAGE_END_KEY);
        PlayerStateManager.clearPersistent(player, MIRAGE_BASE_X_KEY);
        PlayerStateManager.clearPersistent(player, MIRAGE_BASE_Z_KEY);
    }
}
