package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class SpeedFrictionlessSteps {
    private SpeedFrictionlessSteps() {
    }

    public static void tick(MinecraftServer server) {
        if ((server.getTicks() % 5) != 0) {
            return; // run every 5 ticks to cut per-tick scans
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            tickPlayer(player);
        }
    }

    private static void tickPlayer(ServerPlayerEntity player) {
        if (!GemPowers.isPassiveActive(player, PowerIds.SPEED_FRICTIONLESS)) {
            return;
        }
        if (!isOnFrictionBlock(player)) {
            return;
        }
        int amp = GemsBalance.v().speed().frictionlessSpeedAmplifier();
        if (amp < 0) {
            return;
        }
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 10, amp, true, false, false));
    }

    private static boolean isOnFrictionBlock(ServerPlayerEntity player) {
        BlockState state = player.getWorld().getBlockState(player.getBlockPos());
        if (isFrictionBlock(state)) {
            return true;
        }
        BlockState below = player.getWorld().getBlockState(player.getBlockPos().down());
        return isFrictionBlock(below);
    }

    private static boolean isFrictionBlock(BlockState state) {
        return state.isOf(Blocks.COBWEB)
                || state.isOf(Blocks.HONEY_BLOCK)
                || state.isOf(Blocks.POWDER_SNOW);
    }
}
