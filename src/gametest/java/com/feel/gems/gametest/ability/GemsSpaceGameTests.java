package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.space.*;
import com.feel.gems.power.gem.space.SpaceAnomalies;
import com.feel.gems.power.gem.space.SpaceLunarScaling;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class GemsSpaceGameTests {

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
    public void spaceGravityFieldActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity enemy = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(enemy);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(enemy, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPACE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new SpaceGravityFieldAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Gravity Field did not activate");
            }
            double before = enemy.getAttributeValue(EntityAttributes.GRAVITY);
            AbilityRuntime.tickEverySecond(player);
            double after = enemy.getAttributeValue(EntityAttributes.GRAVITY);
            if (before == after) {
                context.throwGameTestException("Gravity Field should modify gravity for nearby enemies");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void spaceBlackHoleActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        GemsGameTestUtil.placeStoneFloor(context, 10);
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPACE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        ZombieEntity target = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
        if (target == null) {
            context.throwGameTestException("Failed to create zombie");
            return;
        }
        target.refreshPositionAndAngles(pos.x + 3.0D, pos.y, pos.z, 0.0F, 0.0F);
        // Disable AI and gravity so the zombie doesn't walk toward player or fall,
        // allowing us to isolate the black hole's pull effect on velocity.
        target.setAiDisabled(true);
        target.setNoGravity(true);
        world.spawnEntity(target);
        Vec3d center = player.getEntityPos().add(0.0D, 0.2D, 0.0D);

        context.runAtTick(5L, () -> {
            boolean ok = new SpaceBlackHoleAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Black Hole did not activate");
            }
        });

        // Tick the anomaly system continuously. Physics runs when world time >= nextPhysicsTick
        // (which is ~20 ticks after spawn). We tick frequently to ensure the stride-based
        // physics logic fires and accumulates velocity on the target.
        for (long t = 6L; t <= 50L; t++) {
            context.runAtTick(t, () -> SpaceAnomalies.tick(world.getServer()));
        }

        context.runAtTick(55L, () -> {
            Vec3d delta = center.subtract(target.getEntityPos());
            Vec3d velocity = target.getVelocity();
            // The black hole should have applied inward velocity toward the center.
            // Check that velocity has a positive component toward the center (dot product > 0).
            if (velocity.lengthSquared() < 0.0001D || velocity.dotProduct(delta) <= 0.0D) {
                context.throwGameTestException("Black Hole should pull targets inward");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void spaceWhiteHoleActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPACE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        ZombieEntity target = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
        if (target == null) {
            context.throwGameTestException("Failed to create zombie");
            return;
        }
        target.refreshPositionAndAngles(pos.x + 2.5D, pos.y, pos.z, 0.0F, 0.0F);
        world.spawnEntity(target);
        Vec3d center = player.getEntityPos().add(0.0D, 0.2D, 0.0D);

        context.runAtTick(5L, () -> {
            boolean ok = new SpaceWhiteHoleAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("White Hole did not activate");
            }
        });

        context.runAtTick(30L, () -> {
            SpaceAnomalies.tick(world.getServer());
            Vec3d delta = target.getEntityPos().subtract(center);
            if (target.getVelocity().dotProduct(delta) <= 0.0D) {
                context.throwGameTestException("White Hole should push targets outward");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void spaceOrbitalLaserActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        // Keep a minimal platform (ray targeting is sensitive to nearby blocks in GameTest).
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                context.setBlockState(x, 1, z, Blocks.STONE.getDefaultState());
            }
        }
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPACE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        BlockPos target = new BlockPos((int) pos.x, (int) pos.y, (int) pos.z + 6);
        world.setBlockState(target, Blocks.STONE.getDefaultState());
        ZombieEntity victim = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
        if (victim == null) {
            context.throwGameTestException("Failed to create zombie");
            return;
        }
        victim.refreshPositionAndAngles(target.getX() + 0.5D, target.getY() + 1.0D, target.getZ() + 0.5D, 0.0F, 0.0F);
        victim.setAiDisabled(true);
        victim.setNoGravity(true);
        world.spawnEntity(victim);
        aimAt(player, world, Vec3d.ofCenter(target));

        float[] before = new float[1];
        context.runAtTick(4L, () -> before[0] = victim.getHealth());

        context.runAtTick(5L, () -> {
            player.setSneaking(false);
            boolean ok = new SpaceOrbitalLaserAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Orbital Laser did not activate");
            }
        });

        int delay = Math.max(1, GemsBalance.v().space().orbitalLaserDelayTicks());
        context.runAtTick(5L + delay + 5L, () -> {
            if (victim.getHealth() >= before[0]) {
                context.throwGameTestException("Orbital Laser should damage targets at the strike location");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void spacePassivesApply(TestContext context) {
        ServerWorld world = context.getWorld();
        GemsGameTestUtil.placeStoneFloor(context, 10);
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.SPACE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            GemPowers.maintain(player);
            if (!player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
                context.throwGameTestException("Low Gravity should apply slow falling");
            }
            world.setTimeOfDay(0L);
            float full = SpaceLunarScaling.multiplier(world);
            world.setTimeOfDay(4L * 24000L);
            float newMoon = SpaceLunarScaling.multiplier(world);
            if (full <= newMoon) {
                context.throwGameTestException("Lunar Scaling should be stronger at full moon");
            }

            world.setTimeOfDay(18000L);
            SkeletonEntity shooter = EntityType.SKELETON.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
            if (shooter == null) {
                context.throwGameTestException("Failed to create skeleton");
                return;
            }
            shooter.refreshPositionAndAngles(player.getX() + 2.0D, player.getY(), player.getZ(), 0.0F, 0.0F);
            shooter.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, new ItemStack(Items.BOW));
            world.spawnEntity(shooter);
            net.minecraft.entity.projectile.ArrowEntity arrow = new net.minecraft.entity.projectile.ArrowEntity(
                    world,
                    shooter,
                    new ItemStack(Items.ARROW),
                    shooter.getMainHandStack()
            );
            float before = player.getHealth();
            player.damage(world, player.getDamageSources().arrow(arrow, shooter), 6.0F);
            float delta = before - player.getHealth();
            float max = 6.0F * GemsBalance.v().space().starshieldProjectileDamageMultiplier() + 0.1F;
            if (delta > max) {
                context.throwGameTestException("Starshield should reduce projectile damage at night");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void spaceConfigValuesValid(TestContext context) {
        var cfg = GemsBalance.v().space();
        
        if (cfg.gravityFieldCooldownTicks() < 0) {
            context.throwGameTestException("Gravity Field cooldown cannot be negative");
        }
        if (cfg.blackHoleCooldownTicks() < 0) {
            context.throwGameTestException("Black Hole cooldown cannot be negative");
        }
        if (cfg.whiteHoleCooldownTicks() < 0) {
            context.throwGameTestException("White Hole cooldown cannot be negative");
        }
        
        context.complete();
    }
}
