package com.feel.gems.power;

import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;

public final class AbilityRestrictions {
    private static final String KEY_SUPPRESSED_UNTIL = "abilitySuppressedUntil";
    private static final String KEY_STUNNED_UNTIL = "abilityStunnedUntil";

    private AbilityRestrictions() {
    }

    public static boolean isSuppressed(ServerPlayerEntity player) {
        long now = player.getServerWorld().getTime();
        return suppressedUntil(player) > now;
    }

    public static boolean isStunned(ServerPlayerEntity player) {
        long now = player.getServerWorld().getTime();
        return stunnedUntil(player) > now;
    }

    public static void suppress(ServerPlayerEntity player, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        persistent(player).putLong(KEY_SUPPRESSED_UNTIL, player.getServerWorld().getTime() + durationTicks);
    }

    public static void stun(ServerPlayerEntity player, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        persistent(player).putLong(KEY_STUNNED_UNTIL, player.getServerWorld().getTime() + durationTicks);
    }

    public static long suppressedUntil(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        if (!nbt.contains(KEY_SUPPRESSED_UNTIL, NbtElement.LONG_TYPE)) {
            return 0L;
        }
        return nbt.getLong(KEY_SUPPRESSED_UNTIL);
    }

    public static long stunnedUntil(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        if (!nbt.contains(KEY_STUNNED_UNTIL, NbtElement.LONG_TYPE)) {
            return 0L;
        }
        return nbt.getLong(KEY_STUNNED_UNTIL);
    }

    private static NbtCompound persistent(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}

