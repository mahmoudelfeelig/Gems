package com.feel.gems.power.runtime;

import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;




public final class GemAbilityCooldowns {
    private static final String KEY_COOLDOWNS = "cooldowns";

    private GemAbilityCooldowns() {
    }

    public static void clearAll(ServerPlayerEntity player) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        root.remove(KEY_COOLDOWNS);
    }

    public static long nextAllowedTick(ServerPlayerEntity player, Identifier abilityId) {
        return cooldowns(player).getLong(abilityId.toString(), 0L);
    }

    public static void setNextAllowedTick(ServerPlayerEntity player, Identifier abilityId, long nextAllowedTick) {
        cooldowns(player).putLong(abilityId.toString(), nextAllowedTick);
    }

    public static int remainingTicks(ServerPlayerEntity player, Identifier abilityId, long nowTick) {
        long nextAllowed = nextAllowedTick(player, abilityId);
        if (nextAllowed <= nowTick) {
            return 0;
        }
        long remaining = nextAllowed - nowTick;
        if (remaining > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) remaining;
    }

    private static NbtCompound cooldowns(ServerPlayerEntity player) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        NbtCompound existing = root.getCompound(KEY_COOLDOWNS).orElse(null);
        if (existing == null) {
            existing = new NbtCompound();
            root.put(KEY_COOLDOWNS, existing);
        }
        return existing;
    }
}

