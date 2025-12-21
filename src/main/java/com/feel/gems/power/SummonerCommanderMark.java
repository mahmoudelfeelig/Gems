package com.feel.gems.power;

import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public final class SummonerCommanderMark {
    private static final String KEY_UNTIL = "summonerMarkUntil";
    private static final String KEY_TARGET = "summonerMarkTarget";

    private SummonerCommanderMark() {
    }

    public static void mark(ServerPlayerEntity owner, Entity target, int durationTicks) {
        if (durationTicks <= 0) {
            return;
        }
        NbtCompound root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        root.putLong(KEY_UNTIL, GemsTime.now(owner) + durationTicks);
        root.put(KEY_TARGET, NbtHelper.fromUuid(target.getUuid()));
    }

    public static UUID activeTargetUuid(ServerPlayerEntity owner) {
        NbtCompound root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        long until = root.getLong(KEY_UNTIL);
        if (until <= GemsTime.now(owner)) {
            return null;
        }
        if (!root.contains(KEY_TARGET, NbtElement.INT_ARRAY_TYPE)) {
            return null;
        }
        return NbtHelper.toUuid(root.get(KEY_TARGET));
    }
}

