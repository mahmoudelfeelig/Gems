package com.feel.gems.gametest;

import com.feel.gems.item.ModItems;
import com.feel.gems.power.AbilityRuntime;
import com.feel.gems.power.FluxBeamAbility;
import com.feel.gems.power.FluxCharge;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameMode;

public final class GemsGameTests {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void astralCameraReturnsToStart(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockPlayer(GameMode.SURVIVAL);

        BlockPos start = context.getAbsolute(new BlockPos(0, 2, 0));
        player.teleport(world, start.getX() + 0.5D, start.getY(), start.getZ() + 0.5D, 0.0F, 0.0F);

        AbilityRuntime.startAstralCamera(player, 20);

        BlockPos moved = start.add(6, 0, 0);
        player.teleport(world, moved.getX() + 0.5D, moved.getY(), moved.getZ() + 0.5D, 90.0F, 0.0F);

        long checkTick = world.getTime() + 60L;
        context.runAtTick(checkTick, () -> {
            if (!player.getBlockPos().equals(start)) {
                context.throwGameTestException("Astral Camera did not return player to start position");
            }
            if (player.interactionManager.getGameMode() != GameMode.SURVIVAL) {
                context.throwGameTestException("Astral Camera did not restore original gamemode");
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void fluxBeamConsumesCharge(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockPlayer(GameMode.SURVIVAL);

        BlockPos start = context.getAbsolute(new BlockPos(0, 2, 0));
        player.teleport(world, start.getX() + 0.5D, start.getY(), start.getZ() + 0.5D, 0.0F, 0.0F);

        // Place a target in front of the player (yaw 0 faces +Z).
        context.spawnEntity(EntityType.COW, 0.5F, 2.0F, 5.5F);
        FluxCharge.set(player, 100);

        long runTick = world.getTime() + 2L;
        context.runAtTick(runTick, () -> {
            boolean activated = new FluxBeamAbility().activate(player);
            if (!activated) {
                context.throwGameTestException("Flux Beam did not activate (target not acquired)");
            }
            if (FluxCharge.get(player) != 0) {
                context.throwGameTestException("Flux Beam did not reset flux charge back to 0");
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void gemsDoNotDropOnDeath(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockPlayer(GameMode.SURVIVAL);

        BlockPos start = context.getAbsolute(new BlockPos(0, 2, 0));
        player.teleport(world, start.getX() + 0.5D, start.getY(), start.getZ() + 0.5D, 0.0F, 0.0F);

        player.giveItemStack(new ItemStack(ModItems.ASTRA_GEM));

        player.damage(player.getDamageSources().generic(), 10_000.0F);

        long checkTick = world.getTime() + 2L;
        context.runAtTick(checkTick, () -> {
            Box box = new Box(start).expand(12.0D);
            boolean foundGemDrop = !world.getEntitiesByType(EntityType.ITEM, box, item -> item.getStack().isOf(ModItems.ASTRA_GEM)).isEmpty();

            if (foundGemDrop) {
                context.throwGameTestException("Gem item dropped on death (should be kept)");
            }
            context.complete();
        });
    }
}
