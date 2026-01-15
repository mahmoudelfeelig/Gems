package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.terror.*;
import com.feel.gems.power.gem.terror.TerrorBloodPrice;
import com.feel.gems.power.gem.terror.TerrorRemoteChargeRuntime;
import com.feel.gems.power.gem.terror.TerrorRigRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class GemsTerrorGameTests {

    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    private static void aimAt(ServerPlayerEntity player, ServerWorld world, Vec3d target) {
        Vec3d pos = player.getEntityPos();
        double dx = target.x - pos.x;
        double dz = target.z - pos.z;
        double dy = target.y - player.getEyeY();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        teleport(player, world, pos.x, pos.y, pos.z, yaw, pitch);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void terrorPanicRingActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.TERROR);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            int before = world.getEntitiesByClass(TntEntity.class, player.getBoundingBox().expand(16.0D), e -> true).size();
            boolean ok = new PanicRingAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Panic Ring did not activate");
            }
            int after = world.getEntitiesByClass(TntEntity.class, player.getBoundingBox().expand(16.0D), e -> true).size();
            int expected = GemsBalance.v().terror().panicRingTntCount();
            if (after - before < expected) {
                context.throwGameTestException("Panic Ring should spawn the configured TNT count");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void terrorBreachChargeActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.TERROR);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        ZombieEntity target = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
        if (target == null) {
            context.throwGameTestException("Failed to create zombie");
            return;
        }
        target.refreshPositionAndAngles(pos.x, pos.y, pos.z + 3.0D, 180.0F, 0.0F);
        world.spawnEntity(target);
        aimAt(player, world, target.getEntityPos().add(0.0D, 1.2D, 0.0D));
        float before = target.getHealth();

        context.runAtTick(5L, () -> {
            boolean ok = new TerrorBreachChargeAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Breach Charge did not activate");
            }
            if (target.getHealth() >= before) {
                context.throwGameTestException("Breach Charge should damage targets");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void terrorRemoteChargeActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.TERROR);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        BlockPos target = new BlockPos((int) pos.x, (int) pos.y - 1, (int) pos.z + 2);
        world.setBlockState(target, Blocks.STONE.getDefaultState());
        aimAt(player, world, Vec3d.ofCenter(target));

        context.runAtTick(5L, () -> {
            boolean ok = new TerrorRemoteChargeAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Remote Charge should begin arming");
            }
            if (!TerrorRemoteChargeRuntime.isArming(player)) {
                context.throwGameTestException("Remote Charge should enter arming state");
            }
            if (!TerrorRemoteChargeRuntime.tryArm(player, target)) {
                context.throwGameTestException("Remote Charge should arm on a block");
            }
            if (!TerrorRemoteChargeRuntime.hasActiveCharge(player)) {
                context.throwGameTestException("Remote Charge should have an active charge after arming");
            }
        });

        context.runAtTick(20L, () -> {
            int before = world.getEntitiesByClass(TntEntity.class, player.getBoundingBox().expand(16.0D), e -> true).size();
            boolean detonated = new TerrorRemoteChargeAbility().activate(player);
            if (!detonated) {
                context.throwGameTestException("Remote Charge should detonate when armed");
            }
            int after = world.getEntitiesByClass(TntEntity.class, player.getBoundingBox().expand(16.0D), e -> true).size();
            if (after <= before) {
                context.throwGameTestException("Remote Charge detonation should spawn TNT");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void terrorRigActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity enemy = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(enemy);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        BlockPos rigPos = new BlockPos((int) pos.x, (int) pos.y - 1, (int) pos.z + 2);
        world.setBlockState(rigPos, Blocks.STONE.getDefaultState());
        aimAt(player, world, Vec3d.ofCenter(rigPos));

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.TERROR);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new TerrorRigAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Rig did not activate");
            }
            if (!TerrorRigRuntime.hasTrap(world, rigPos)) {
                context.throwGameTestException("Rig should arm the target block");
            }
        });

        context.runAtTick(20L, () -> {
            int before = world.getEntitiesByClass(TntEntity.class, player.getBoundingBox().expand(16.0D), e -> true).size();
            teleport(enemy, world, rigPos.getX() + 0.5D, rigPos.getY() + 1.1D, rigPos.getZ() + 0.5D, 0.0F, 0.0F);
            TerrorRigRuntime.checkStep(enemy);
            int after = world.getEntitiesByClass(TntEntity.class, player.getBoundingBox().expand(16.0D), e -> true).size();
            if (after <= before) {
                context.throwGameTestException("Rig should detonate when stepped on");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void terrorTradeActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        ZombieEntity target = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
        if (target == null) {
            context.throwGameTestException("Failed to create zombie");
            return;
        }
        target.refreshPositionAndAngles(pos.x, pos.y, pos.z + 3.0D, 180.0F, 0.0F);
        world.spawnEntity(target);
        aimAt(player, world, target.getEntityPos().add(0.0D, 1.2D, 0.0D));

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.TERROR);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new TerrorTradeAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Terror Trade did not activate");
            }
        });

        context.runAtTick(20L, () -> {
            if (target.isAlive()) {
                context.throwGameTestException("Terror Trade should kill the target");
            }
            if (player.isAlive()) {
                context.throwGameTestException("Terror Trade should kill the caster");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void terrorPassivesApply(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity enemy = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(enemy);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(enemy, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.TERROR);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(StatusEffects.DARKNESS, 200, 0));
        player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(StatusEffects.BLINDNESS, 200, 0));

        context.runAtTick(5L, () -> {
            GemPowers.maintain(player);
            if (!enemy.hasStatusEffect(StatusEffects.DARKNESS)) {
                context.throwGameTestException("Dread Aura should apply Darkness to nearby enemies");
            }
            if (player.hasStatusEffect(StatusEffects.DARKNESS) || player.hasStatusEffect(StatusEffects.BLINDNESS)) {
                context.throwGameTestException("Fearless should clear Darkness and Blindness");
            }
            TerrorBloodPrice.onPlayerKill(player);
            if (!player.hasStatusEffect(StatusEffects.STRENGTH) || !player.hasStatusEffect(StatusEffects.RESISTANCE)) {
                context.throwGameTestException("Blood Price should grant Strength and Resistance");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void terrorConfigValuesValid(TestContext context) {
        var cfg = GemsBalance.v().terror();
        
        if (cfg.panicRingCooldownTicks() < 0) {
            context.throwGameTestException("Panic Ring cooldown cannot be negative");
        }
        if (cfg.breachChargeCooldownTicks() < 0) {
            context.throwGameTestException("Breach Charge cooldown cannot be negative");
        }
        if (cfg.remoteChargeCooldownTicks() < 0) {
            context.throwGameTestException("Remote Charge cooldown cannot be negative");
        }
        
        context.complete();
    }
}
