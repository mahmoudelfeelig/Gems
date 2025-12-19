package com.blissmc.gems.power;

import com.blissmc.gems.state.GemsPersistentDataHolder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WealthFumble {
    private static final String KEY_UNTIL = "wealthFumbleUntil";

    private WealthFumble() {
    }

    public static void apply(ServerPlayerEntity target, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        persistent(target).putLong(KEY_UNTIL, target.getServerWorld().getTime() + durationTicks);
    }

    public static boolean isActive(ServerPlayerEntity player) {
        long now = player.getServerWorld().getTime();
        return until(player) > now;
    }

    public static long until(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        if (!nbt.contains(KEY_UNTIL, NbtElement.LONG_TYPE)) {
            return 0L;
        }
        return nbt.getLong(KEY_UNTIL);
    }

    private static NbtCompound persistent(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}

