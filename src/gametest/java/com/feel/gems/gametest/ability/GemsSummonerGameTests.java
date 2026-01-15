package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.summoner.*;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Vec3d;

public final class GemsSummonerGameTests {

    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void summonerSummonSlotActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SUMMONER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            int before = world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(12.0D), SummonerSummons::isSummon).size();
            boolean ok = new SummonSlotAbility(1).activate(player);
            if (!ok) {
                context.throwGameTestException("Summon Slot did not activate");
            }
            int after = world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(12.0D), SummonerSummons::isSummon).size();
            if (after <= before) {
                context.throwGameTestException("Summon Slot should spawn summons");
            }
            MobEntity base = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
            MobEntity spawned = world.getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(12.0D), SummonerSummons::isSummon)
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (base != null && spawned != null) {
                float bonus = GemsBalance.v().summoner().summonBonusHealth();
                if (bonus > 0.0F && spawned.getMaxHealth() <= base.getMaxHealth()) {
                    context.throwGameTestException("Familiar's Blessing should increase summon max health");
                }
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void summonerRecallActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SUMMONER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new SummonSlotAbility(1).activate(player);
            if (!ok) {
                context.throwGameTestException("Summon Slot did not activate");
            }
        });

        context.runAtTick(20L, () -> {
            boolean ok = new SummonRecallAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Summon Recall did not activate");
            }
            int remaining = SummonerSummons.pruneAndCount(player);
            if (remaining > 0) {
                context.throwGameTestException("Summon Recall should despawn summons");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void summonerConfigValuesValid(TestContext context) {
        var cfg = GemsBalance.v().summoner();
        
        if (cfg.summonSlotCooldownTicks() < 0) {
            context.throwGameTestException("Summon Slot cooldown cannot be negative");
        }
        if (cfg.recallCooldownTicks() < 0) {
            context.throwGameTestException("Recall cooldown cannot be negative");
        }
        if (cfg.maxActiveSummons() <= 0) {
            context.throwGameTestException("Max summons must be positive");
        }
        
        context.complete();
    }
}
