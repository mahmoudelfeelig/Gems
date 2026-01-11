package com.feel.gems.gametest.passive;

import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.passive.bonus.*;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Vec3d;

/**
 * Tests for ALL 50 Bonus Passives.
 * Each passive is tested for basic apply/remove lifecycle.
 */
public final class GemsBonusPassiveGameTests {

    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    private static ServerPlayerEntity setupPlayer(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);
        return player;
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusAdrenalineRushPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusAdrenalineRushPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusAdrenalineSurgePassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusAdrenalineSurgePassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusArcaneBarrierPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusArcaneBarrierPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusAttackSpeedPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusAttackSpeedPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusBattleMedicPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusBattleMedicPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusBerserkerBloodPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusBerserkerBloodPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusBloodthirstPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusBloodthirstPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusBulwarkPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusBulwarkPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusChainBreakerPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusChainBreakerPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusCombatMeditatePassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusCombatMeditatePassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusCounterStrikePassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusCounterStrikePassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusCriticalStrikePassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusCriticalStrikePassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusCullingBladePassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusCullingBladePassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusDamageReductionPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusDamageReductionPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusDodgeChancePassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusDodgeChancePassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusEchoStrikePassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusEchoStrikePassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusElementalHarmonyPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusElementalHarmonyPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusEvasiveRollPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusEvasiveRollPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusExecutionerPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusExecutionerPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusFocusedMindPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusFocusedMindPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusHungerResistPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusHungerResistPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusHuntersInstinctPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusHuntersInstinctPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusImpactAbsorbPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusImpactAbsorbPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusIntimidatePassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusIntimidatePassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusIroncladPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusIroncladPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusLastStandPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusLastStandPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusLifestealPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusLifestealPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusMagneticPullPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusMagneticPullPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusManaShieldPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusManaShieldPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusNemesisPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusNemesisPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusOpportunistPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusOpportunistPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusOverflowingVitalityPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusOverflowingVitalityPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusPoisonImmunityPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusPoisonImmunityPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusPredatorSensePassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusPredatorSensePassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusQuickRecoveryPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusQuickRecoveryPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusReachExtendPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusReachExtendPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusRegenerationBoostPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusRegenerationBoostPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusSecondWindPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusSecondWindPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusSixthSensePassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusSixthSensePassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusSpectralFormPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusSpectralFormPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusSteelResolvePassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusSteelResolvePassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusStoneSkinPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusStoneSkinPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusThickSkinPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusThickSkinPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusThornsAuraPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusThornsAuraPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusTreasureHunterPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusTreasureHunterPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusUnbreakablePassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusUnbreakablePassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusVengeancePassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusVengeancePassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusWarCryPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusWarCryPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusWeaponMasteryPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusWeaponMasteryPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void bonusXpBoostPassiveLifecycle(TestContext context) {
        ServerPlayerEntity player = setupPlayer(context);
        context.runAtTick(5L, () -> {
            var passive = new BonusXpBoostPassive();
            passive.apply(player);
            passive.remove(player);
            context.complete();
        });
    }
}
