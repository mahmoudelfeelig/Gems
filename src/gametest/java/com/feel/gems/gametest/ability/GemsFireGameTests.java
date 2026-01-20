package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.fire.CosyCampfireAbility;
import com.feel.gems.power.ability.fire.FireballAbility;
import com.feel.gems.power.ability.fire.HeatHazeZoneAbility;
import com.feel.gems.power.ability.fire.MeteorShowerAbility;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.trust.GemTrust;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;




public final class GemsFireGameTests {

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
    public void fireballLaunchesProjectile(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FIRE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new FireballAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Fireball did not activate");
                return;
            }
        });

        context.runAtTick(6L, () -> new FireballAbility().activate(player));

        context.runAtTick(15L, () -> {
            Box box = new Box(player.getBlockPos()).expand(12.0D);
            int found = world.getEntitiesByClass(FireballEntity.class, box, e -> true).size();
            if (found == 0) {
                context.throwGameTestException("Expected a fireball entity to spawn");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void cosyCampfireHealsAllies(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity ally = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(ally);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(ally, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FIRE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        GemTrust.trust(player, ally.getUuid());
        ally.setHealth(10.0F);

        context.runAtTick(5L, () -> {
            boolean ok = new CosyCampfireAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Cosy Campfire did not activate");
                return;
            }
        });

        context.runAtTick(25L, () -> {
            AbilityRuntime.tickEverySecond(player);
            if (!ally.hasStatusEffect(StatusEffects.REGENERATION)) {
                context.throwGameTestException("Cosy Campfire should grant regeneration to trusted allies");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void heatHazeZoneAppliesBuffsAndDebuffs(TestContext context) {
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
        GemPlayerState.setActiveGem(player, GemId.FIRE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        GemTrust.trust(player, ally.getUuid());

        context.runAtTick(5L, () -> {
            boolean ok = new HeatHazeZoneAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Heat Haze Zone did not activate");
                return;
            }
        });

        context.runAtTick(25L, () -> {
            AbilityRuntime.tickEverySecond(player);
            if (!ally.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                context.throwGameTestException("Heat Haze Zone should grant Fire Resistance to allies");
                return;
            }
            if (!enemy.hasStatusEffect(StatusEffects.MINING_FATIGUE) || !enemy.hasStatusEffect(StatusEffects.WEAKNESS)) {
                context.throwGameTestException("Heat Haze Zone should debuff enemies");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 300)
    public void meteorShowerRainsFireballs(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        // Create a target area
        Vec3d targetPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 5.5D));
        // Ensure the ability's block raycast has something to hit at the target location.
        BlockPos targetBlock = BlockPos.ofFloored(targetPos.x, targetPos.y - 1.0D, targetPos.z);
        world.setBlockState(targetBlock, Blocks.STONE.getDefaultState());
        var target = EntityType.ARMOR_STAND.create(world, e -> {}, BlockPos.ofFloored(targetPos), SpawnReason.TRIGGERED, false, false);
        if (target != null) {
            target.refreshPositionAndAngles(targetPos.x, targetPos.y, targetPos.z, 0.0F, 0.0F);
            target.setNoGravity(true);
            world.spawnEntity(target);
        }

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FIRE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);
        aimAt(player, world, Vec3d.ofCenter(targetBlock));

        context.runAtTick(5L, () -> {
            boolean ok = new MeteorShowerAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Meteor Shower did not activate");
                return;
            }
        });

        // Meteors are fast and typically impact/despawn well before tick 40; validate shortly after activation.
        context.runAtTick(10L, () -> {
            Box box = new Box(BlockPos.ofFloored(targetPos)).expand(16.0D, 30.0D, 16.0D);
            int expected = GemsBalance.v().fire().meteorShowerCount();
            int found = world.getEntitiesByClass(FireballEntity.class, box, e -> e.getCommandTags().contains("gems_meteor")).size();
            if (found < expected) {
                context.throwGameTestException("Expected at least " + expected + " meteors, found " + found);
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fireAutoEnchantAppliesFireAspect(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FIRE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, sword);

        context.runAtTick(5L, () -> {
            GemPowers.maintain(player);
            var fireAspect = world.getRegistryManager().getOptionalEntry(Enchantments.FIRE_ASPECT).orElseThrow();
            int level = EnchantmentHelper.getLevel(fireAspect, sword);
            if (level <= 0) {
                context.throwGameTestException("Auto-enchant Fire Aspect should apply to melee weapons");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fireAutoSmeltDropsSmeltedItems(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        BlockPos blockPos = BlockPos.ofFloored(pos);
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FIRE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        context.runAtTick(5L, () -> {
            var drops = Block.getDroppedStacks(Blocks.IRON_ORE.getDefaultState(), world, blockPos, null, player, tool);
            boolean hasIngot = drops.stream().anyMatch(stack -> stack.isOf(Items.IRON_INGOT));
            if (!hasIngot) {
                context.throwGameTestException("Auto-smelt should convert ore drops into ingots");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fireballHasCooldownConfigured(TestContext context) {
        // Direct activate() bypasses cooldown system (handled by GemAbilities.activateByIndex)
        // Just verify the cooldown config is valid
        var cfg = GemsBalance.v().fire();
        if (cfg.fireballInternalCooldownTicks() <= 0) {
            context.throwGameTestException("Fireball cooldown must be positive");
        }
        context.complete();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fireConfigValuesValid(TestContext context) {
        var cfg = GemsBalance.v().fire();
        
        if (cfg.fireballInternalCooldownTicks() < 0) {
            context.throwGameTestException("Fireball cooldown cannot be negative");
        }
        if (cfg.cosyCampfireCooldownTicks() < 0) {
            context.throwGameTestException("Cosy Campfire cooldown cannot be negative");
        }
        if (cfg.heatHazeCooldownTicks() < 0) {
            context.throwGameTestException("Heat Haze Zone cooldown cannot be negative");
        }
        if (cfg.meteorShowerCooldownTicks() < 0) {
            context.throwGameTestException("Meteor Shower cooldown cannot be negative");
        }
        
        context.complete();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fireImmunityPassiveGrantsFireResistance(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FIRE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            GemPowers.maintain(player);
            if (!player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE)) {
                context.throwGameTestException("Fire Resistance passive should apply the effect");
                return;
            }
            context.complete();
        });
    }
}
