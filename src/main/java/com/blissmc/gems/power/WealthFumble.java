package com.feel.gems.power;

import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;

public final class WealthFumble {
    private static final String KEY_UNTIL = "wealthFumbleUntil";

    private WealthFumble() {
    }

    public static void apply(ServerPlayerEntity target, int durationTicks) {
        apply(persistent(target), target.getServerWorld().getTime(), durationTicks);
    }

    public static boolean isActive(ServerPlayerEntity player) {
        long now = player.getServerWorld().getTime();
        return until(persistent(player), now) > now;
    }

    public static long until(ServerPlayerEntity player) {
        return until(persistent(player), player.getServerWorld().getTime());
    }

    static void apply(NbtCompound nbt, long now, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        nbt.putLong(KEY_UNTIL, now + durationTicks);
    }

    static long until(NbtCompound nbt, long now) {
        if (!nbt.contains(KEY_UNTIL, NbtElement.LONG_TYPE)) {
            return 0L;
        }
        long until = nbt.getLong(KEY_UNTIL);
        if (until <= now) {
            nbt.remove(KEY_UNTIL);
            return 0L;
        }
        return until;
    }

    private static NbtCompound persistent(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
