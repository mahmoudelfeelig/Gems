package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.flux.*;
import com.feel.gems.power.gem.flux.FluxCharge;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Vec3d;

public final class GemsFluxGameTests {

    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fluxBeamRequiresTarget(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new FluxBeamAbility().activate(player);
            if (ok) {
                context.throwGameTestException("Flux Beam should fail without target");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fluxDischargeRequiresCharge(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);
        FluxCharge.set(player, 0);

        context.runAtTick(5L, () -> {
            boolean ok = new FluxDischargeAbility().activate(player);
            int minCharge = GemsBalance.v().flux().fluxDischargeMinCharge();
            if (minCharge > 0 && ok) {
                context.throwGameTestException("Discharge should fail without enough charge");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fluxSurgeRequiresCharge(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);
        FluxCharge.set(player, 0); // No charge

        context.runAtTick(5L, () -> {
            // Surge requires charge - should fail without enough
            boolean ok = new FluxSurgeAbility().activate(player);
            int cost = GemsBalance.v().flux().fluxSurgeChargeCost();
            if (cost > 0 && ok) {
                context.throwGameTestException("Flux Surge should fail without enough charge");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fluxStaticBurstRequiresStoredDamage(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            // StaticBurst requires stored damage from damage taken - should fail without any
            boolean ok = new StaticBurstAbility().activate(player);
            if (ok) {
                context.throwGameTestException("Static Burst should fail without stored damage");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fluxChargeTracking(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            FluxCharge.set(player, 50);
            int charge = FluxCharge.get(player);
            if (charge != 50) {
                context.throwGameTestException("Flux charge should be 50, got " + charge);
            }
            FluxCharge.set(player, 75);
            charge = FluxCharge.get(player);
            if (charge != 75) {
                context.throwGameTestException("Flux charge should be 75, got " + charge);
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fluxConfigValuesValid(TestContext context) {
        var cfg = GemsBalance.v().flux();
        
        if (cfg.fluxBeamCooldownTicks() < 0) {
            context.throwGameTestException("Flux Beam cooldown cannot be negative");
        }
        if (cfg.fluxDischargeCooldownTicks() < 0) {
            context.throwGameTestException("Discharge cooldown cannot be negative");
        }
        if (cfg.fluxSurgeCooldownTicks() < 0) {
            context.throwGameTestException("Surge cooldown cannot be negative");
        }
        
        context.complete();
    }
}
