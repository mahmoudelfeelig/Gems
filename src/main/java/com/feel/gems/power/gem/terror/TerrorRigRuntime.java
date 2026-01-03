package com.feel.gems.power.gem.terror;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.util.GemsTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.TntEntity;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;


public final class TerrorRigRuntime {
    private static final Map<TrapKey, RiggedTrap> ACTIVE = new HashMap<>();

    private TerrorRigRuntime() {
    }

    public static boolean arm(ServerPlayerEntity player, BlockPos pos) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }
        if (world.getBlockState(pos).isAir()) {
            return false;
        }
        int duration = GemsBalance.v().terror().rigDurationTicks();
        long until = duration > 0 ? GemsTime.now(player) + duration : 0L;
        TrapKey key = new TrapKey(world.getRegistryKey().getValue(), pos.toImmutable());
        RiggedTrap existing = ACTIVE.get(key);
        if (existing != null && !existing.owner().equals(player.getUuid())) {
            return false;
        }
        ACTIVE.put(key, new RiggedTrap(player.getUuid(), until));
        return true;
    }

    public static boolean tryTriggerUse(ServerPlayerEntity player, BlockPos pos) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }
        TrapKey key = new TrapKey(world.getRegistryKey().getValue(), pos.toImmutable());
        RiggedTrap trap = ACTIVE.get(key);
        if (trap == null) {
            return false;
        }
        trigger(world, pos, key, trap);
        return true;
    }

    public static boolean tryTriggerBreak(ServerPlayerEntity player, BlockPos pos) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }
        TrapKey key = new TrapKey(world.getRegistryKey().getValue(), pos.toImmutable());
        RiggedTrap trap = ACTIVE.get(key);
        if (trap == null) {
            return false;
        }
        trigger(world, pos, key, trap);
        return true;
    }

    public static boolean tryTriggerUpdate(ServerWorld world, BlockPos pos, BlockState before, BlockState after) {
        if (ACTIVE.isEmpty()) {
            return false;
        }
        if (before == after || before.equals(after)) {
            return false;
        }
        TrapKey key = new TrapKey(world.getRegistryKey().getValue(), pos.toImmutable());
        RiggedTrap trap = ACTIVE.get(key);
        if (trap == null) {
            return false;
        }
        trigger(world, pos, key, trap);
        return true;
    }

    public static boolean hasTrap(ServerWorld world, BlockPos pos) {
        if (ACTIVE.isEmpty()) {
            return false;
        }
        TrapKey key = new TrapKey(world.getRegistryKey().getValue(), pos.toImmutable());
        return ACTIVE.containsKey(key);
    }

    public static void checkStep(ServerPlayerEntity player) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        BlockPos below = player.getBlockPos().down();
        TrapKey key = new TrapKey(world.getRegistryKey().getValue(), below);
        RiggedTrap trap = ACTIVE.get(key);
        if (trap == null) {
            return;
        }
        trigger(world, below, key, trap);
    }

    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        long now = GemsTime.now(server);
        Iterator<Map.Entry<TrapKey, RiggedTrap>> iter = ACTIVE.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<TrapKey, RiggedTrap> entry = iter.next();
            RiggedTrap trap = entry.getValue();
            if (trap.until() > 0 && trap.until() <= now) {
                iter.remove();
            }
        }
    }

    private static void trigger(ServerWorld world, BlockPos pos, TrapKey key, RiggedTrap trap) {
        ACTIVE.remove(key);
        int fuse = GemsBalance.v().terror().rigFuseTicks();
        int tntCount = GemsBalance.v().terror().rigTntCount();
        for (int i = 0; i < tntCount; i++) {
            TntEntity tnt = new TntEntity(world, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, null);
            tnt.setFuse(fuse);
            world.spawnEntity(tnt);
        }
    }

    private record TrapKey(Identifier dimension, BlockPos pos) {
    }

    private record RiggedTrap(UUID owner, long until) {
    }
}
