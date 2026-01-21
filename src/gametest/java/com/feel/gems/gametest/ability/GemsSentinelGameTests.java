package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.sentinel.*;
import com.feel.gems.power.gem.sentinel.SentinelPassiveRuntime;
import com.feel.gems.power.runtime.AbilityRestrictions;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.trust.GemTrust;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class GemsSentinelGameTests {

    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void sentinelTauntActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(target, world, pos.x + 2.0D, pos.y, pos.z, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SENTINEL);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new SentinelTauntAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Sentinel Taunt did not activate");
                return;
            }
            if (!SentinelTauntRuntime.isTaunted(target)) {
                context.throwGameTestException("Taunt should mark nearby enemies as taunted");
                return;
            }
            if (player.getStatusEffect(StatusEffects.RESISTANCE) == null) {
                context.throwGameTestException("Taunt should grant Resistance to the caster");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void sentinelShieldWallActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SENTINEL);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new SentinelShieldWallAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Shield Wall did not activate");
                return;
            }
            String worldId = world.getRegistryKey().getValue().toString();
            BlockPos basePos = player.getBlockPos().offset(player.getHorizontalFacing(), 2);
            if (!SentinelShieldWallRuntime.isInWall(Vec3d.ofCenter(basePos), worldId)) {
                context.throwGameTestException("Shield Wall should register wall positions");
                return;
            }
            if (!SentinelShieldWallRuntime.hasAnyWalls()) {
                context.throwGameTestException("Shield Wall runtime should track active walls");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void sentinelRallyCryActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity ally = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(ally);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(ally, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SENTINEL);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        GemTrust.trust(player, ally.getUuid());
        GemTrust.trust(ally, player.getUuid());
        GemTrust.trust(ally, player.getUuid());
        ally.setHealth(6.0F);

        context.runAtTick(5L, () -> {
            boolean ok = new SentinelRallyCryAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Rally Cry did not activate");
                return;
            }
            if (ally.getHealth() <= 6.0F) {
                context.throwGameTestException("Rally Cry should heal trusted allies");
                return;
            }
            if (ally.getStatusEffect(StatusEffects.RESISTANCE) == null) {
                context.throwGameTestException("Rally Cry should grant Resistance to allies");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void sentinelLockdownActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity enemy = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(enemy);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(enemy, world, pos.x + 2.0D, pos.y, pos.z, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SENTINEL);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new SentinelLockdownAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Lockdown did not activate");
                return;
            }
            SentinelLockdownRuntime.tick(world.getTime(), world.getRegistryKey().getValue().toString(), world);
        });

        context.runAtTick(15L, () -> {
            if (!SentinelLockdownRuntime.isInLockdownZone(enemy.getEntityPos(), enemy.getUuid(), world.getRegistryKey().getValue().toString())) {
                context.throwGameTestException("Lockdown should create a zone affecting enemies");
                return;
            }
            if (!AbilityRestrictions.isSuppressed(enemy)) {
                context.throwGameTestException("Lockdown should suppress enemy abilities");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void sentinelInterventionRedirectsDamage(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity ally = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity attacker = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(ally);
        GemsGameTestUtil.forceSurvival(attacker);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(ally, world, pos.x + 3.0D, pos.y, pos.z, 0.0F, 0.0F);
        teleport(attacker, world, pos.x + 3.0D, pos.y, pos.z + 2.0D, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SENTINEL);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(4L, () -> {
            GemTrust.trust(player, ally.getUuid());
            GemTrust.trust(ally, player.getUuid());
        });

        context.runAtTick(6L, () -> {
            boolean ok = new SentinelInterventionAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Intervention did not activate with a trusted ally nearby");
            }
        });

        context.runAtTick(12L, () -> {
            if (SentinelInterventionRuntime.getProtector(ally) != player) {
                context.throwGameTestException("Intervention should mark the sentinel as the ally's protector");
            }
        });

        context.runAtTick(20L, () -> {
            float allyBefore = ally.getHealth();
            float sentinelBefore = player.getHealth();
            ally.damage(world, attacker.getDamageSources().playerAttack(attacker), 4.0F);
            if (ally.getHealth() < allyBefore) {
                context.throwGameTestException("Intervention should redirect damage away from the ally");
                return;
            }
            if (player.getHealth() >= sentinelBefore) {
                context.throwGameTestException("Intervention should transfer damage to the sentinel");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void sentinelGuardianAuraReducesDamage(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity sentinel = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity ally = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity attacker = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(sentinel);
        GemsGameTestUtil.forceSurvival(ally);
        GemsGameTestUtil.forceSurvival(attacker);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(sentinel, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(ally, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
        teleport(attacker, world, pos.x + 2.0D, pos.y, pos.z + 2.0D, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(sentinel);
        GemPlayerState.setActiveGem(sentinel, GemId.SENTINEL);
        GemPlayerState.setEnergy(sentinel, 5);
        GemPowers.sync(sentinel);

        GemTrust.trust(sentinel, ally.getUuid());

        context.runAtTick(5L, () -> {
            float before = ally.getHealth();
            ally.damage(world, attacker.getDamageSources().playerAttack(attacker), 6.0F);
            float taken = before - ally.getHealth();
            if (taken <= 0.0F) {
                context.throwGameTestException("Guardian Aura test did not apply damage");
                return;
            }
            if (!SentinelPassiveRuntime.isProtectedByGuardianAura(ally)) {
                context.throwGameTestException("Guardian Aura should detect protected allies nearby");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void sentinelFortressGrantsResistanceAfterStandingStill(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SENTINEL);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        int required = GemsBalance.v().sentinel().fortressStandStillTicks();

        context.runAtTick(5L, () -> SentinelPassiveRuntime.tickFortress(player));
        context.runAtTick(5L + required + 2L, () -> {
            SentinelPassiveRuntime.tickFortress(player);
            if (player.getStatusEffect(StatusEffects.RESISTANCE) == null) {
                context.throwGameTestException("Fortress should grant Resistance after standing still");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void sentinelRetributionThornsReflectsDamage(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity sentinel = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity attacker = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(sentinel);
        GemsGameTestUtil.forceSurvival(attacker);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(sentinel, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(attacker, world, pos.x + 2.0D, pos.y, pos.z, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(sentinel);
        GemPlayerState.setActiveGem(sentinel, GemId.SENTINEL);
        GemPlayerState.setEnergy(sentinel, 5);
        GemPowers.sync(sentinel);

        context.runAtTick(5L, () -> {
            float attackerBefore = attacker.getHealth();
            sentinel.damage(world, attacker.getDamageSources().playerAttack(attacker), 4.0F);
            if (attacker.getHealth() >= attackerBefore) {
                context.throwGameTestException("Retribution Thorns should reflect damage to the attacker");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void sentinelConfigValuesValid(TestContext context) {
        var cfg = GemsBalance.v().sentinel();
        
        if (cfg.tauntCooldownTicks() < 0) {
            context.throwGameTestException("Taunt cooldown cannot be negative");
        }
        if (cfg.shieldWallCooldownTicks() < 0) {
            context.throwGameTestException("Shield Wall cooldown cannot be negative");
        }
        if (cfg.rallyCryCooldownTicks() < 0) {
            context.throwGameTestException("Rally Cry cooldown cannot be negative");
        }
        
        context.complete();
    }
}
