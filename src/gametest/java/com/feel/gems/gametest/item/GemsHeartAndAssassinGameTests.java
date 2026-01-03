package com.feel.gems.gametest.item;

import com.feel.gems.assassin.AssassinState;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.event.GemsPlayerDeath;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.item.ModItems;
import com.feel.gems.state.GemPlayerState;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;




public final class GemsHeartAndAssassinGameTests {
    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void assassinConversionAndHeartsApplied(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        AssassinState.initIfNeeded(player);

        // Force conversion using the same persistent path the death mixin uses.
        AssassinState.becomeAssassin(player);
        AssassinState.addAssassinHearts(player, -2); // 8 hearts
        GemPlayerState.applyMaxHearts(player);

        context.runAtTick(2L, () -> {
            if (!AssassinState.isAssassin(player)) {
                context.throwGameTestException("Expected player to be assassin after becomeAssassin");
            }
            if (AssassinState.getAssassinHearts(player) != 8) {
                context.throwGameTestException("Expected assassin hearts to be 8, got " + AssassinState.getAssassinHearts(player));
            }
            double maxHealth = player.getAttributeValue(EntityAttributes.MAX_HEALTH);
            if (Math.abs(maxHealth - 16.0D) > 0.01D) {
                context.throwGameTestException("Expected max health 16.0 for 8 hearts, got " + maxHealth);
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 160)
    public void heartDropsAboveFloorAndStopsAtFive(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity killer = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity victim = GemsGameTestUtil.createMockCreativeServerPlayer(context);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(killer, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(victim, world, pos.x + 1.0D, pos.y, pos.z, 180.0F, 0.0F);

        context.runAtTick(1L, () -> {
            GemsGameTestUtil.forceSurvival(killer);
            GemsGameTestUtil.forceSurvival(victim);
            teleport(killer, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
            teleport(victim, world, pos.x + 1.0D, pos.y, pos.z, 180.0F, 0.0F);

            GemPlayerState.initIfNeeded(killer);
            GemPlayerState.initIfNeeded(victim);
            GemPlayerState.setActiveGem(killer, GemId.FIRE);
            GemPlayerState.setActiveGem(victim, GemId.LIFE);
            GemsGameTestUtil.resetAssassinState(victim);
            int minHearts = GemPlayerState.minMaxHearts();
            GemPlayerState.setMaxHearts(victim, minHearts + 1);
            GemPlayerState.applyMaxHearts(victim);
        });

        context.runAtTick(5L, () -> GemsPlayerDeath.onDeathTail(victim, victim.getDamageSources().playerAttack(killer)));

        context.runAtTick(40L, () -> {
            int heartsDropped = world.getEntitiesByClass(ItemEntity.class, new Box(pos, pos.add(1.0D, 1.0D, 1.0D)).expand(3.0D), e -> e.getStack().isOf(ModItems.HEART)).size();
	            int invHearts = GemsGameTestUtil.countItem(killer, ModItems.HEART);
	            var victimDataAfter = ((com.feel.gems.state.GemsPersistentDataHolder) victim).gems$getPersistentData();
	            if (heartsDropped + invHearts != 1) {
	                context.throwGameTestException("Expected exactly one heart gained (dropped or picked up), found dropped=" + heartsDropped + " inv=" + invHearts + " assassin=" + victimDataAfter.getBoolean("assassinIsAssassin", false) + " storedMax=" + victimDataAfter.getInt("maxHearts", 0) + " assassinHearts=" + victimDataAfter.getInt("assassinHearts", 0));
	                return;
	            }
            int storedHearts = victimDataAfter.getInt("maxHearts", 0);
            if (storedHearts != GemPlayerState.minMaxHearts()) {
                context.throwGameTestException("Victim hearts should clamp to floor after drop: " + storedHearts);
                return;
            }
            boolean becameAssassin = victimDataAfter.getBoolean("assassinIsAssassin", false);
            if (becameAssassin) {
                context.throwGameTestException("Victim above floor should not convert to assassin");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 160)
    public void heartAtFloorDoesNotDropAndTurnsAssassin(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity killer = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity victim = GemsGameTestUtil.createMockCreativeServerPlayer(context);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(killer, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(victim, world, pos.x + 1.0D, pos.y, pos.z, 180.0F, 0.0F);

        context.runAtTick(1L, () -> {
            GemsGameTestUtil.forceSurvival(killer);
            GemsGameTestUtil.forceSurvival(victim);
            teleport(killer, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
            teleport(victim, world, pos.x + 1.0D, pos.y, pos.z, 180.0F, 0.0F);

            GemPlayerState.initIfNeeded(killer);
            GemPlayerState.initIfNeeded(victim);
            GemPlayerState.setActiveGem(killer, GemId.FIRE);
            GemPlayerState.setActiveGem(victim, GemId.LIFE);
            GemsGameTestUtil.resetAssassinState(victim);
            int minHearts = GemPlayerState.minMaxHearts();
            GemPlayerState.setMaxHearts(victim, minHearts);
            GemPlayerState.applyMaxHearts(victim);
        });

        context.runAtTick(5L, () -> GemsPlayerDeath.onDeathTail(victim, victim.getDamageSources().playerAttack(killer)));

        context.runAtTick(40L, () -> {
            int heartsDropped = world.getEntitiesByClass(ItemEntity.class, new Box(pos, pos.add(1.0D, 1.0D, 1.0D)).expand(3.0D), e -> e.getStack().isOf(ModItems.HEART)).size();
            var victimDataAfter = ((com.feel.gems.state.GemsPersistentDataHolder) victim).gems$getPersistentData();
            if (heartsDropped != 0) {
                context.throwGameTestException("No hearts should drop at the floor (" + GemPlayerState.minMaxHearts() + " hearts)");
                return;
            }
            int invHearts = GemsGameTestUtil.countItem(killer, ModItems.HEART);
            if (invHearts != 0) {
                context.throwGameTestException("Killer should not receive a heart when victim at floor");
                return;
            }
            boolean becameAssassin = victimDataAfter.getBoolean("assassinIsAssassin", false);
	            int triggerHearts = Math.max(GemPlayerState.minMaxHearts(), GemsBalance.v().systems().assassinTriggerHearts());
	            if (!becameAssassin) {
	                context.throwGameTestException("Victim at " + triggerHearts + " hearts should be converted to assassin on death (storedMax=" + victimDataAfter.getInt("maxHearts", 0) + " assassinHearts=" + victimDataAfter.getInt("assassinHearts", 0) + ")");
	                return;
	            }
            context.complete();
        });
    }

}

