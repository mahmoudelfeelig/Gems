package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.trickster.TricksterGlitchStepAbility;
import com.feel.gems.power.ability.trickster.TricksterMindGamesAbility;
import com.feel.gems.power.ability.trickster.TricksterMirageAbility;
import com.feel.gems.power.ability.trickster.TricksterPuppetMasterAbility;
import com.feel.gems.power.ability.trickster.TricksterShadowSwapAbility;
import com.feel.gems.power.ability.trickster.TricksterMindGamesRuntime;
import com.feel.gems.power.ability.trickster.TricksterMirageRuntime;
import com.feel.gems.power.ability.trickster.TricksterPuppetRuntime;
import com.feel.gems.power.gem.trickster.TricksterPassiveRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;




public final class GemsTricksterGameTests {

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
    public void glitchStepTeleportsAndDamagesAfterimage(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity enemy = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(enemy);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(enemy, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.TRICKSTER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        Vec3d startPos = player.getEntityPos();
        float enemyBefore = enemy.getHealth();

        context.runAtTick(5L, () -> {
            boolean ok = new TricksterGlitchStepAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Glitch Step did not activate");
            }
        });

        context.runAtTick(15L, () -> {
            Vec3d endPos = player.getEntityPos();
            double dist = startPos.distanceTo(endPos);
            
            if (dist < 1.0D) {
                context.throwGameTestException("Glitch Step did not teleport player");
            }
            if (enemy.getHealth() >= enemyBefore) {
                context.throwGameTestException("Glitch Step afterimage should damage nearby enemies");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void mirageCreatesRuntimeClones(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.TRICKSTER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new TricksterMirageAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Mirage did not activate");
                return;
            }
        });

        context.runAtTick(15L, () -> {
            int count = TricksterMirageRuntime.getMirageCount(player);
            if (count <= 0) {
                context.throwGameTestException("Mirage should register clone count in runtime");
                return;
            }
            if (TricksterMirageRuntime.getMiragePositions(player).isEmpty()) {
                context.throwGameTestException("Mirage should expose clone positions");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void shadowSwapTeleportsBothPlayers(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d playerPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        Vec3d targetPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 5.5D));
        teleport(player, world, playerPos.x, playerPos.y, playerPos.z, 0.0F, 0.0F);
        teleport(target, world, targetPos.x, targetPos.y, targetPos.z, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.TRICKSTER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new TricksterShadowSwapAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Shadow Swap did not activate");
            }
        });

        context.runAtTick(15L, () -> {
            double playerDistToOriginal = player.getEntityPos().squaredDistanceTo(playerPos);
            double targetDistToOriginal = target.getEntityPos().squaredDistanceTo(targetPos);
            
            if (playerDistToOriginal < 1.0D || targetDistToOriginal < 1.0D) {
                context.throwGameTestException("Shadow Swap should swap player positions");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 300)
    public void mindGamesConfusesMobTargets(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        Vec3d mobPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 3.5D));
        var zombie = EntityType.ZOMBIE.create(world, e -> {}, BlockPos.ofFloored(mobPos), SpawnReason.TRIGGERED, false, false);
        if (zombie == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        zombie.refreshPositionAndAngles(mobPos.x, mobPos.y, mobPos.z, 0.0F, 0.0F);
        world.spawnEntity(zombie);
        aimAt(player, world, mobPos.add(0.0D, 1.0D, 0.0D));

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.TRICKSTER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new TricksterMindGamesAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Mind Games did not activate with a mob target");
                return;
            }
        });

        context.runAtTick(15L, () -> {
            if (zombie.getStatusEffect(StatusEffects.SLOWNESS) == null || zombie.getStatusEffect(StatusEffects.NAUSEA) == null) {
                context.throwGameTestException("Mind Games should apply confusion effects to mobs");
                return;
            }
            if (!TricksterMindGamesRuntime.isMobConfused(zombie)) {
                context.throwGameTestException("Mind Games should mark mobs as confused in runtime");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 300)
    public void puppetMasterMarksMobAsPuppeted(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        // Spawn a mob to control
        Vec3d mobPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 3.5D));
        var zombie = EntityType.ZOMBIE.create(world, e -> {}, BlockPos.ofFloored(mobPos), SpawnReason.TRIGGERED, false, false);
        if (zombie == null) {
            context.throwGameTestException("Failed to spawn zombie");
            return;
        }
        zombie.refreshPositionAndAngles(mobPos.x, mobPos.y, mobPos.z, 0.0F, 0.0F);
        world.spawnEntity(zombie);
        aimAt(player, world, mobPos.add(0.0D, 1.0D, 0.0D));

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.TRICKSTER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            // Re-aim at the zombie right before activation to ensure rotation is current
            aimAt(player, world, mobPos.add(0.0D, 1.0D, 0.0D));
            boolean ok = new TricksterPuppetMasterAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Puppet Master did not activate with a mob target");
                return;
            }
        });

        context.runAtTick(15L, () -> {
            if (!TricksterPuppetRuntime.isMobPuppeted(zombie)) {
                context.throwGameTestException("Puppet Master should mark the mob as puppeted");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void sleightOfHandPassiveCanPreventConsumption(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.TRICKSTER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean triggered = false;
            for (int i = 0; i < 200; i++) {
                if (TricksterPassiveRuntime.shouldNotConsumeThrowable(player)) {
                    triggered = true;
                    break;
                }
            }
            if (!triggered && GemsBalance.v().trickster().sleightOfHandChance() > 0.0F) {
                context.throwGameTestException("Sleight of Hand should occasionally prevent item consumption");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void tricksterSlipperyCanRemoveSlowness(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.TRICKSTER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(StatusEffects.SLOWNESS, 200, 0, false, false));
            for (int i = 0; i < 200; i++) {
                TricksterPassiveRuntime.tryRemoveSlowEffects(player);
                if (player.getStatusEffect(StatusEffects.SLOWNESS) == null) {
                    break;
                }
            }
            if (player.getStatusEffect(StatusEffects.SLOWNESS) != null && GemsBalance.v().trickster().slipperyChance() > 0.0F) {
                context.throwGameTestException("Slippery should occasionally remove slowing effects");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void glitchStepHasCooldownConfigured(TestContext context) {
        // Direct activate() bypasses cooldown system (handled by GemAbilities.activateByIndex)
        // Just verify the cooldown config is valid
        var cfg = GemsBalance.v().trickster();
        if (cfg.glitchStepCooldownTicks() <= 0) {
            context.throwGameTestException("Glitch Step cooldown must be positive");
        }
        context.complete();
    }
}
