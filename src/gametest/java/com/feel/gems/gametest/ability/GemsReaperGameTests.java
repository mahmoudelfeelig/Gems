package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.reaper.ReaperBloodChargeAbility;
import com.feel.gems.power.ability.reaper.ReaperDeathOathAbility;
import com.feel.gems.power.ability.reaper.ReaperGraveSteedAbility;
import com.feel.gems.power.ability.reaper.ReaperRetributionAbility;
import com.feel.gems.power.ability.reaper.ReaperScytheSweepAbility;
import com.feel.gems.power.ability.reaper.ReaperShadowCloneAbility;
import com.feel.gems.power.ability.reaper.ReaperWitheringStrikesAbility;
import com.feel.gems.power.gem.reaper.ReaperBloodCharge;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SkeletonHorseEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Vec3d;
import net.minecraft.item.Items;

public final class GemsReaperGameTests {

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
    public void bloodChargeStoresMultiplier(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.REAPER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean started = new ReaperBloodChargeAbility().activate(player);
            if (started) {
                context.throwGameTestException("Blood Charge should not consume on start");
            }
            if (!ReaperBloodCharge.isCharging(player)) {
                context.throwGameTestException("Blood Charge should start charging");
            }
            AbilityRuntime.tickEverySecond(player);
            boolean stored = new ReaperBloodChargeAbility().activate(player);
            if (!stored) {
                context.throwGameTestException("Blood Charge should store a buff on second activation");
            }
            float mult = ReaperBloodCharge.consumeMultiplierIfActive(player);
            if (mult <= 1.0F) {
                context.throwGameTestException("Blood Charge should grant a damage multiplier");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void scytheSweepDamagesNearby(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        ZombieEntity enemy = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
        if (enemy == null) {
            context.throwGameTestException("Failed to create zombie");
            return;
        }
        enemy.refreshPositionAndAngles(pos.x, pos.y, pos.z + 2.0D, 0.0F, 0.0F);
        world.spawnEntity(enemy);
        aimAt(player, world, enemy.getEntityPos().add(0.0D, 1.2D, 0.0D));

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.REAPER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new ReaperScytheSweepAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Scythe Sweep did not activate");
            }
            if (enemy.getHealth() >= enemy.getMaxHealth()) {
                context.throwGameTestException("Scythe Sweep should damage enemies");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void witheringStrikesAppliesWither(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.REAPER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new ReaperWitheringStrikesAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Withering Strikes did not activate");
            }
        });

        context.runAtTick(15L, () -> {
            ZombieEntity target = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
            if (target == null) {
                context.throwGameTestException("Failed to create zombie");
                return;
            }
            target.refreshPositionAndAngles(pos.x, pos.y, pos.z + 2.0D, 0.0F, 0.0F);
            world.spawnEntity(target);
            player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, Items.DIAMOND_SWORD.getDefaultStack());
            player.attack(target);
            if (!target.hasStatusEffect(StatusEffects.WITHER)) {
                context.throwGameTestException("Withering Strikes should apply Wither on hit");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void shadowCloneSpawnsClone(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.REAPER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            int before = world.getEntitiesByClass(com.feel.gems.entity.ShadowCloneEntity.class, player.getBoundingBox().expand(8.0D), e -> true).size();
            boolean ok = new ReaperShadowCloneAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Shadow Clone did not activate");
            }
            int after = world.getEntitiesByClass(com.feel.gems.entity.ShadowCloneEntity.class, player.getBoundingBox().expand(8.0D), e -> true).size();
            if (after <= before) {
                context.throwGameTestException("Shadow Clone should spawn clones");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void retributionReflectsDamage(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ZombieEntity attacker = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
        GemsGameTestUtil.forceSurvival(player);

        if (attacker == null) {
            context.throwGameTestException("Failed to create zombie");
            return;
        }

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        attacker.refreshPositionAndAngles(pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
        world.spawnEntity(attacker);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.REAPER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new ReaperRetributionAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Retribution did not activate");
            }
            float beforePlayer = player.getHealth();
            float beforeAttacker = attacker.getHealth();
            player.damage(world, player.getDamageSources().mobAttack(attacker), 6.0F);
            if (player.getHealth() < beforePlayer) {
                context.throwGameTestException("Retribution should prevent incoming damage");
            }
            if (attacker.getHealth() >= beforeAttacker) {
                context.throwGameTestException("Retribution should reflect damage to the attacker");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void deathOathMarksTarget(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        ZombieEntity target = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
        if (target == null) {
            context.throwGameTestException("Failed to create zombie");
            return;
        }
        target.refreshPositionAndAngles(pos.x, pos.y, pos.z + 3.0D, 180.0F, 0.0F);
        world.spawnEntity(target);
        aimAt(player, world, target.getEntityPos().add(0.0D, 1.2D, 0.0D));

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.REAPER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new ReaperDeathOathAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Death Oath did not activate");
            }
            if (!AbilityRuntime.isReaperDeathOathActive(player)) {
                context.throwGameTestException("Death Oath should mark a target");
            }
            if (!target.getUuid().equals(AbilityRuntime.reaperDeathOathTarget(player))) {
                context.throwGameTestException("Death Oath should store the target UUID");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void graveSteedSummonsHorse(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.REAPER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new ReaperGraveSteedAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Grave Steed did not activate");
            }
            SkeletonHorseEntity horse = world.getEntitiesByClass(SkeletonHorseEntity.class, player.getBoundingBox().expand(6.0D), e -> true)
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (horse == null) {
                context.throwGameTestException("Grave Steed should spawn a skeleton horse");
            }
            if (!player.hasVehicle()) {
                context.throwGameTestException("Grave Steed should mount the player");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void reaperPassivesApply(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.REAPER);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            ItemStack food = Items.ROTTEN_FLESH.getDefaultStack();
            player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(StatusEffects.HUNGER, 200, 0));
            food.finishUsing(world, player);
        });

        context.runAtTick(25L, () -> {
            if (player.hasStatusEffect(StatusEffects.HUNGER)) {
                context.throwGameTestException("Rot Eater should clear hunger from rotten flesh");
                return;
            }

            ZombieEntity undead = EntityType.ZOMBIE.create(world, net.minecraft.entity.SpawnReason.MOB_SUMMONED);
            if (undead == null) {
                context.throwGameTestException("Failed to create zombie");
                return;
            }
            undead.refreshPositionAndAngles(pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
            world.spawnEntity(undead);
            float before = player.getHealth();
            player.damage(world, player.getDamageSources().mobAttack(undead), 6.0F);
            float delta = before - player.getHealth();
            float expectedMax = 6.0F * GemsBalance.v().reaper().undeadWardDamageMultiplier() + 0.1F;
            if (delta > expectedMax) {
                context.throwGameTestException("Undead Ward should reduce damage from undead");
            }

            undead.setHealth(1.0F);
            undead.damage(world, world.getDamageSources().playerAttack(player), 10.0F);
            if (!player.hasStatusEffect(StatusEffects.REGENERATION)) {
                context.throwGameTestException("Harvest should grant regeneration on mob kills");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void reaperConfigValuesValid(TestContext context) {
        var cfg = GemsBalance.v().reaper();
        
        if (cfg.scytheSweepCooldownTicks() < 0) {
            context.throwGameTestException("Scythe Sweep cooldown cannot be negative");
        }
        if (cfg.witheringStrikesCooldownTicks() < 0) {
            context.throwGameTestException("Withering Strikes cooldown cannot be negative");
        }
        if (cfg.shadowCloneCooldownTicks() < 0) {
            context.throwGameTestException("Shadow Clone cooldown cannot be negative");
        }
        if (cfg.retributionCooldownTicks() < 0) {
            context.throwGameTestException("Retribution cooldown cannot be negative");
        }
        if (cfg.deathOathCooldownTicks() < 0) {
            context.throwGameTestException("Death Oath cooldown cannot be negative");
        }
        if (cfg.graveSteedCooldownTicks() < 0) {
            context.throwGameTestException("Grave Steed cooldown cannot be negative");
        }
        
        context.complete();
    }
}
