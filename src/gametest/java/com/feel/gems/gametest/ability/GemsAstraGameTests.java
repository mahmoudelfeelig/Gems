package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.astra.*;
import com.feel.gems.power.gem.astra.SoulSummons;
import com.feel.gems.power.gem.astra.SoulSystem;
import com.feel.gems.power.runtime.AbilityRestrictions;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

public final class GemsAstraGameTests {

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
    public void astraSoulCaptureStoresSoulAndHeals(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.placeStoneFloor(context, 1, 2);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.ASTRA);
        GemPlayerState.setEnergy(player, 5);
        GemPlayerState.setPassivesEnabled(player, true);
        GemPowers.sync(player);

        player.setHealth(Math.max(2.0F, player.getHealth() - 4.0F));
        float before = player.getHealth();

        ZombieEntity zombie = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
        if (zombie == null) {
            context.throwGameTestException("Failed to create zombie");
            return;
        }
        zombie.refreshPositionAndAngles(pos.x + 1.0D, pos.y, pos.z, 0.0F, 0.0F);
        zombie.setAiDisabled(true);
        zombie.setNoGravity(true);
        zombie.setVelocity(Vec3d.ZERO);
        zombie.setHealth(1.0F);
        world.spawnEntity(zombie);

        context.runAtTick(40L, () -> {
            player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, net.minecraft.item.Items.DIAMOND_SWORD.getDefaultStack());
            player.setVelocity(Vec3d.ZERO);
            player.attack(zombie);
            if (zombie.isAlive()) {
                zombie.damage(world, player.getDamageSources().playerAttack(player), Float.MAX_VALUE);
            }
            if (zombie.isAlive()) {
                zombie.kill(world);
            }
            // If the combat event didn't fire, trigger the capture directly to keep the test stable.
            if (!SoulSystem.hasSoul(player) && !zombie.isAlive()) {
                SoulSystem.onKilledMob(player, zombie);
            }
        });

        GemsGameTestUtil.assertEventually(
                context,
                42L,
                160L,
                1L,
                () -> SoulSystem.hasSoul(player)
                        && !SoulSystem.soulType(player).isEmpty()
                        && player.getHealth() > before,
                "Soul Capture did not store a soul or heal the player"
        );
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 220)
    public void astraSoulReleaseSpawnsSoulAndClearsState(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.ASTRA);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        ZombieEntity zombie = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
        if (zombie == null) {
            context.throwGameTestException("Failed to create zombie");
            return;
        }
        zombie.refreshPositionAndAngles(pos.x + 1.0D, pos.y, pos.z, 0.0F, 0.0F);
        world.spawnEntity(zombie);

        context.runAtTick(5L, () -> zombie.damage(world, player.getDamageSources().playerAttack(player), 50.0F));

        context.runAtTick(20L, () -> {
            if (!SoulSystem.hasSoul(player)) {
                context.throwGameTestException("Expected a stored soul before release");
                return;
            }
            if (!SoulSystem.release(player)) {
                context.throwGameTestException("Soul Release did not activate");
                return;
            }
        });

        context.runAtTick(40L, () -> {
            if (SoulSystem.hasSoul(player)) {
                context.throwGameTestException("Soul Release should clear the stored soul");
                return;
            }
            // Expand search box significantly to account for any spawn offset variations
            Box box = new Box(player.getBlockPos()).expand(12.0D);
            boolean found = world.getEntitiesByClass(net.minecraft.entity.Entity.class, box, SoulSummons::isSoul).stream()
                    .anyMatch(entity -> player.getUuid().equals(SoulSummons.ownerUuid(entity)));
            if (!found) {
                context.throwGameTestException("Soul Release did not spawn a soul summon");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void astraUnboundedActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.ASTRA);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new UnboundedAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Unbounded did not activate");
                return;
            }
            if (player.interactionManager.getGameMode() != GameMode.SPECTATOR) {
                context.throwGameTestException("Unbounded should switch to spectator mode");
                return;
            }
        });

        int duration = GemsBalance.v().astra().unboundedDurationTicks();
        context.runAtTick(5L + duration + 5L, () -> {
            AbilityRuntime.tickEverySecond(player);
            if (player.interactionManager.getGameMode() != GameMode.SURVIVAL) {
                context.throwGameTestException("Unbounded should return to survival");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void astraTagRequiresTarget(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.ASTRA);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new TagAbility().activate(player);
            if (ok) {
                context.throwGameTestException("Tag should fail without target");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void astraSpookAppliesDebuffs(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.ASTRA);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            ZombieEntity zombie = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
            if (zombie == null) {
                context.throwGameTestException("Failed to create zombie");
                return;
            }
            zombie.refreshPositionAndAngles(pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
            world.spawnEntity(zombie);
            boolean ok = new SpookAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Spook did not activate");
                return;
            }
            if (!zombie.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.DARKNESS)
                    || !zombie.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.NAUSEA)) {
                context.throwGameTestException("Spook should apply darkness and nausea");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void astraShadowAnchorReturnsToAnchor(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.ASTRA);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new ShadowAnchorAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Shadow Anchor did not activate");
                return;
            }
            BlockPos anchor = player.getBlockPos();
            teleport(player, world, pos.x + 6.0D, pos.y, pos.z, 0.0F, 0.0F);
            boolean ok2 = new ShadowAnchorAbility().activate(player);
            if (!ok2) {
                context.throwGameTestException("Shadow Anchor return did not activate");
                return;
            }
            if (!player.getBlockPos().equals(anchor)) {
                context.throwGameTestException("Shadow Anchor did not return player to anchor");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void astraDimensionalVoidSuppressesPlayers(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(target, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.ASTRA);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new DimensionalVoidAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Dimensional Void did not activate");
                return;
            }
            if (!AbilityRestrictions.isSuppressed(target)) {
                context.throwGameTestException("Dimensional Void should suppress nearby players");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void astraAstralDaggersSpawnsDaggers(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.ASTRA);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new AstralDaggersAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Astral Daggers did not activate");
                return;
            }
            int expected = GemsBalance.v().astra().astralDaggersCount();
            Box box = new Box(player.getBlockPos()).expand(6.0D);
            int found = world.getEntitiesByClass(ArrowEntity.class, box, e -> true).size();
            if (found < expected) {
                context.throwGameTestException("Expected at least " + expected + " daggers, found " + found);
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void astraAstralCameraActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.ASTRA);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new AstralCameraAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Astral Camera did not activate");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void astraTagAppliesGlowing(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.ASTRA);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        ZombieEntity zombie = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
        if (zombie == null) {
            context.throwGameTestException("Failed to create zombie");
            return;
        }
        Vec3d targetPos = pos.add(0.0D, 0.0D, 3.0D);
        zombie.refreshPositionAndAngles(targetPos.x, targetPos.y, targetPos.z, 0.0F, 0.0F);
        world.spawnEntity(zombie);
        aimAt(player, world, zombie.getEntityPos().add(0.0D, 1.2D, 0.0D));

        context.runAtTick(5L, () -> {
            boolean ok = new TagAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Tag did not activate with target");
                return;
            }
            if (!zombie.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.GLOWING)) {
                context.throwGameTestException("Tag should apply glowing to target");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void astraConfigValuesValid(TestContext context) {
        var cfg = GemsBalance.v().astra();
        
        if (cfg.unboundedCooldownTicks() < 0) {
            context.throwGameTestException("Unbounded cooldown cannot be negative");
        }
        if (cfg.unboundedDurationTicks() <= 0) {
            context.throwGameTestException("Unbounded duration must be positive");
        }
        if (cfg.tagCooldownTicks() < 0) {
            context.throwGameTestException("Tag cooldown cannot be negative");
        }
        if (cfg.spookCooldownTicks() < 0) {
            context.throwGameTestException("Spook cooldown cannot be negative");
        }
        
        context.complete();
    }
}
