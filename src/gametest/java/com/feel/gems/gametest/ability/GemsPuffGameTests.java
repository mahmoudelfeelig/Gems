package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.puff.*;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.trust.GemTrust;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Blocks;
import net.minecraft.block.SculkSensorBlock;
import net.minecraft.block.entity.SculkSensorBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.event.GameEvent;

public final class GemsPuffGameTests {

    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void puffDoubleJumpLaunchesPlayer(TestContext context) {
        ServerWorld world = context.getWorld();
        GemsGameTestUtil.placeStoneFloor(context, 6);
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PUFF);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(25L, () -> {
            teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
            player.setVelocity(Vec3d.ZERO);
        });

        context.runAtTick(60L, () -> {
            BlockPos below = player.getBlockPos().down();
            if (world.getBlockState(below).isAir()) {
                context.throwGameTestException("Test player is not standing on a solid block");
                return;
            }
            boolean ok = new DoubleJumpAbility().activate(player);
            if (ok) {
                context.throwGameTestException("Double Jump should fail while on ground");
                return;
            }
        });

        context.runAtTick(70L, () -> {
            // The "empty" GameTest structure has a low ceiling; teleporting high up can clamp back to the floor.
            // Instead, create a temporary hole under the player so Double Jump is guaranteed to be "midair".
            BlockPos below = BlockPos.ofFloored(pos.x, pos.y - 1.0D, pos.z);
            world.setBlockState(below, Blocks.AIR.getDefaultState());
            teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
            boolean ok = new DoubleJumpAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Double Jump did not activate");
                return;
            }
        });

        context.runAtTick(90L, () -> {
            Vec3d vel = player.getVelocity();
            if (vel.y < 0.0D) {
                context.throwGameTestException("Double Jump should give upward velocity");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void puffDashDamagesEnemy(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PUFF);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        ZombieEntity zombie = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
        if (zombie == null) {
            context.throwGameTestException("Failed to create zombie");
            return;
        }
        zombie.refreshPositionAndAngles(pos.x, pos.y, pos.z + 2.0D, 180.0F, 0.0F);
        world.spawnEntity(zombie);
        float before = zombie.getHealth();

        context.runAtTick(5L, () -> {
            boolean ok = new DashAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Puff Dash did not activate");
                return;
            }
        });

        context.runAtTick(15L, () -> {
            if (zombie.getHealth() >= before) {
                context.throwGameTestException("Dash should damage enemies in the path");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void puffGustAppliesSlowness(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity enemy = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(enemy);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(enemy, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PUFF);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new PuffGustAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Puff Gust did not activate");
                return;
            }
        });

        context.runAtTick(15L, () -> {
            if (!enemy.hasStatusEffect(StatusEffects.SLOWNESS)) {
                context.throwGameTestException("Gust should apply slowness to enemies");
                return;
            }
            if (!player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
                context.throwGameTestException("Gust should apply slow falling to the caster");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void puffBreezyBashLaunchesTarget(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PUFF);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            ZombieEntity zombie = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
            if (zombie == null) {
                context.throwGameTestException("Failed to create zombie");
                return;
            }
            zombie.refreshPositionAndAngles(pos.x, pos.y, pos.z + 2.5D, 180.0F, 0.0F);
            world.spawnEntity(zombie);
            boolean ok = new BreezyBashAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Breezy Bash did not activate");
                return;
            }
            if (zombie.getVelocity().y <= 0.0D) {
                context.throwGameTestException("Breezy Bash should launch the target upward");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void puffGroupBreezyBashActivates(TestContext context) {
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
        GemPlayerState.setActiveGem(player, GemId.PUFF);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        GemTrust.trust(player, ally.getUuid());

        context.runAtTick(5L, () -> {
            boolean ok = new GroupBreezyBashAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Group Breezy Bash did not activate");
                return;
            }
        });

        context.runAtTick(15L, () -> {
            if (enemy.getVelocity().lengthSquared() <= 1.0E-4D) {
                context.throwGameTestException("Group Breezy Bash should knock back untrusted players");
                return;
            }
            if (ally.getVelocity().lengthSquared() > 1.0E-4D) {
                context.throwGameTestException("Group Breezy Bash should not affect trusted allies");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void puffConfigValuesValid(TestContext context) {
        var cfg = GemsBalance.v().puff();
        
        if (cfg.doubleJumpCooldownTicks() < 0) {
            context.throwGameTestException("Double Jump cooldown cannot be negative");
        }
        if (cfg.dashCooldownTicks() < 0) {
            context.throwGameTestException("Dash cooldown cannot be negative");
        }
        if (cfg.gustCooldownTicks() < 0) {
            context.throwGameTestException("Gust cooldown cannot be negative");
        }
        
        context.complete();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void puffPassivesApplyEnchantsAndWindborne(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 6.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PUFF);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        ItemStack bow = new ItemStack(Items.BOW);
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, bow);

        context.runAtTick(5L, () -> {
            GemPowers.maintain(player);
            int power = EnchantmentHelper.getLevel(world.getRegistryManager().getOptionalEntry(Enchantments.POWER).orElseThrow(), bow);
            int punch = EnchantmentHelper.getLevel(world.getRegistryManager().getOptionalEntry(Enchantments.PUNCH).orElseThrow(), bow);
            if (power <= 0 || punch <= 0) {
                context.throwGameTestException("Auto-enchant Power and Punch should apply to bows");
                return;
            }
            if (!player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
                context.throwGameTestException("Windborne should grant slow falling while airborne");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void puffFallDamageImmunityPreventsDamage(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 6.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PUFF);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        float before = player.getHealth();
        context.runAtTick(5L, () -> {
            player.handleFallDamage(10.0D, 1.0F, player.getDamageSources().fall());
            if (player.getHealth() < before) {
                context.throwGameTestException("Fall damage immunity should prevent damage");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void puffCropTrampleImmunityKeepsFarmland(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        context.setBlockState(0, 1, 0, Blocks.FARMLAND.getDefaultState());
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 3.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PUFF);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        BlockPos farmlandPos = BlockPos.ofFloored(context.getAbsolute(new Vec3d(0.0D, 1.0D, 0.0D)));

        context.runAtTick(5L, () -> {
            Blocks.FARMLAND.onLandedUpon(world, world.getBlockState(farmlandPos), farmlandPos, player, 6.0D);
            if (!world.getBlockState(farmlandPos).isOf(Blocks.FARMLAND)) {
                context.throwGameTestException("Crop trample immunity should keep farmland intact");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void puffSculkSilencePreventsSensorTrigger(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        context.setBlockState(0, 1, 0, Blocks.SCULK_SENSOR.getDefaultState());
        BlockPos pos = BlockPos.ofFloored(context.getAbsolute(new Vec3d(0.0D, 1.0D, 0.0D)));
        SculkSensorBlockEntity sensor = (SculkSensorBlockEntity) world.getBlockEntity(pos);
        if (sensor == null) {
            context.throwGameTestException("Failed to create sculk sensor block entity");
            return;
        }

        Vec3d playerPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 2.5D));
        teleport(player, world, playerPos.x, playerPos.y, playerPos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PUFF);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            world.emitGameEvent(GameEvent.STEP, player.getEntityPos(), GameEvent.Emitter.of(player));
        });

        context.runAtTick(15L, () -> {
            if (!SculkSensorBlock.isInactive(world.getBlockState(pos))) {
                context.throwGameTestException("Sculk Silence should block sculk sensor activation");
                return;
            }
            context.complete();
        });
    }
}
