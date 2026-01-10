package com.feel.gems.power.gem.wealth;

import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;


public final class HotbarLock {
    private static final String KEY_LOCK_UNTIL = "hotbarLockUntil";
    private static final String KEY_LOCK_SLOT = "hotbarLockSlot";

    private HotbarLock() {
    }

    public static void lock(ServerPlayerEntity target, int hotbarSlot, int durationTicks) {
        lock(persistent(target), GemsTime.now(target), hotbarSlot, durationTicks);
    }

    public static boolean isLocked(ServerPlayerEntity player) {
        return lockedSlot(player) >= 0;
    }

    public static int lockedSlot(ServerPlayerEntity player) {
        return lockedSlot(persistent(player), GemsTime.now(player));
    }

    static void lock(NbtCompound nbt, long now, int hotbarSlot, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        int slot = clamp(hotbarSlot, 0, 8);
        nbt.putLong(KEY_LOCK_UNTIL, now + durationTicks);
        nbt.putInt(KEY_LOCK_SLOT, slot);
    }

    static int lockedSlot(NbtCompound nbt, long now) {
        long until = nbt.getLong(KEY_LOCK_UNTIL, 0L);
        if (until <= now) {
            if (until != 0L) {
                nbt.remove(KEY_LOCK_UNTIL);
                nbt.remove(KEY_LOCK_SLOT);
            }
            return -1;
        }
        int slot = nbt.getInt(KEY_LOCK_SLOT, -1);
        if (slot < 0) {
            return -1;
        }
        return clamp(slot, 0, 8);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static NbtCompound persistent(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
