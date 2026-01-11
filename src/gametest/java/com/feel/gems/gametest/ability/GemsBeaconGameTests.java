package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.beacon.BeaconAuraAbility;
import com.feel.gems.power.gem.beacon.BeaconAuraRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Vec3d;

public final class GemsBeaconGameTests {

    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void beaconAuraStrengthActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.BEACON);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new BeaconAuraAbility(BeaconAuraRuntime.AuraType.STRENGTH).activate(player);
            if (!ok) {
                context.throwGameTestException("Beacon Aura Strength did not activate");
            }
            BeaconAuraRuntime.AuraType active = BeaconAuraRuntime.activeType(player);
            if (active != BeaconAuraRuntime.AuraType.STRENGTH) {
                context.throwGameTestException("Aura type was not set to STRENGTH");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void beaconAuraResistanceActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.BEACON);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new BeaconAuraAbility(BeaconAuraRuntime.AuraType.RESISTANCE).activate(player);
            if (!ok) {
                context.throwGameTestException("Beacon Aura Resistance did not activate");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void beaconAuraToggleOff(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.BEACON);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            new BeaconAuraAbility(BeaconAuraRuntime.AuraType.STRENGTH).activate(player);
        });

        context.runAtTick(10L, () -> {
            new BeaconAuraAbility(BeaconAuraRuntime.AuraType.STRENGTH).activate(player);
            BeaconAuraRuntime.AuraType active = BeaconAuraRuntime.activeType(player);
            if (active != null) {
                context.throwGameTestException("Aura should be toggled off");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void beaconConfigValuesValid(TestContext context) {
        var cfg = GemsBalance.v().beacon();
        
        if (cfg.auraCooldownTicks() < 0) {
            context.throwGameTestException("Aura cooldown cannot be negative");
        }
        if (cfg.auraRadiusBlocks() <= 0) {
            context.throwGameTestException("Aura radius must be positive");
        }
        
        context.complete();
    }
}
