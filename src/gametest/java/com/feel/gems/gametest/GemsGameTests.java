package com.feel.gems.gametest;

import com.feel.gems.item.ModItems;
import com.feel.gems.power.AbilityRuntime;
import com.feel.gems.power.FluxBeamAbility;
import com.feel.gems.power.FluxCharge;
import com.feel.gems.core.GemId;
import com.feel.gems.state.GemPlayerState;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameMode;

public final class GemsGameTests {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void astralCameraReturnsToStart(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        BlockPos startBlock = BlockPos.ofFloored(startPos);
        player.teleport(world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        AbilityRuntime.startAstralCamera(player, 20);

        Vec3d movedPos = context.getAbsolute(new Vec3d(6.5D, 2.0D, 0.5D));
        player.teleport(world, movedPos.x, movedPos.y, movedPos.z, 90.0F, 0.0F);

        context.runAtTick(80L, () -> {
            if (!player.getBlockPos().equals(startBlock)) {
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
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        player.teleport(world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);
        player.setYaw(0.0F);
        player.setPitch(0.0F);

        // Place a target in front of the player (yaw 0 faces +Z).
        context.spawnEntity(EntityType.COW, 0.5F, 3.0F, 5.5F);
        FluxCharge.set(player, 100);

        context.runAtTick(2L, () -> {
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
    public void deathKeepsActiveGemOnly(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        BlockPos startBlock = BlockPos.ofFloored(startPos);
        player.teleport(world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.ASTRA);
        player.giveItemStack(new ItemStack(ModItems.ASTRA_GEM));
        player.giveItemStack(new ItemStack(ModItems.FIRE_GEM));

        player.damage(player.getDamageSources().generic(), 10_000.0F);

        context.runAtTick(5L, () -> {
            Box box = new Box(startBlock).expand(12.0D);
            boolean foundActiveDrop = !world.getEntitiesByType(EntityType.ITEM, box, item -> item.getStack().isOf(ModItems.ASTRA_GEM)).isEmpty();
            boolean foundOtherDrop = !world.getEntitiesByType(EntityType.ITEM, box, item -> item.getStack().isOf(ModItems.FIRE_GEM)).isEmpty();

            if (foundActiveDrop) {
                context.throwGameTestException("Active gem dropped on death (should be kept)");
            }
            if (!foundOtherDrop) {
                context.throwGameTestException("Non-active gem did not drop on death (should drop)");
            }
            context.complete();
        });
    }
}
