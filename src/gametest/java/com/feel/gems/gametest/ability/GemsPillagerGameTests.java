package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.pillager.*;
import com.feel.gems.power.gem.pillager.PillagerDiscipline;
import com.feel.gems.power.gem.pillager.PillagerVolleyRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.trust.GemTrust;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public final class GemsPillagerGameTests {

    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void pillagerFangsActivates(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PILLAGER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            int before = world.getEntitiesByClass(EvokerFangsEntity.class, player.getBoundingBox().expand(8.0D), e -> true).size();
            boolean ok = new PillagerFangsAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Pillager Fangs did not activate");
                return;
            }
            int after = world.getEntitiesByClass(EvokerFangsEntity.class, player.getBoundingBox().expand(8.0D), e -> true).size();
            if (after <= before) {
                context.throwGameTestException("Pillager Fangs should spawn evoker fangs");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void pillagerSnareAppliesSlownessAndGlowing(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(target, world, pos.x, pos.y, pos.z + 4.0D, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PILLAGER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new PillagerSnareAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Pillager Snare did not activate");
                return;
            }
        });

        context.runAtTick(15L, () -> {
            if (target.getStatusEffect(StatusEffects.GLOWING) == null) {
                context.throwGameTestException("Snare should apply Glowing");
                return;
            }
            if (target.getStatusEffect(StatusEffects.SLOWNESS) == null) {
                context.throwGameTestException("Snare should apply Slowness");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void pillagerVolleyFiresArrows(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PILLAGER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new PillagerVolleyAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Pillager Volley did not activate");
                return;
            }
        });

        context.runAtTick(10L, () -> PillagerVolleyRuntime.tick(world.getServer()));

        context.runAtTick(20L, () -> {
            int arrows = world.getEntitiesByClass(ArrowEntity.class, player.getBoundingBox().expand(12.0D), e -> true).size();
            if (arrows <= 0) {
                context.throwGameTestException("Volley should fire arrows over time");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void pillagerWarhornBuffsAlliesAndDebuffsEnemies(TestContext context) {
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
        GemPlayerState.setActiveGem(player, GemId.PILLAGER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        GemTrust.trust(player, ally.getUuid());

        context.runAtTick(5L, () -> {
            boolean ok = new PillagerWarhornAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Pillager Warhorn did not activate");
                return;
            }
        });

        context.runAtTick(15L, () -> {
            if (ally.getStatusEffect(StatusEffects.SPEED) == null) {
                context.throwGameTestException("Warhorn should buff allies with Speed");
                return;
            }
            if (enemy.getStatusEffect(StatusEffects.SLOWNESS) == null) {
                context.throwGameTestException("Warhorn should slow enemies");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void pillagerRavageDamagesAndKnocksBack(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(target, world, pos.x, pos.y, pos.z + 2.0D, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PILLAGER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        float before = target.getHealth();

        context.runAtTick(5L, () -> {
            boolean ok = new PillagerRavageAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Pillager Ravage did not activate with a target");
                return;
            }
        });

        context.runAtTick(15L, () -> {
            if (target.getHealth() >= before) {
                context.throwGameTestException("Pillager Ravage should damage the target");
                return;
            }
            if (target.getVelocity().lengthSquared() <= 0.01D) {
                context.throwGameTestException("Pillager Ravage should knock back the target");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void pillagerVindicatorBreakGrantsStrength(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PILLAGER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new PillagerVindicatorBreakAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Vindicator Break did not activate");
                return;
            }
            if (player.getStatusEffect(StatusEffects.STRENGTH) == null) {
                context.throwGameTestException("Vindicator Break should grant Strength");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void pillagerRaidersTrainingBoostsProjectileVelocity(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PILLAGER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            ArrowEntity arrow = new ArrowEntity(world, player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
            arrow.setPosition(player.getX(), player.getEyeY(), player.getZ());
            arrow.setVelocity(1.0D, 0.0D, 0.0D, 1.0F, 0.0F);
            double speed = arrow.getVelocity().length();
            if (speed <= 1.0D) {
                context.throwGameTestException("Raider's Training should increase projectile velocity");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void pillagerIllagerDisciplineGrantsResistanceAtLowHealth(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PILLAGER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            player.setHealth(2.0F);
            PillagerDiscipline.tick(player);
            if (player.getStatusEffect(StatusEffects.RESISTANCE) == null) {
                context.throwGameTestException("Illager Discipline should grant Resistance at low health");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void pillagerCrossbowMasteryAutoEnchants(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PILLAGER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        player.setStackInHand(Hand.MAIN_HAND, crossbow);

        context.runAtTick(5L, () -> {
            GemPowers.maintain(player);
            var quickCharge = world.getRegistryManager().getOptionalEntry(Enchantments.QUICK_CHARGE).orElseThrow();
            int level = EnchantmentHelper.getLevel(quickCharge, crossbow);
            if (level <= 0) {
                context.throwGameTestException("Crossbow Mastery should apply Quick Charge");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void pillagerRaiderStrideGrantsSpeed(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.PILLAGER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            GemPowers.maintain(player);
            if (player.getStatusEffect(StatusEffects.SPEED) == null) {
                context.throwGameTestException("Raider's Stride should grant Speed");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void pillagerConfigValuesValid(TestContext context) {
        var cfg = GemsBalance.v().pillager();
        
        if (cfg.fangsCooldownTicks() < 0) {
            context.throwGameTestException("Fangs cooldown cannot be negative");
        }
        if (cfg.snareCooldownTicks() < 0) {
            context.throwGameTestException("Snare cooldown cannot be negative");
        }
        if (cfg.volleyCooldownTicks() < 0) {
            context.throwGameTestException("Volley cooldown cannot be negative");
        }
        
        context.complete();
    }
}
