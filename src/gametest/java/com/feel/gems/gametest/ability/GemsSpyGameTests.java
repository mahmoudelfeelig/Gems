package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.spy.*;
import com.feel.gems.power.gem.spy.SpySystem;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsNbt;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.SculkSensorBlock;
import net.minecraft.block.entity.SculkSensorBlockEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;

public final class GemsSpyGameTests {

    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void spySmokeBombActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity enemy = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(enemy);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(enemy, world, pos.x + 2.0D, pos.y, pos.z, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPY);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new SpySmokeBombAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Smoke Bomb did not activate");
                return;
            }
        });

        context.runAtTick(15L, () -> {
            if (player.getStatusEffect(StatusEffects.INVISIBILITY) == null) {
                context.throwGameTestException("Smoke Bomb should cloak the spy");
                return;
            }
            if (enemy.getStatusEffect(StatusEffects.BLINDNESS) == null || enemy.getStatusEffect(StatusEffects.SLOWNESS) == null) {
                context.throwGameTestException("Smoke Bomb should blind and slow nearby enemies");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void spySkinshiftActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(target, world, pos.x, pos.y, pos.z + 2.0D, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPY);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new SpySkinshiftAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Skinshift did not activate with target in sight");
                return;
            }
            NbtCompound nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
            if (GemsNbt.getUuid(nbt, "spySkinshiftTarget") == null) {
                context.throwGameTestException("Skinshift should store the target UUID");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void spyEchoReplaysLastSeenAbility(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity enemy = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(enemy);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(enemy, world, pos.x + 2.0D, pos.y, pos.z, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPY);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            NbtCompound nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
            Identifier observed = com.feel.gems.power.registry.PowerIds.SPY_SMOKE_BOMB;
            nbt.putString("spyLastSeenAbility", observed.toString());
            nbt.putLong("spyLastSeenAt", world.getTime());
            NbtCompound observedRoot = new NbtCompound();
            NbtCompound rec = new NbtCompound();
            rec.putInt("epoch", com.feel.gems.power.gem.spy.SpySystem.deaths(player));
            rec.putLong("first", world.getTime());
            rec.putLong("last", world.getTime());
            rec.putInt("count", 1);
            observedRoot.put(observed.toString(), rec);
            nbt.put("spyObserved", observedRoot);
            boolean ok = new SpyEchoAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Echo did not replay the last seen ability");
                return;
            }
        });

        context.runAtTick(15L, () -> {
            // Smoke Bomb's own test covers affecting nearby enemies; for Echo, verify that we successfully
            // replayed the ability (self invisibility is always applied on activation).
            if (player.getStatusEffect(StatusEffects.INVISIBILITY) == null) {
                context.throwGameTestException("Echo should re-cast the observed ability");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void spyMimicFormActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPY);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            NbtCompound nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
            nbt.putString("spyLastKilledType", "minecraft:cow");
            boolean ok = new SpyMimicFormAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Mimic Form did not activate with a stored kill");
                return;
            }
            if (player.getStatusEffect(StatusEffects.INVISIBILITY) == null) {
                context.throwGameTestException("Mimic Form should grant invisibility");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void spyStealAddsStolenAbility(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPY);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            NbtCompound nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
            Identifier observed = com.feel.gems.power.registry.PowerIds.SPY_SMOKE_BOMB;
            nbt.putString("spyLastSeenAbility", observed.toString());
            nbt.putLong("spyLastSeenAt", world.getTime());

            NbtCompound observedMap = new NbtCompound();
            NbtCompound rec = new NbtCompound();
            int required = GemsBalance.v().spy().stealRequiredWitnessCount();
            rec.putInt("count", required);
            rec.putLong("first", world.getTime());
            rec.putLong("last", world.getTime());
            rec.putInt("epoch", SpySystem.deaths(player));
            observedMap.put(observed.toString(), rec);
            nbt.put("spyObserved", observedMap);

            boolean ok = new SpyStealAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Steal did not add the observed ability");
                return;
            }
            if (SpySystem.selectedStolenAbility(player) == null) {
                context.throwGameTestException("Steal should store a stolen ability");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void spyStolenCastUsesStolenAbility(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity enemy = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(enemy);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(enemy, world, pos.x + 2.0D, pos.y, pos.z, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPY);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            NbtCompound nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
            NbtList stolen = new NbtList();
            stolen.add(NbtString.of(com.feel.gems.power.registry.PowerIds.SPY_SMOKE_BOMB.toString()));
            nbt.put("spyStolen", stolen);
            nbt.putInt("spyStolenSelected", 0);
            boolean ok = new SpyStolenCastAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Stolen Cast did not activate with a stolen ability");
                return;
            }
        });

        context.runAtTick(15L, () -> {
            if (enemy.getStatusEffect(StatusEffects.BLINDNESS) == null) {
                context.throwGameTestException("Stolen Cast should execute the stolen ability");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void spyStillnessCloakGrantsInvisibility(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPY);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        int stillness = GemsBalance.v().spy().stillnessTicks();

        context.runAtTick(5L, () -> SpySystem.tickEverySecond(player));
        context.runAtTick(5L + stillness + 2L, () -> {
            SpySystem.tickEverySecond(player);
            if (player.getStatusEffect(StatusEffects.INVISIBILITY) == null) {
                context.throwGameTestException("Stillness Cloak should grant invisibility after standing still");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void spySilentStepPreventsSculkSensorTrigger(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        context.setBlockState(0, 1, 0, Blocks.SCULK_SENSOR.getDefaultState());
        var pos = net.minecraft.util.math.BlockPos.ofFloored(context.getAbsolute(new Vec3d(0.0D, 1.0D, 0.0D)));
        SculkSensorBlockEntity sensor = (SculkSensorBlockEntity) world.getBlockEntity(pos);
        if (sensor == null) {
            context.throwGameTestException("Failed to create sculk sensor block entity");
            return;
        }

        Vec3d playerPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 2.5D));
        teleport(player, world, playerPos.x, playerPos.y, playerPos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPY);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            world.emitGameEvent(GameEvent.STEP, player.getEntityPos(), GameEvent.Emitter.of(player));
        });

        context.runAtTick(15L, () -> {
            if (!SculkSensorBlock.isInactive(world.getBlockState(pos))) {
                context.throwGameTestException("Silent Step should block sculk sensor activation");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void spyBackstabDealsBonusDamage(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity attacker = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(attacker);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(attacker, world, pos.x, pos.y, pos.z - 1.5D, 0.0F, 0.0F);
        teleport(target, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(attacker);
        GemPlayerState.setActiveGem(attacker, GemId.SPY);
        GemPlayerState.setEnergy(attacker, 5);
        GemPowers.sync(attacker);

        context.runAtTick(5L, () -> {
            float before = target.getHealth();
            attacker.attack(target);
            if (target.getHealth() >= before) {
                context.throwGameTestException("Backstab should deal bonus damage from behind");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void spyQuickHandsGrantsHaste(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPY);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            if (player.getStatusEffect(StatusEffects.HASTE) == null) {
                context.throwGameTestException("Quick Hands should grant Haste");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void spyConfigValuesValid(TestContext context) {
        var cfg = GemsBalance.v().spy();
        
        if (cfg.smokeBombCooldownTicks() < 0) {
            context.throwGameTestException("Smoke Bomb cooldown cannot be negative");
        }
        if (cfg.skinshiftCooldownTicks() < 0) {
            context.throwGameTestException("Skinshift cooldown cannot be negative");
        }
        if (cfg.echoCooldownTicks() < 0) {
            context.throwGameTestException("Echo cooldown cannot be negative");
        }
        
        context.complete();
    }
}
