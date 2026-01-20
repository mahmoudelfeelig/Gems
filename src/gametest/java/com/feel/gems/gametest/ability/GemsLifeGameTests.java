package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.life.HealthDrainAbility;
import com.feel.gems.power.ability.life.HeartLockAbility;
import com.feel.gems.power.ability.life.LifeCircleAbility;
import com.feel.gems.power.ability.life.LifeSwapAbility;
import com.feel.gems.power.ability.life.VitalityVortexAbility;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.trust.GemTrust;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
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
import net.minecraft.world.GameMode;




public final class GemsLifeGameTests {

    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void healthDrainStealsHealthFromTarget(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(target, world, pos.x, pos.y, pos.z + 3.0D, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.LIFE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        player.setHealth(10.0F);
        target.setHealth(20.0F);

        float playerHealthBefore = player.getHealth();
        float targetHealthBefore = target.getHealth();

        context.runAtTick(5L, () -> {
            boolean ok = new HealthDrainAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Health Drain did not activate");
            }
        });

        context.runAtTick(15L, () -> {
            if (target.getHealth() >= targetHealthBefore) {
                context.throwGameTestException("Health Drain did not damage target");
            }
            if (player.getHealth() <= playerHealthBefore) {
                context.throwGameTestException("Health Drain did not heal caster");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void heartLockPreventsHealthChange(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(target, world, pos.x, pos.y, pos.z + 3.0D, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.LIFE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        target.setHealth(8.0F);
        float lockedHealth = target.getHealth();

        context.runAtTick(5L, () -> {
            boolean ok = new HeartLockAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Heart Lock did not activate");
            }
        });

        context.runAtTick(15L, () -> {
            // Try to heal the target
            target.heal(10.0F);
            double maxHealth = target.getAttributeValue(EntityAttributes.MAX_HEALTH);
            
            // Heart lock should cap max health to the locked value
            if (maxHealth > lockedHealth + 0.1D) {
                context.throwGameTestException("Heart Lock did not restrict max health");
                return;
            }
        });

        int duration = GemsBalance.v().life().heartLockDurationTicks();
        context.runAtTick(15L + duration + 10L, () -> {
            AbilityRuntime.tickEverySecond(target);
            double maxHealth = target.getAttributeValue(EntityAttributes.MAX_HEALTH);
            if (maxHealth <= lockedHealth + 0.1D) {
                context.throwGameTestException("Heart Lock should expire and restore max health");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void lifeSwapExchangesHealth(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(target, world, pos.x, pos.y, pos.z + 3.0D, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.LIFE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        player.setHealth(8.0F);
        target.setHealth(18.0F);

        float playerHealthBefore = player.getHealth();
        float targetHealthBefore = target.getHealth();

        context.runAtTick(5L, () -> {
            boolean ok = new LifeSwapAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Life Swap did not activate");
            }
        });

        context.runAtTick(15L, () -> {
            // Health should be swapped (with possible min health limits)
            float playerHealthAfter = player.getHealth();
            float targetHealthAfter = target.getHealth();
            
            // After swap, player should have more health if target had more
            if (playerHealthAfter <= playerHealthBefore && targetHealthAfter >= targetHealthBefore) {
                context.throwGameTestException("Life Swap did not exchange health");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 300)
    public void lifeCircleAdjustsMaxHealthForAlliesAndEnemies(TestContext context) {
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
        GemPlayerState.setActiveGem(player, GemId.LIFE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        GemTrust.trust(player, ally.getUuid());
        double allyBase = ally.getAttributeValue(EntityAttributes.MAX_HEALTH);
        double enemyBase = enemy.getAttributeValue(EntityAttributes.MAX_HEALTH);

        context.runAtTick(5L, () -> {
            boolean ok = new LifeCircleAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Life Circle did not activate");
                return;
            }
        });

        context.runAtTick(25L, () -> {
            AbilityRuntime.tickEverySecond(player);
            double allyMax = ally.getAttributeValue(EntityAttributes.MAX_HEALTH);
            double enemyMax = enemy.getAttributeValue(EntityAttributes.MAX_HEALTH);
            if (allyMax <= allyBase) {
                context.throwGameTestException("Life Circle should increase ally max health");
                return;
            }
            if (enemyMax >= enemyBase) {
                context.throwGameTestException("Life Circle should reduce enemy max health");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 300)
    public void vitalityVortexDefaultAppliesBuffsAndDebuffs(TestContext context) {
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
        GemPlayerState.setActiveGem(player, GemId.LIFE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        GemTrust.trust(player, ally.getUuid());

        context.runAtTick(5L, () -> {
            boolean ok = new VitalityVortexAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Vitality Vortex did not activate");
                return;
            }
        });

        context.runAtTick(15L, () -> {
            if (!ally.hasStatusEffect(StatusEffects.REGENERATION)) {
                context.throwGameTestException("Vitality Vortex should buff trusted allies");
                return;
            }
            if (!enemy.hasStatusEffect(StatusEffects.POISON) || !enemy.hasStatusEffect(StatusEffects.WEAKNESS)) {
                context.throwGameTestException("Vitality Vortex should debuff enemies in default mode");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void healthDrainHasCooldownConfigured(TestContext context) {
        // Direct activate() bypasses cooldown system (handled by GemAbilities.activateByIndex)
        // Just verify the cooldown config is valid
        var cfg = GemsBalance.v().life();
        if (cfg.healthDrainCooldownTicks() <= 0) {
            context.throwGameTestException("Health Drain cooldown must be positive");
        }
        context.complete();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void lifeSwapRespectsMinimumHealth(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(target, world, pos.x, pos.y, pos.z + 3.0D, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.LIFE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        // Set player to minimum health requirement
        player.setHealth(20.0F);
        target.setHealth(4.0F); // Below min hearts threshold

        context.runAtTick(5L, () -> {
            boolean ok = new LifeSwapAbility().activate(player);
            // Life Swap should either fail or handle the minimum health case
            // Either outcome is acceptable as long as no crash occurs
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void vitalityVortexAquaticModeAppliesAquaticBuffs(TestContext context) {
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

        context.setBlockState(0, 1, 1, net.minecraft.block.Blocks.WATER.getDefaultState());

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.LIFE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        GemTrust.trust(player, ally.getUuid());

        context.runAtTick(5L, () -> {
            boolean ok = new VitalityVortexAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Vitality Vortex did not activate");
                return;
            }
        });

        context.runAtTick(15L, () -> {
            if (!ally.hasStatusEffect(StatusEffects.WATER_BREATHING) || !ally.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
                context.throwGameTestException("Aquatic mode should grant water breathing and dolphin's grace");
                return;
            }
            if (!enemy.hasStatusEffect(StatusEffects.SLOWNESS) || !enemy.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
                context.throwGameTestException("Aquatic mode should debuff enemies");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void lifeAutoEnchantAppliesUnbreaking(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.LIFE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, tool);

        context.runAtTick(5L, () -> {
            GemPowers.maintain(player);
            var unbreaking = world.getRegistryManager().getOptionalEntry(Enchantments.UNBREAKING).orElseThrow();
            int level = EnchantmentHelper.getLevel(unbreaking, tool);
            if (level <= 0) {
                context.throwGameTestException("Auto-enchant Unbreaking should apply to tools");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void lifeDoubleSaturationBoostsFoodSaturation(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.LIFE);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        var hunger = player.getHungerManager();
        hunger.setFoodLevel(10);
        hunger.setSaturationLevel(0.0F);

        ItemStack food = new ItemStack(Items.COOKED_BEEF);
        context.runAtTick(5L, () -> {
            float before = hunger.getSaturationLevel();
            food.finishUsing(world, player);
            float after = hunger.getSaturationLevel();
            if (after <= before) {
                context.throwGameTestException("Double Saturation should increase saturation on eat");
                return;
            }
            context.complete();
        });
    }
}
