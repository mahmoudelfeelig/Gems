package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.flux.*;
import com.feel.gems.power.gem.flux.FluxCharge;
import com.feel.gems.power.passive.flux.FluxCapacitorPassive;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.trust.GemTrust;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.effect.StatusEffects;

public final class GemsFluxGameTests {

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
    public void fluxBeamRequiresTarget(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new FluxBeamAbility().activate(player);
            if (ok) {
                context.throwGameTestException("Flux Beam should fail without target");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fluxBeamDamagesEnemyAndConsumesCharge(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        ZombieEntity zombie = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
        if (zombie == null) {
            context.throwGameTestException("Failed to create zombie");
            return;
        }
        zombie.refreshPositionAndAngles(pos.x, pos.y, pos.z + 3.0D, 180.0F, 0.0F);
        world.spawnEntity(zombie);
        aimAt(player, world, zombie.getEntityPos().add(0.0D, 1.2D, 0.0D));

        FluxCharge.set(player, 100);
        float before = zombie.getHealth();

        context.runAtTick(5L, () -> {
            boolean ok = new FluxBeamAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Flux Beam did not activate");
                return;
            }
            if (zombie.getHealth() >= before) {
                context.throwGameTestException("Flux Beam should damage the target");
                return;
            }
            if (FluxCharge.get(player) != 0) {
                context.throwGameTestException("Flux Beam should consume charge");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fluxBeamRepairsTrustedAllyArmor(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity ally = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(ally);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(ally, world, pos.x, pos.y, pos.z + 3.0D, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        GemTrust.trust(player, ally.getUuid());

        ItemStack armor = new ItemStack(Items.IRON_CHESTPLATE);
        armor.setDamage(armor.getMaxDamage() / 2);
        ally.equipStack(EquipmentSlot.CHEST, armor);
        int before = armor.getDamage();

        FluxCharge.set(player, 120);
        aimAt(player, world, ally.getEntityPos().add(0.0D, 1.2D, 0.0D));

        context.runAtTick(5L, () -> {
            boolean ok = new FluxBeamAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Flux Beam did not activate");
                return;
            }
            int after = ally.getEquippedStack(EquipmentSlot.CHEST).getDamage();
            if (after >= before) {
                context.throwGameTestException("Flux Beam should repair trusted ally armor");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fluxDischargeRequiresCharge(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);
        FluxCharge.set(player, 0);

        context.runAtTick(5L, () -> {
            boolean ok = new FluxDischargeAbility().activate(player);
            int minCharge = GemsBalance.v().flux().fluxDischargeMinCharge();
            if (minCharge > 0 && ok) {
                context.throwGameTestException("Discharge should fail without enough charge");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fluxDischargeDamagesAndResetsCharge(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        ZombieEntity zombie = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
        if (zombie == null) {
            context.throwGameTestException("Failed to create zombie");
            return;
        }
        zombie.refreshPositionAndAngles(pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
        world.spawnEntity(zombie);
        float before = zombie.getHealth();

        FluxCharge.set(player, Math.max(GemsBalance.v().flux().fluxDischargeMinCharge(), 50));

        context.runAtTick(5L, () -> {
            boolean ok = new FluxDischargeAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Flux Discharge did not activate");
                return;
            }
            if (zombie.getHealth() >= before) {
                context.throwGameTestException("Flux Discharge should damage targets");
                return;
            }
            if (FluxCharge.get(player) != 0) {
                context.throwGameTestException("Flux Discharge should reset charge to 0");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fluxSurgeRequiresCharge(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);
        FluxCharge.set(player, 0); // No charge

        context.runAtTick(5L, () -> {
            // Surge requires charge - should fail without enough
            boolean ok = new FluxSurgeAbility().activate(player);
            int cost = GemsBalance.v().flux().fluxSurgeChargeCost();
            if (cost > 0 && ok) {
                context.throwGameTestException("Flux Surge should fail without enough charge");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fluxSurgeAppliesBuffsAndKnockback(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity enemy = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(enemy);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(enemy, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        FluxCharge.set(player, Math.max(GemsBalance.v().flux().fluxSurgeChargeCost(), 50));

        context.runAtTick(5L, () -> {
            boolean ok = new FluxSurgeAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Flux Surge did not activate");
                return;
            }
            if (!player.hasStatusEffect(StatusEffects.SPEED)) {
                context.throwGameTestException("Flux Surge should grant speed");
                return;
            }
            if (enemy.getVelocity().lengthSquared() <= 1.0E-4D) {
                context.throwGameTestException("Flux Surge should knock back nearby enemies");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fluxStaticBurstRequiresStoredDamage(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            // StaticBurst requires stored damage from damage taken - should fail without any
            boolean ok = new StaticBurstAbility().activate(player);
            if (ok) {
                context.throwGameTestException("Static Burst should fail without stored damage");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fluxStaticBurstDamagesNearbyTargets(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity enemy = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(enemy);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(enemy, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        float before = enemy.getHealth();
        StaticBurstAbility.onDamaged(player, 6.0F);

        context.runAtTick(5L, () -> {
            boolean ok = new StaticBurstAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Static Burst did not activate");
                return;
            }
            if (enemy.getHealth() >= before) {
                context.throwGameTestException("Static Burst should damage nearby enemies");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fluxChargeTracking(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            FluxCharge.set(player, 50);
            int charge = FluxCharge.get(player);
            if (charge != 50) {
                context.throwGameTestException("Flux charge should be 50, got " + charge);
            }
            FluxCharge.set(player, 75);
            charge = FluxCharge.get(player);
            if (charge != 75) {
                context.throwGameTestException("Flux charge should be 75, got " + charge);
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fluxChargeStorageConsumesFuel(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        player.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.DIAMOND_BLOCK));
        context.runAtTick(5L, () -> {
            boolean ok = FluxCharge.tryConsumeChargeItem(player);
            if (!ok) {
                context.throwGameTestException("Flux charge storage did not consume fuel");
                return;
            }
            if (FluxCharge.get(player) <= 0) {
                context.throwGameTestException("Flux charge should increase after consuming fuel");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 300)
    public void fluxOverchargeRampIncreasesCharge(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        // Overcharge only starts once charge reaches 100, and it starts counting from when 100 was reached.
        // Set to 100 and tick once to establish the "at 100" timestamp.
        FluxCharge.set(player, 100);
        context.runAtTick(5L, () -> FluxCharge.tickOvercharge(player));

        int delay = GemsBalance.v().flux().overchargeDelayTicks();
        int[] baseline = new int[1];
        context.runAtTick(5L + delay + 5L, () -> baseline[0] = FluxCharge.get(player));
        context.runAtTick(5L + delay + 30L, () -> {
            FluxCharge.tickOvercharge(player);
            int after = FluxCharge.get(player);
            if (after <= baseline[0]) {
                context.throwGameTestException("Overcharge ramp should increase charge");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fluxCapacitorGrantsAbsorption(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        FluxCharge.set(player, GemsBalance.v().flux().fluxCapacitorChargeThreshold());
        new FluxCapacitorPassive().maintain(player);

        context.runAtTick(5L, () -> {
            if (!player.hasStatusEffect(StatusEffects.ABSORPTION)) {
                context.throwGameTestException("Flux Capacitor should grant absorption at high charge");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fluxConductivityAddsChargeOnDamage(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        FluxCharge.set(player, 0);
        StaticBurstAbility.onDamaged(player, 6.0F);

        context.runAtTick(5L, () -> {
            if (FluxCharge.get(player) <= 0) {
                context.throwGameTestException("Flux Conductivity should add charge on damage");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fluxInsulationReducesDamageAtHighCharge(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity charged = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity baseline = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(charged);
        GemsGameTestUtil.forceSurvival(baseline);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(charged, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(baseline, world, pos.x + 3.0D, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(charged);
        GemPlayerState.setActiveGem(charged, GemId.FLUX);
        GemPlayerState.setEnergy(charged, 5);
        GemPowers.sync(charged);

        GemPlayerState.initIfNeeded(baseline);
        GemPlayerState.setActiveGem(baseline, GemId.FLUX);
        GemPlayerState.setEnergy(baseline, 5);
        GemPowers.sync(baseline);

        FluxCharge.set(charged, GemsBalance.v().flux().fluxInsulationChargeThreshold());
        FluxCharge.set(baseline, 0);

        context.runAtTick(5L, () -> {
            float damage = 6.0F;
            charged.damage(world, charged.getDamageSources().magic(), damage);
            baseline.damage(world, baseline.getDamageSources().magic(), damage);

            if (charged.getHealth() >= baseline.getHealth()) {
                context.complete();
                return;
            }
            context.throwGameTestException("Flux Insulation should reduce damage at high charge");
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fluxConfigValuesValid(TestContext context) {
        var cfg = GemsBalance.v().flux();
        
        if (cfg.fluxBeamCooldownTicks() < 0) {
            context.throwGameTestException("Flux Beam cooldown cannot be negative");
        }
        if (cfg.fluxDischargeCooldownTicks() < 0) {
            context.throwGameTestException("Discharge cooldown cannot be negative");
        }
        if (cfg.fluxSurgeCooldownTicks() < 0) {
            context.throwGameTestException("Surge cooldown cannot be negative");
        }
        
        context.complete();
    }
}
