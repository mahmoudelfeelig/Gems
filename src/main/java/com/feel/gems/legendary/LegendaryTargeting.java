package com.feel.gems.legendary;

import com.feel.gems.util.GemsTime;
import com.feel.gems.util.GemsNbt;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;




public final class LegendaryTargeting {
    private static final String KEY_LAST_TARGET = "legendaryLastTarget";
    private static final String KEY_LAST_TARGET_AT = "legendaryLastTargetAt";

    private LegendaryTargeting() {
    }

    public static void recordHit(ServerPlayerEntity attacker, LivingEntity target) {
        NbtCompound data = ((com.feel.gems.state.GemsPersistentDataHolder) attacker).gems$getPersistentData();
        GemsNbt.putUuid(data, KEY_LAST_TARGET, target.getUuid());
        data.putLong(KEY_LAST_TARGET_AT, GemsTime.now(attacker));
    }

    public static LivingEntity findTarget(ServerPlayerEntity player, int rangeBlocks, int timeoutTicks) {
        NbtCompound data = ((com.feel.gems.state.GemsPersistentDataHolder) player).gems$getPersistentData();
        if (!GemsNbt.containsUuid(data, KEY_LAST_TARGET)) {
            return null;
        }
        long now = GemsTime.now(player);
        long last = data.getLong(KEY_LAST_TARGET_AT, 0L);
        if (timeoutTicks > 0 && now - last > timeoutTicks) {
            return null;
        }
        UUID uuid = GemsNbt.getUuid(data, KEY_LAST_TARGET);
        LivingEntity target = player.getEntityWorld().getServer() == null ? null : findLiving(player.getEntityWorld().getServer(), uuid);
        if (target == null || target.getEntityWorld() != player.getEntityWorld() || !target.isAlive()) {
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

    private static LivingEntity findLiving(MinecraftServer server, UUID uuid) {
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }
}
