package com.feel.gems.power.gem.terror;

import com.feel.gems.admin.GemsAdmin;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.net.GemCooldownSync;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemAbilityCooldowns;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.minecraft.entity.TntEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class TerrorRemoteChargeRuntime {
    private static final String KEY_ARM_UNTIL = "terrorRemoteArmUntil";
    private static final String KEY_CHARGE_UNTIL = "terrorRemoteChargeUntil";
    private static final String KEY_CHARGE_POS = "terrorRemoteChargePos";
    private static final String KEY_CHARGE_DIM = "terrorRemoteChargeDim";
    private static final String KEY_COOLDOWN_UNTIL = "terrorRemoteCooldownUntil";

    private TerrorRemoteChargeRuntime() {
    }

    public static boolean startArming(ServerPlayerEntity player) {
        int window = GemsBalance.v().terror().remoteChargeArmWindowTicks();
        if (window <= 0) {
            return false;
        }
        NbtCompound nbt = persistent(player);
        clearExpired(player, nbt);
        // Skip cooldown check if admin has no-cooldowns enabled
        if (!GemsAdmin.noCooldowns(player) && isOnCooldown(player, nbt)) {
            return false;
        }
        if (hasActiveCharge(player, nbt)) {
            return false;
        }
        nbt.putLong(KEY_ARM_UNTIL, GemsTime.now(player) + window);
        return true;
    }

    public static boolean tryArm(ServerPlayerEntity player, BlockPos pos) {
        NbtCompound nbt = persistent(player);
        clearExpired(player, nbt);
        if (!isArming(player, nbt)) {
            return false;
        }
        if (!(player.getEntityWorld() instanceof ServerWorld playerWorld)) {
            return false;
        }
        if (playerWorld.getBlockState(pos).isAir()) {
            return false;
        }
        int detonateWindow = GemsBalance.v().terror().remoteChargeDetonateWindowTicks();
        if (detonateWindow <= 0) {
            return false;
        }
        nbt.putLong(KEY_CHARGE_UNTIL, GemsTime.now(player) + detonateWindow);
        nbt.putLong(KEY_CHARGE_POS, pos.asLong());
        nbt.putString(KEY_CHARGE_DIM, playerWorld.getRegistryKey().getValue().toString());
        nbt.remove(KEY_ARM_UNTIL);
        return true;
    }

    public static boolean detonate(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        clearExpired(player, nbt);
        if (!hasActiveCharge(player, nbt)) {
            return false;
        }
        BlockPos pos = readChargePos(nbt);
        String dimRaw = nbt.getString(KEY_CHARGE_DIM, "");
        if (pos == null || dimRaw == null || dimRaw.isBlank()) {
            clearCharge(nbt);
            return false;
        }
        Identifier dimId = Identifier.tryParse(dimRaw);
        if (dimId == null) {
            clearCharge(nbt);
            return false;
        }
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            clearCharge(nbt);
            return false;
        }
        var key = net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.WORLD, dimId);
        ServerWorld world = server.getWorld(key);
        if (world == null) {
            clearCharge(nbt);
            return false;
        }
        int fuse = Math.max(1, GemsBalance.v().terror().remoteChargeFuseTicks());
        TntEntity tnt = new TntEntity(world, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, player);
        tnt.setFuse(fuse);
        world.spawnEntity(tnt);
        clearCharge(nbt);
        startCooldown(player, nbt);
        return true;
    }

    public static boolean isArming(ServerPlayerEntity player) {
        return isArming(player, persistent(player));
    }

    public static boolean hasActiveCharge(ServerPlayerEntity player) {
        return hasActiveCharge(player, persistent(player));
    }

    public static boolean isOnCooldown(ServerPlayerEntity player) {
        return isOnCooldown(player, persistent(player));
    }

    private static boolean isArming(ServerPlayerEntity player, NbtCompound nbt) {
        long until = nbt.getLong(KEY_ARM_UNTIL, 0L);
        if (until <= 0) {
            return false;
        }
        long now = GemsTime.now(player);
        if (now >= until) {
            nbt.remove(KEY_ARM_UNTIL);
            return false;
        }
        return true;
    }

    private static boolean hasActiveCharge(ServerPlayerEntity player, NbtCompound nbt) {
        long until = nbt.getLong(KEY_CHARGE_UNTIL, 0L);
        if (until <= 0) {
            return false;
        }
        long now = GemsTime.now(player);
        if (now >= until) {
            clearCharge(nbt);
            return false;
        }
        if (!nbt.contains(KEY_CHARGE_POS)) {
            clearCharge(nbt);
            return false;
        }
        if (!nbt.contains(KEY_CHARGE_DIM)) {
            clearCharge(nbt);
            return false;
        }
        return true;
    }

    private static boolean isOnCooldown(ServerPlayerEntity player, NbtCompound nbt) {
        long until = nbt.getLong(KEY_COOLDOWN_UNTIL, 0L);
        if (until <= 0) {
            return false;
        }
        long now = GemsTime.now(player);
        if (now >= until) {
            nbt.remove(KEY_COOLDOWN_UNTIL);
            return false;
        }
        return true;
    }

    private static void clearExpired(ServerPlayerEntity player, NbtCompound nbt) {
        long now = GemsTime.now(player);
        long armUntil = nbt.getLong(KEY_ARM_UNTIL, 0L);
        if (armUntil > 0 && now >= armUntil) {
            nbt.remove(KEY_ARM_UNTIL);
        }
        long chargeUntil = nbt.getLong(KEY_CHARGE_UNTIL, 0L);
        if (chargeUntil > 0 && now >= chargeUntil) {
            clearCharge(nbt);
        }
        long cooldownUntil = nbt.getLong(KEY_COOLDOWN_UNTIL, 0L);
        if (cooldownUntil > 0 && now >= cooldownUntil) {
            nbt.remove(KEY_COOLDOWN_UNTIL);
        }
    }

    private static void clearCharge(NbtCompound nbt) {
        nbt.remove(KEY_CHARGE_UNTIL);
        nbt.remove(KEY_CHARGE_POS);
        nbt.remove(KEY_CHARGE_DIM);
    }

    private static void startCooldown(ServerPlayerEntity player, NbtCompound nbt) {
        // Skip cooldown if admin has no-cooldowns enabled
        if (GemsAdmin.noCooldowns(player)) {
            return;
        }
        int cooldown = GemsBalance.v().terror().remoteChargeCooldownTicks();
        if (cooldown > 0) {
            long until = GemsTime.now(player) + cooldown;
            nbt.putLong(KEY_COOLDOWN_UNTIL, until);
            GemAbilityCooldowns.setNextAllowedTick(player, PowerIds.TERROR_REMOTE_CHARGE, until);
            GemCooldownSync.send(player);
        }
    }

    private static BlockPos readChargePos(NbtCompound nbt) {
        long packed = nbt.getLong(KEY_CHARGE_POS, Long.MIN_VALUE);
        if (packed == Long.MIN_VALUE) {
            return null;
        }
        return BlockPos.fromLong(packed);
    }

    private static NbtCompound persistent(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
