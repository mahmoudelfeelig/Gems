package com.feel.gems.legendary;

import com.feel.gems.util.GemsTime;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;




public final class LegendaryTargeting {
    private static final String KEY_LAST_TARGET = "legendaryLastTarget";
    private static final String KEY_LAST_TARGET_AT = "legendaryLastTargetAt";

    private LegendaryTargeting() {
    }

    public static void recordHit(ServerPlayerEntity attacker, ServerPlayerEntity target) {
        NbtCompound data = ((com.feel.gems.state.GemsPersistentDataHolder) attacker).gems$getPersistentData();
        data.putUuid(KEY_LAST_TARGET, target.getUuid());
        data.putLong(KEY_LAST_TARGET_AT, GemsTime.now(attacker));
    }

    public static ServerPlayerEntity findTarget(ServerPlayerEntity player, int rangeBlocks, int timeoutTicks) {
        NbtCompound data = ((com.feel.gems.state.GemsPersistentDataHolder) player).gems$getPersistentData();
        if (!data.containsUuid(KEY_LAST_TARGET)) {
            return null;
        }
        long now = GemsTime.now(player);
        long last = data.getLong(KEY_LAST_TARGET_AT);
        if (timeoutTicks > 0 && now - last > timeoutTicks) {
            return null;
        }
        java.util.UUID uuid = data.getUuid(KEY_LAST_TARGET);
        ServerPlayerEntity target = player.getServer() == null ? null : player.getServer().getPlayerManager().getPlayer(uuid);
        if (target == null || target.getWorld() != player.getWorld()) {
            return null;
        }
        double max = rangeBlocks * (double) rangeBlocks;
        if (player.squaredDistanceTo(target) > max) {
            return null;
        }
        if (!player.canSee(target)) {
            return null;
        }
        return target;
    }
}
