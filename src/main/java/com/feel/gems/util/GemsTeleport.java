package com.feel.gems.util;

import java.util.Set;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class GemsTeleport {
    private GemsTeleport() {
    }

    public static boolean teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        return player.teleport(world, x, y, z, Set.<PositionFlag>of(), yaw, pitch, true);
    }
}
