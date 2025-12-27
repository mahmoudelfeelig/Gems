package com.feel.gems.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;




/**
 * Canonical server-side tick time source for durations/cooldowns.
 *
 * <p>Uses the overworld's game time as a single monotonic reference shared across dimensions,
 * so timers don't drift when players change dimensions.</p>
 */
public final class GemsTime {
    private GemsTime() {
    }

    public static long now(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return player.getServerWorld().getTime();
        }
        return now(server);
    }

    public static long now(ServerWorld world) {
        MinecraftServer server = world.getServer();
        if (server == null) {
            return world.getTime();
        }
        return now(server);
    }

    public static long now(MinecraftServer server) {
        return server.getOverworld().getTime();
    }
}

