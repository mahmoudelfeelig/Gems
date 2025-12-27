package com.feel.gems.power.runtime;

import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;




public final class GemAbilityCooldowns {
    private static final String KEY_COOLDOWNS = "cooldowns";

    private GemAbilityCooldowns() {
    }

    public static long nextAllowedTick(ServerPlayerEntity player, Identifier abilityId) {
        return cooldowns(player).getLong(abilityId.toString());
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
        if (!root.contains(KEY_COOLDOWNS, NbtElement.COMPOUND_TYPE)) {
            root.put(KEY_COOLDOWNS, new NbtCompound());
        }
        return root.getCompound(KEY_COOLDOWNS);
    }
}

