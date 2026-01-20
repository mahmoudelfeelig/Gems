package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.air.AirCrosswindAbility;
import com.feel.gems.power.ability.air.AirDashAbility;
import com.feel.gems.power.ability.air.AirGaleSlamAbility;
import com.feel.gems.power.ability.air.AirWindJumpAbility;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public final class GemsAirGameTests {

    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    private static ServerPlayerEntity setupAirPlayer(TestContext context, Vec3d pos, float yaw, float pitch) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        teleport(player, world, pos.x, pos.y, pos.z, yaw, pitch);
        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.AIR);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);
        return player;
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void airWindJumpLaunchesPlayer(TestContext context) {
        GemsGameTestUtil.placeStoneFloor(context, 6);
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity player = setupAirPlayer(context, pos, 0.0F, 0.0F);
        Vec3d start = player.getEntityPos();

        context.runAtTick(5L, () -> {
            boolean ok = new AirWindJumpAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Wind Jump did not activate");
            }
        });

        context.runAtTick(7L, () -> {
            Vec3d vel = player.getVelocity();
            if (vel.y <= 0.05D) {
                context.throwGameTestException("Wind Jump did not launch player upward");
            }
            double forward = GemsBalance.v().air().windJumpForwardVelocity();
            if (forward > 0.0D && vel.horizontalLengthSquared() <= 0.01D) {
                context.throwGameTestException("Wind Jump did not apply forward velocity");
            }
        });

        context.runAtTick(15L, () -> {
            Vec3d now = player.getEntityPos();
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void airDashGrantsResistanceAndVelocity(TestContext context) {
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity player = setupAirPlayer(context, pos, 0.0F, 0.0F);

        context.runAtTick(5L, () -> {
            boolean ok = new AirDashAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Air Dash did not activate");
            }
        });

        context.runAtTick(15L, () -> {
            Vec3d vel = player.getVelocity();
            if (vel.lengthSquared() <= 0.05D) {
                context.throwGameTestException("Air Dash did not apply velocity");
            }
            int iFrameTicks = GemsBalance.v().air().dashIFrameDurationTicks();
            if (iFrameTicks > 0 && player.getStatusEffect(StatusEffects.RESISTANCE) == null) {
                context.throwGameTestException("Air Dash should grant Resistance during i-frame window");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void airCrosswindDamagesAndSlowsEnemy(TestContext context) {
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity player = setupAirPlayer(context, pos, 0.0F, 0.0F);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(target);
        teleport(target, context.getWorld(), pos.x + 3.0D, pos.y, pos.z, 180.0F, 0.0F);
        float healthBefore = target.getHealth();

        context.runAtTick(5L, () -> {
            boolean ok = new AirCrosswindAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Crosswind did not activate");
            }
        });

        context.runAtTick(15L, () -> {
            if (target.getHealth() >= healthBefore) {
                context.throwGameTestException("Crosswind did not damage the enemy");
            }
            if (target.getStatusEffect(StatusEffects.SLOWNESS) == null) {
                context.throwGameTestException("Crosswind should apply Slowness to enemies");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void airGaleSlamDamagesNearbyTargets(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 3.0D, 0.5D));
        ServerPlayerEntity player = setupAirPlayer(context, pos, 0.0F, 0.0F);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity bystander = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(target);
        GemsGameTestUtil.forceSurvival(bystander);
        teleport(target, world, pos.x, pos.y, pos.z + 2.0D, 180.0F, 0.0F);
        teleport(bystander, world, pos.x + 1.5D, pos.y, pos.z + 2.0D, 180.0F, 0.0F);

        player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.MACE));
        float bystanderBefore = bystander.getHealth();

        context.runAtTick(5L, () -> {
            boolean ok = new AirGaleSlamAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Gale Slam did not activate");
            }
        });

        context.runAtTick(10L, () -> player.attack(target));

        context.runAtTick(20L, () -> {
            if (bystander.getHealth() >= bystanderBefore) {
                context.throwGameTestException("Gale Slam should damage nearby targets on hit");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void airMacePassiveGrantsEnchantedMace(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity player = setupAirPlayer(context, pos, 0.0F, 0.0F);

        context.runAtTick(5L, () -> {
            GemPowers.maintain(player);
            if (!GemsGameTestUtil.containsAirMace(player)) {
                context.throwGameTestException("Windburst Mace passive should grant an Air Mace");
                return;
            }
            ItemStack mace = ItemStack.EMPTY;
            var inv = player.getInventory();
            for (int i = 0; i < inv.size(); i++) {
                ItemStack stack = inv.getStack(i);
                if (stack.isOf(Items.MACE)) {
                    mace = stack;
                    break;
                }
            }
            if (mace.isEmpty()) {
                context.throwGameTestException("Expected player to have an Air Mace");
                return;
            }
            var breachEntry = world.getRegistryManager().getOptionalEntry(Enchantments.BREACH).orElseThrow();
            var windBurstEntry = world.getRegistryManager().getOptionalEntry(Enchantments.WIND_BURST).orElseThrow();
            var fireAspectEntry = world.getRegistryManager().getOptionalEntry(Enchantments.FIRE_ASPECT).orElseThrow();
            int breach = EnchantmentHelper.getLevel(breachEntry, mace);
            int windBurst = EnchantmentHelper.getLevel(windBurstEntry, mace);
            int fireAspect = EnchantmentHelper.getLevel(fireAspectEntry, mace);
            var cfg = GemsBalance.v().air();
            if (cfg.airMaceBreachLevel() > 0 && breach < cfg.airMaceBreachLevel()) {
                context.throwGameTestException("Air mace should have Breach enchantment");
                return;
            }
            if (cfg.airMaceWindBurstLevel() > 0 && windBurst < cfg.airMaceWindBurstLevel()) {
                context.throwGameTestException("Air mace should have Wind Burst enchantment");
                return;
            }
            if (cfg.airMaceFireAspectLevel() > 0 && fireAspect < cfg.airMaceFireAspectLevel()) {
                context.throwGameTestException("Air mace should have Fire Aspect enchantment");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void airWindShearAddsSlownessOnHit(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity player = setupAirPlayer(context, pos, 0.0F, 0.0F);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(target);
        teleport(target, world, pos.x, pos.y, pos.z + 2.0D, 180.0F, 0.0F);

        player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.MACE));

        context.runAtTick(5L, () -> player.attack(target));

        context.runAtTick(15L, () -> {
            if (target.getStatusEffect(StatusEffects.SLOWNESS) == null) {
                context.throwGameTestException("Wind Shear should apply Slowness on hit");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void airAerialGuardReducesDamage(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity guard = setupAirPlayer(context, pos, 0.0F, 0.0F);
        guard.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.MACE));

        ServerPlayerEntity control = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(control);
        teleport(control, world, pos.x + 3.0D, pos.y, pos.z, 180.0F, 0.0F);

        ServerPlayerEntity attacker = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(attacker);
        teleport(attacker, world, pos.x + 1.5D, pos.y, pos.z + 3.0D, 0.0F, 0.0F);

        context.runAtTick(5L, () -> {
            float guardBefore = guard.getHealth();
            float controlBefore = control.getHealth();
            guard.damage(world, attacker.getDamageSources().playerAttack(attacker), 6.0F);
            control.damage(world, attacker.getDamageSources().playerAttack(attacker), 6.0F);
            float guardTaken = guardBefore - guard.getHealth();
            float controlTaken = controlBefore - control.getHealth();
            if (guardTaken >= controlTaken) {
                context.throwGameTestException("Aerial Guard should reduce incoming damage");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void airAerialGuardReducesKnockback(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        ServerPlayerEntity guard = setupAirPlayer(context, pos, 0.0F, 0.0F);
        guard.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.MACE));

        ServerPlayerEntity control = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(control);
        teleport(control, world, pos.x + 3.0D, pos.y, pos.z, 180.0F, 0.0F);

        context.runAtTick(5L, () -> {
            guard.setVelocity(Vec3d.ZERO);
            control.setVelocity(Vec3d.ZERO);
            guard.takeKnockback(1.0F, 1.0D, 0.0D);
            control.takeKnockback(1.0F, 1.0D, 0.0D);
        });

        context.runAtTick(10L, () -> {
            double guardKb = guard.getVelocity().lengthSquared();
            double controlKb = control.getVelocity().lengthSquared();
            if (guardKb >= controlKb) {
                context.throwGameTestException("Aerial Guard should reduce knockback while holding the mace");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void airAerialGuardReducesFallDamage(TestContext context) {
        ServerWorld world = context.getWorld();
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 6.0D, 0.5D));
        ServerPlayerEntity guard = setupAirPlayer(context, pos, 0.0F, 0.0F);
        guard.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.MACE));

        ServerPlayerEntity control = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(control);
        teleport(control, world, pos.x + 3.0D, pos.y, pos.z, 180.0F, 0.0F);

        context.runAtTick(5L, () -> {
            float guardBefore = guard.getHealth();
            float controlBefore = control.getHealth();
            guard.handleFallDamage(10.0D, 1.0F, guard.getDamageSources().fall());
            control.handleFallDamage(10.0D, 1.0F, control.getDamageSources().fall());
            float guardTaken = guardBefore - guard.getHealth();
            float controlTaken = controlBefore - control.getHealth();
            if (guardTaken >= controlTaken) {
                context.throwGameTestException("Aerial Guard should reduce fall damage");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void airConfigValuesValid(TestContext context) {
        var cfg = GemsBalance.v().air();

        if (cfg.windJumpCooldownTicks() < 0) {
            context.throwGameTestException("Wind Jump cooldown cannot be negative");
        }
        if (cfg.dashCooldownTicks() < 0) {
            context.throwGameTestException("Dash cooldown cannot be negative");
        }
        if (cfg.crosswindCooldownTicks() < 0) {
            context.throwGameTestException("Crosswind cooldown cannot be negative");
        }
        if (cfg.galeSlamCooldownTicks() < 0) {
            context.throwGameTestException("Gale Slam cooldown cannot be negative");
        }

        context.complete();
    }
}
