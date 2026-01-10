package com.feel.gems.power.gem.summoner;

import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsNbt;
import com.feel.gems.util.GemsTime;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;


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
        GemsNbt.putUuid(root, KEY_TARGET, target.getUuid());
    }

    public static UUID activeTargetUuid(ServerPlayerEntity owner) {
        NbtCompound root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        long until = root.getLong(KEY_UNTIL, 0L);
        if (until <= GemsTime.now(owner)) {
            return null;
        }
        return GemsNbt.getUuid(root, KEY_TARGET);
    }
}

