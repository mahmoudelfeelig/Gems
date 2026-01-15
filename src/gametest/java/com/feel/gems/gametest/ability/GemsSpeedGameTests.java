package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.speed.ArcShotAbility;
import com.feel.gems.power.ability.speed.SpeedAfterimageAbility;
import com.feel.gems.power.ability.speed.SpeedSlipstreamAbility;
import com.feel.gems.power.ability.speed.SpeedStormAbility;
import com.feel.gems.power.ability.speed.SpeedTempoShiftAbility;
import com.feel.gems.power.ability.speed.TerminalVelocityAbility;
import com.feel.gems.power.gem.speed.SpeedFrictionlessSteps;
import com.feel.gems.power.gem.speed.SpeedMomentum;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemAbilityCooldowns;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.trust.GemTrust;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;




public final class GemsSpeedGameTests {

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
    public void arcShotStrikesTargetsInLine(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPEED);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        ZombieEntity zombie = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
        if (zombie == null) {
            context.throwGameTestException("Failed to create zombie");
            return;
        }
        zombie.refreshPositionAndAngles(pos.x, pos.y, pos.z + 4.0D, 180.0F, 0.0F);
        world.spawnEntity(zombie);
        aimAt(player, world, zombie.getEntityPos().add(0.0D, 1.0D, 0.0D));
        float before = zombie.getHealth();

        context.runAtTick(5L, () -> {
            boolean ok = new ArcShotAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Arc Shot did not activate");
                return;
            }
            if (zombie.getHealth() >= before) {
                context.throwGameTestException("Arc Shot should damage targets in its path");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void speedStormAppliesEffectsInRadius(TestContext context) {
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
        teleport(enemy, world, pos.x + 3.0D, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPEED);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        GemTrust.trust(player, ally.getUuid());

        context.runAtTick(5L, () -> {
            boolean ok = new SpeedStormAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Speed Storm did not activate");
                return;
            }
        });

        context.runAtTick(15L, () -> {
            AbilityRuntime.tickEverySecond(player);
            if (!ally.hasStatusEffect(StatusEffects.SPEED) || !ally.hasStatusEffect(StatusEffects.HASTE)) {
                context.throwGameTestException("Speed Storm should buff trusted allies");
                return;
            }
            if (!enemy.hasStatusEffect(StatusEffects.SLOWNESS) || !enemy.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
                context.throwGameTestException("Speed Storm should debuff enemies");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void terminalVelocityGrantsSpeedAndHaste(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPEED);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new TerminalVelocityAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Terminal Velocity did not activate");
            }
        });

        context.runAtTick(15L, () -> {
            var speed = player.getStatusEffect(StatusEffects.SPEED);
            var haste = player.getStatusEffect(StatusEffects.HASTE);

            if (speed == null) {
                context.throwGameTestException("Terminal Velocity did not grant speed");
            }
            if (haste == null) {
                context.throwGameTestException("Terminal Velocity did not grant haste");
            }

            var cfg = GemsBalance.v().speed();
            if (speed.getAmplifier() != cfg.terminalVelocitySpeedAmplifier()) {
                context.throwGameTestException("Terminal Velocity speed amplifier mismatch: got " + speed.getAmplifier() + " expected " + cfg.terminalVelocitySpeedAmplifier());
            }

            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void slipstreamBuffsAlliesAndSlowsEnemies(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity ally = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity enemy = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(ally);
        GemsGameTestUtil.forceSurvival(enemy);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(ally, world, pos.x, pos.y, pos.z + 2.0D, 180.0F, 0.0F);
        teleport(enemy, world, pos.x, pos.y, pos.z + 3.0D, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPEED);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        GemPlayerState.initIfNeeded(ally);
        GemPlayerState.setActiveGem(ally, GemId.SPEED);
        GemPlayerState.setEnergy(ally, 5);
        GemPowers.sync(ally);

        GemPlayerState.initIfNeeded(enemy);
        GemPlayerState.setActiveGem(enemy, GemId.SPEED);
        GemPlayerState.setEnergy(enemy, 5);
        GemPowers.sync(enemy);

        GemTrust.trust(player, ally.getUuid());

        context.runAtTick(5L, () -> {
            boolean ok = new SpeedSlipstreamAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Slipstream did not activate");
                return;
            }
        });

        context.runAtTick(15L, () -> {
            AbilityRuntime.tickEverySecond(player);
            if (!ally.hasStatusEffect(StatusEffects.SPEED)) {
                context.throwGameTestException("Slipstream should buff trusted allies");
                return;
            }
            if (!enemy.hasStatusEffect(StatusEffects.SLOWNESS)) {
                context.throwGameTestException("Slipstream should slow enemies");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void afterimageGrantsSpeedAndInvisibility(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPEED);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new SpeedAfterimageAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Afterimage did not activate");
            }
        });

        context.runAtTick(15L, () -> {
            var speed = player.getStatusEffect(StatusEffects.SPEED);
            if (speed == null) {
                context.throwGameTestException("Afterimage did not grant speed");
                return;
            }
            if (!player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                context.throwGameTestException("Afterimage should grant invisibility");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void tempoShiftShiftsCooldownsForAlliesAndEnemies(TestContext context) {
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
        teleport(enemy, world, pos.x + 3.0D, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPEED);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        GemPlayerState.initIfNeeded(ally);
        GemPlayerState.setActiveGem(ally, GemId.SPEED);
        GemPlayerState.setEnergy(ally, 5);
        GemPowers.sync(ally);

        GemPlayerState.initIfNeeded(enemy);
        GemPlayerState.setActiveGem(enemy, GemId.SPEED);
        GemPlayerState.setEnergy(enemy, 5);
        GemPowers.sync(enemy);

        GemTrust.trust(player, ally.getUuid());

        long now = world.getTime();
        GemAbilityCooldowns.setNextAllowedTick(ally, PowerIds.TERMINAL_VELOCITY, now + 200);
        GemAbilityCooldowns.setNextAllowedTick(enemy, PowerIds.TERMINAL_VELOCITY, now + 200);

        context.runAtTick(5L, () -> {
            boolean ok = new SpeedTempoShiftAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Tempo Shift did not activate");
                return;
            }
        });

        context.runAtTick(25L, () -> {
            AbilityRuntime.tickEverySecond(player);
            long allyNext = GemAbilityCooldowns.nextAllowedTick(ally, PowerIds.TERMINAL_VELOCITY);
            long enemyNext = GemAbilityCooldowns.nextAllowedTick(enemy, PowerIds.TERMINAL_VELOCITY);
            if (allyNext >= now + 200) {
                context.throwGameTestException("Tempo Shift should reduce ally cooldowns");
                return;
            }
            if (enemyNext <= now + 200) {
                context.throwGameTestException("Tempo Shift should increase enemy cooldowns");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void momentumPassiveScalesDamageWithSpeed(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(target, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPEED);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            // Set player to be moving fast
            player.setVelocity(new Vec3d(0.5D, 0.0D, 0.0D));
            
            float mult = SpeedMomentum.multiplier(player);
            var cfg = GemsBalance.v().speed();
            
            if (mult < cfg.momentumMinMultiplier() || mult > cfg.momentumMaxMultiplier()) {
                context.throwGameTestException("Momentum multiplier out of range: " + mult);
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void frictionlessStepsGrantsSpeedOnFrictionBlocks(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPEED);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        // Place cobweb under player
        context.setBlockState(0, 1, 0, net.minecraft.block.Blocks.COBWEB.getDefaultState());

        context.runAtTick(5L, () -> {
            // Frictionless steps ticks on the server, not per-player
            SpeedFrictionlessSteps.tick(world.getServer());
        });

        context.runAtTick(15L, () -> {
            if (!player.hasStatusEffect(StatusEffects.SPEED)) {
                context.throwGameTestException("Frictionless Steps should grant speed on friction blocks");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void speedPassivesApplySpeedAndHaste(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPEED);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            GemPowers.maintain(player);
            if (!player.hasStatusEffect(StatusEffects.SPEED) || !player.hasStatusEffect(StatusEffects.HASTE)) {
                context.throwGameTestException("Speed passives should apply Speed and Haste");
                return;
            }
            context.complete();
        });
    }
}
