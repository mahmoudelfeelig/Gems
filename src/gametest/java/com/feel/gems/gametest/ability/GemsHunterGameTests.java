package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.entity.HunterPackEntity;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.hunter.*;
import com.feel.gems.power.gem.hunter.HunterPreyMarkRuntime;
import com.feel.gems.power.gem.hunter.HunterTrophyHunterRuntime;
import com.feel.gems.power.ability.hunter.HunterPackTacticsRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.trust.GemTrust;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public final class GemsHunterGameTests {

    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void hunterPreyMarkAppliesOnHitAndGrantsGlowing(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity hunter = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(hunter);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(hunter, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(target, world, pos.x, pos.y, pos.z + 2.0D, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(hunter);
        GemPlayerState.setActiveGem(hunter, GemId.HUNTER);
        GemPlayerState.setEnergy(hunter, 5);
        GemPowers.sync(hunter);

        context.runAtTick(5L, () -> {
            hunter.attack(target);
        });

        context.runAtTick(15L, () -> {
            if (!HunterPreyMarkRuntime.isMarked(hunter, target)) {
                context.throwGameTestException("Prey Mark should apply to the last attacked target");
                return;
            }
            if (target.getStatusEffect(StatusEffects.GLOWING) == null) {
                context.throwGameTestException("Tracker's Eye should grant Glowing on marked targets");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void hunterPounceLaunchesTowardMarkedTarget(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity hunter = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(hunter);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(hunter, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(target, world, pos.x, pos.y, pos.z + 6.0D, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(hunter);
        GemPlayerState.setActiveGem(hunter, GemId.HUNTER);
        GemPlayerState.setEnergy(hunter, 5);
        GemPowers.sync(hunter);

        context.runAtTick(5L, () -> hunter.attack(target));

        context.runAtTick(10L, () -> {
            boolean ok = new HunterPounceAbility().activate(hunter);
            if (!ok) {
                context.throwGameTestException("Pounce did not activate with a marked target");
                return;
            }
        });

        context.runAtTick(20L, () -> {
            if (hunter.getVelocity().lengthSquared() <= 0.05D) {
                context.throwGameTestException("Pounce should launch the hunter toward the marked target");
                return;
            }
            if (!HunterPounceRuntime.isPouncing(hunter)) {
                context.throwGameTestException("Pounce should register an active pounce state");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void hunterNetShotAppliesSlownessAndNetted(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity hunter = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(hunter);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(hunter, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(target, world, pos.x, pos.y, pos.z + 4.0D, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(hunter);
        GemPlayerState.setActiveGem(hunter, GemId.HUNTER);
        GemPlayerState.setEnergy(hunter, 5);
        GemPowers.sync(hunter);

        context.runAtTick(5L, () -> {
            boolean ok = new HunterNetShotAbility().activate(hunter);
            if (!ok) {
                context.throwGameTestException("Net Shot did not activate");
            }
        });

        context.runAtTick(15L, () -> {
            if (target.getStatusEffect(StatusEffects.SLOWNESS) == null) {
                context.throwGameTestException("Net Shot should apply Slowness");
                return;
            }
            if (!HunterNetShotAbility.isNetted(target)) {
                context.throwGameTestException("Net Shot should mark target as netted");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void hunterCripplingShotAppliesSlowness(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity hunter = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(hunter);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(hunter, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(target, world, pos.x, pos.y, pos.z + 4.0D, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(hunter);
        GemPlayerState.setActiveGem(hunter, GemId.HUNTER);
        GemPlayerState.setEnergy(hunter, 5);
        GemPowers.sync(hunter);

        context.runAtTick(5L, () -> {
            boolean ok = new HunterCripplingShotAbility().activate(hunter);
            if (!ok) {
                context.throwGameTestException("Crippling Shot did not activate");
                return;
            }
        });

        context.runAtTick(15L, () -> {
            if (target.getStatusEffect(StatusEffects.SLOWNESS) == null) {
                context.throwGameTestException("Crippling Shot should apply Slowness");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void hunterHuntingTrapRootsAndDamages(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity hunter = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(hunter);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(hunter, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(target, world, pos.x + 3.0D, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(hunter);
        GemPlayerState.setActiveGem(hunter, GemId.HUNTER);
        GemPlayerState.setEnergy(hunter, 5);
        GemPowers.sync(hunter);

        context.runAtTick(5L, () -> {
            boolean ok = new HunterHuntingTrapAbility().activate(hunter);
            if (!ok) {
                context.throwGameTestException("Hunting Trap did not activate");
                return;
            }
        });

        context.runAtTick(10L, () -> {
            float before = target.getHealth();
            teleport(target, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
            HunterHuntingTrapAbility.checkStep(target);
            if (target.getHealth() >= before) {
                context.throwGameTestException("Hunting Trap should damage targets that step on it");
                return;
            }
            if (target.getStatusEffect(StatusEffects.SLOWNESS) == null) {
                context.throwGameTestException("Hunting Trap should root targets with Slowness");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void hunterPackTacticsGrantsBuffToAllies(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity hunter = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity ally = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(hunter);
        GemsGameTestUtil.forceSurvival(ally);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(hunter, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(ally, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);
        teleport(target, world, pos.x, pos.y, pos.z + 2.0D, 180.0F, 0.0F);

        GemPlayerState.initIfNeeded(hunter);
        GemPlayerState.setActiveGem(hunter, GemId.HUNTER);
        GemPlayerState.setEnergy(hunter, 5);
        GemPowers.sync(hunter);

        GemTrust.trust(hunter, ally.getUuid());

        context.runAtTick(5L, () -> {
            hunter.attack(target);
            boolean ok = new HunterPackTacticsAbility().activate(hunter);
            if (!ok) {
                context.throwGameTestException("Pack Tactics did not activate with a marked target");
                return;
            }
        });

        context.runAtTick(15L, () -> {
            if (!HunterPackTacticsRuntime.hasBuffAgainst(ally, target.getUuid())) {
                context.throwGameTestException("Pack Tactics should grant buff to trusted allies");
                return;
            }
            if (!HunterPackTacticsRuntime.hasBuffAgainst(hunter, target.getUuid())) {
                context.throwGameTestException("Pack Tactics should grant buff to the caster");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void hunterCallThePackSummonsAllies(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity hunter = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(hunter);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(hunter, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(hunter);
        GemPlayerState.setActiveGem(hunter, GemId.HUNTER);
        GemPlayerState.setEnergy(hunter, 5);
        GemPowers.sync(hunter);

        context.runAtTick(5L, () -> {
            boolean ok = new HunterCallThePackAbility().activate(hunter);
            if (!ok) {
                context.throwGameTestException("Call The Pack did not activate");
                return;
            }
        });

        context.runAtTick(15L, () -> {
            int count = world.getEntitiesByClass(HunterPackEntity.class, hunter.getBoundingBox().expand(12.0D), e -> true).size();
            if (count <= 0) {
                context.throwGameTestException("Call The Pack should summon hunter pack entities");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void hunterOriginTrackingStartsBounty(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity hunter = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(hunter);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(hunter, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(target, world, pos.x + 3.0D, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(hunter);
        GemPlayerState.setActiveGem(hunter, GemId.HUNTER);
        GemPlayerState.setEnergy(hunter, 5);
        GemPowers.sync(hunter);

        ItemStack stack = new ItemStack(Items.DIAMOND);
        AbilityRuntime.setOwnerWithName(stack, target.getUuid(), target.getName().getString());
        hunter.setStackInHand(Hand.MAIN_HAND, stack);

        context.runAtTick(5L, () -> {
            boolean ok = new HunterOriginTrackingAbility().activate(hunter);
            if (!ok) {
                context.throwGameTestException("Origin Tracking did not activate with a valid first owner");
                return;
            }
            var nbt = ((GemsPersistentDataHolder) hunter).gems$getPersistentData();
            if (!nbt.contains("bountyTarget")) {
                context.throwGameTestException("Origin Tracking should start a bounty on the first owner");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void hunterTrophyHunterStoresStolenPassive(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity hunter = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity victim = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(hunter);
        GemsGameTestUtil.forceSurvival(victim);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(hunter, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(victim, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(hunter);
        GemPlayerState.setActiveGem(hunter, GemId.HUNTER);
        GemPlayerState.setEnergy(hunter, 5);
        GemPowers.sync(hunter);

        GemPlayerState.initIfNeeded(victim);
        GemPlayerState.setActiveGem(victim, GemId.AIR);
        GemPlayerState.setEnergy(victim, 5);
        GemPowers.sync(victim);

        context.runAtTick(5L, () -> {
            HunterTrophyHunterRuntime.onPlayerKill(hunter, victim);
            if (HunterTrophyHunterRuntime.getTrophyPassive(hunter) == null) {
                context.throwGameTestException("Trophy Hunter should store a stolen passive");
                return;
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void hunterConfigValuesValid(TestContext context) {
        var cfg = GemsBalance.v().hunter();
        
        if (cfg.pounceCooldownTicks() < 0) {
            context.throwGameTestException("Pounce cooldown cannot be negative");
        }
        if (cfg.netShotCooldownTicks() < 0) {
            context.throwGameTestException("Net Shot cooldown cannot be negative");
        }
        
        context.complete();
    }
}
