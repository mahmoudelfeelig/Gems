package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.beacon.BeaconAuraAbility;
import com.feel.gems.power.gem.beacon.BeaconAuraRuntime;
import com.feel.gems.power.gem.beacon.BeaconSupportRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.trust.GemTrust;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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
    public void beaconAuraAppliesToAlliesAndDebuffsEnemies(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity ally = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity enemy = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(ally);
        GemsGameTestUtil.forceSurvival(enemy);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(ally, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
        teleport(enemy, world, pos.x - 2.0D, pos.y, pos.z, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.BEACON);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        GemTrust.trust(player, ally.getUuid());

        context.runAtTick(5L, () -> {
            new BeaconAuraAbility(BeaconAuraRuntime.AuraType.SPEED).activate(player);
            BeaconAuraRuntime.tickEverySecond(player);
        });

        context.runAtTick(15L, () -> {
            if (ally.getStatusEffect(StatusEffects.SPEED) == null) {
                context.throwGameTestException("Beacon aura should buff trusted allies");
                return;
            }
            if (enemy.getStatusEffect(StatusEffects.SLOWNESS) == null || enemy.getStatusEffect(StatusEffects.WEAKNESS) == null) {
                context.throwGameTestException("Beacon aura should debuff enemies with slowness and weakness");
                return;
            }
            if (enemy.getStatusEffect(StatusEffects.SPEED) != null) {
                context.throwGameTestException("Beacon aura should not apply positive effects to enemies");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void beaconRallyGrantsAbsorptionToAllies(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity ally = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity enemy = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(ally);
        GemsGameTestUtil.forceSurvival(enemy);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(ally, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
        teleport(enemy, world, pos.x - 2.0D, pos.y, pos.z, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.BEACON);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        GemTrust.trust(player, ally.getUuid());

        context.runAtTick(5L, () -> new BeaconAuraAbility(BeaconAuraRuntime.AuraType.RESISTANCE).activate(player));

        context.runAtTick(15L, () -> {
            if (ally.getStatusEffect(StatusEffects.ABSORPTION) == null) {
                context.throwGameTestException("Beacon Rally should grant Absorption to allies");
                return;
            }
            if (enemy.getStatusEffect(StatusEffects.WEAKNESS) == null) {
                context.throwGameTestException("Beacon Rally should apply Weakness to enemies");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void beaconCoreAppliesRegenPulse(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity ally = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(ally);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(ally, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.BEACON);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        GemTrust.trust(player, ally.getUuid());

        context.runAtTick(5L, () -> BeaconSupportRuntime.tickEverySecond(player));

        context.runAtTick(15L, () -> {
            if (ally.getStatusEffect(StatusEffects.REGENERATION) == null) {
                context.throwGameTestException("Beacon Core should grant Regeneration to trusted allies");
                return;
            }
            if (player.getStatusEffect(StatusEffects.REGENERATION) == null) {
                context.throwGameTestException("Beacon Core should grant Regeneration to self");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void beaconStabilizeReducesHarmfulEffects(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity ally = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity enemy = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(ally);
        GemsGameTestUtil.forceSurvival(enemy);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(ally, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
        teleport(enemy, world, pos.x - 2.0D, pos.y, pos.z, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.BEACON);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        GemTrust.trust(player, ally.getUuid());

        StatusEffectInstance poison = new StatusEffectInstance(StatusEffects.POISON, 200, 0, true, false, false);
        ally.addStatusEffect(poison);

        context.runAtTick(5L, () -> BeaconSupportRuntime.tickEverySecond(player));

        context.runAtTick(15L, () -> {
            var remaining = ally.getStatusEffect(StatusEffects.POISON);
            if (remaining != null && remaining.getDuration() >= poison.getDuration()) {
                context.throwGameTestException("Stabilize should reduce harmful effect duration on allies");
                return;
            }
            if (enemy.getStatusEffect(StatusEffects.MINING_FATIGUE) == null) {
                context.throwGameTestException("Stabilize should debuff enemies with Mining Fatigue");
                return;
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
