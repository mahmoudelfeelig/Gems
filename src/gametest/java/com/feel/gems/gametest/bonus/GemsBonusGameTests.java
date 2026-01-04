package com.feel.gems.gametest.bonus;

import com.feel.gems.bonus.BonusClaimsState;
import com.feel.gems.bonus.BonusPoolRegistry;
import com.feel.gems.bonus.PrismSelectionsState;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.trust.GemTrust;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Game tests for the bonus pool system and special gem mechanics.
 */
public final class GemsBonusGameTests {

    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    // ==================== Bonus Claims Tests ====================

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void bonusClaimsAreServerWideUnique(TestContext context) {
        ServerWorld world = context.getWorld();
        MinecraftServer server = world.getServer();
        if (server == null) {
            context.throwGameTestException("No server instance");
            return;
        }

        ServerPlayerEntity player1 = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity player2 = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player1.changeGameMode(GameMode.SURVIVAL);
        player2.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos1 = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        Vec3d pos2 = context.getAbsolute(new Vec3d(2.5D, 2.0D, 0.5D));
        teleport(player1, world, pos1.x, pos1.y, pos1.z, 0.0F, 0.0F);
        teleport(player2, world, pos2.x, pos2.y, pos2.z, 0.0F, 0.0F);

        context.runAtTick(10L, () -> {
            BonusClaimsState claimsState = BonusClaimsState.get(server);
            
            // Player 1 claims thunderstrike
            boolean claim1 = claimsState.claimAbility(player1.getUuid(), PowerIds.BONUS_THUNDERSTRIKE);
            if (!claim1) {
                context.throwGameTestException("Player 1 should be able to claim unclaimed ability");
            }

            // Player 2 tries to claim same ability - should fail
            boolean claim2 = claimsState.claimAbility(player2.getUuid(), PowerIds.BONUS_THUNDERSTRIKE);
            if (claim2) {
                context.throwGameTestException("Player 2 should NOT be able to claim ability already claimed by player 1");
            }

            // Player 2 can claim a different ability
            boolean claim3 = claimsState.claimAbility(player2.getUuid(), PowerIds.BONUS_FROSTBITE);
            if (!claim3) {
                context.throwGameTestException("Player 2 should be able to claim a different unclaimed ability");
            }

            // Cleanup
            claimsState.releaseAllClaims(player1.getUuid());
            claimsState.releaseAllClaims(player2.getUuid());

            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void bonusClaimsMaxTwoAbilitiesPerPlayer(TestContext context) {
        ServerWorld world = context.getWorld();
        MinecraftServer server = world.getServer();
        if (server == null) {
            context.throwGameTestException("No server instance");
            return;
        }

        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        context.runAtTick(10L, () -> {
            BonusClaimsState claimsState = BonusClaimsState.get(server);
            UUID playerUuid = player.getUuid();

            // First two claims should succeed
            boolean first = claimsState.claimAbility(playerUuid, PowerIds.BONUS_THUNDERSTRIKE);
            boolean second = claimsState.claimAbility(playerUuid, PowerIds.BONUS_FROSTBITE);
            if (!first || !second) {
                context.throwGameTestException("First two ability claims should succeed");
            }

            // Third claim should fail (max 2)
            boolean third = claimsState.claimAbility(playerUuid, PowerIds.BONUS_EARTHSHATTER);
            if (third) {
                context.throwGameTestException("Third ability claim should fail (max 2)");
            }

            // Player should have exactly 2 abilities
            int count = claimsState.getPlayerAbilities(playerUuid).size();
            if (count != 2) {
                context.throwGameTestException("Expected 2 abilities, got " + count);
            }

            // Cleanup
            claimsState.releaseAllClaims(playerUuid);

            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void bonusClaimsReleaseReallowsOthers(TestContext context) {
        ServerWorld world = context.getWorld();
        MinecraftServer server = world.getServer();
        if (server == null) {
            context.throwGameTestException("No server instance");
            return;
        }

        ServerPlayerEntity player1 = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity player2 = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player1.changeGameMode(GameMode.SURVIVAL);
        player2.changeGameMode(GameMode.SURVIVAL);

        context.runAtTick(10L, () -> {
            BonusClaimsState claimsState = BonusClaimsState.get(server);
            
            // Player 1 claims
            claimsState.claimAbility(player1.getUuid(), PowerIds.BONUS_THUNDERSTRIKE);
            
            // Player 2 cannot claim
            boolean beforeRelease = claimsState.claimAbility(player2.getUuid(), PowerIds.BONUS_THUNDERSTRIKE);
            if (beforeRelease) {
                context.throwGameTestException("Should not claim before release");
            }

            // Player 1 releases
            claimsState.releaseAllClaims(player1.getUuid());

            // Now player 2 can claim
            boolean afterRelease = claimsState.claimAbility(player2.getUuid(), PowerIds.BONUS_THUNDERSTRIKE);
            if (!afterRelease) {
                context.throwGameTestException("Should be able to claim after other player releases");
            }

            // Cleanup
            claimsState.releaseAllClaims(player2.getUuid());

            context.complete();
        });
    }

    // ==================== Trust System Tests ====================

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void trustSystemAllowsSelfTrust(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        context.runAtTick(10L, () -> {
            // Player should always trust themselves
            boolean trustsSelf = GemTrust.isTrusted(player, player);
            if (!trustsSelf) {
                context.throwGameTestException("Player should always trust themselves");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void trustSystemManualTrustWorks(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity owner = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity other = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        owner.changeGameMode(GameMode.SURVIVAL);
        other.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos1 = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        Vec3d pos2 = context.getAbsolute(new Vec3d(2.5D, 2.0D, 0.5D));
        teleport(owner, world, pos1.x, pos1.y, pos1.z, 0.0F, 0.0F);
        teleport(other, world, pos2.x, pos2.y, pos2.z, 0.0F, 0.0F);

        context.runAtTick(10L, () -> {
            // Initially other should not be trusted
            boolean initiallyTrusted = GemTrust.isTrusted(owner, other);
            if (initiallyTrusted) {
                context.throwGameTestException("Other player should not be trusted initially");
            }

            // Add trust
            GemTrust.trust(owner, other.getUuid());

            // Now should be trusted
            boolean afterTrust = GemTrust.isTrusted(owner, other);
            if (!afterTrust) {
                context.throwGameTestException("Other player should be trusted after adding");
            }

            // Remove trust
            GemTrust.untrust(owner, other.getUuid());

            // Should no longer be trusted
            boolean afterUntrust = GemTrust.isTrusted(owner, other);
            if (afterUntrust) {
                context.throwGameTestException("Other player should not be trusted after removing");
            }

            context.complete();
        });
    }

    // ==================== Special Gems Tests ====================

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void voidGemExistsAndHasImmunityPassive(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        context.runAtTick(10L, () -> {
            GemPlayerState.initIfNeeded(player);
            GemPlayerState.setActiveGem(player, GemId.VOID);
            GemPlayerState.setEnergy(player, 5);

            GemId active = GemPlayerState.getActiveGem(player);
            if (active != GemId.VOID) {
                context.throwGameTestException("Active gem should be VOID");
            }

            // Verify VOID_IMMUNITY is in blacklist (special handling)
            if (!BonusPoolRegistry.isBlacklisted(PowerIds.VOID_IMMUNITY)) {
                context.throwGameTestException("VOID_IMMUNITY should be blacklisted from Prism");
            }

            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void chaosGemExistsAndHasRandomRotation(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        context.runAtTick(10L, () -> {
            GemPlayerState.initIfNeeded(player);
            GemPlayerState.setActiveGem(player, GemId.CHAOS);
            GemPlayerState.setEnergy(player, 5);

            GemId active = GemPlayerState.getActiveGem(player);
            if (active != GemId.CHAOS) {
                context.throwGameTestException("Active gem should be CHAOS");
            }

            // Verify CHAOS_RANDOM_ROTATION is in blacklist
            if (!BonusPoolRegistry.isBlacklisted(PowerIds.CHAOS_RANDOM_ROTATION)) {
                context.throwGameTestException("CHAOS_RANDOM_ROTATION should be blacklisted from Prism");
            }

            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void prismGemSelectionsWork(TestContext context) {
        ServerWorld world = context.getWorld();
        MinecraftServer server = world.getServer();
        if (server == null) {
            context.throwGameTestException("No server instance");
            return;
        }

        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        context.runAtTick(10L, () -> {
            GemPlayerState.initIfNeeded(player);
            GemPlayerState.setActiveGem(player, GemId.PRISM);
            GemPlayerState.setEnergy(player, 10);

            GemId active = GemPlayerState.getActiveGem(player);
            if (active != GemId.PRISM) {
                context.throwGameTestException("Active gem should be PRISM");
            }

            // Test prism selections
            PrismSelectionsState prismState = PrismSelectionsState.get(server);
            UUID playerUuid = player.getUuid();
            
            // Can add gem abilities (max 3)
            boolean added1 = prismState.addGemAbility(playerUuid, PowerIds.AIR_DASH);
            if (!added1) {
                context.throwGameTestException("Should be able to add gem ability to Prism");
            }

            // Cannot add blacklisted
            boolean addedBlacklist = prismState.addGemAbility(playerUuid, PowerIds.VOID_IMMUNITY);
            if (addedBlacklist) {
                context.throwGameTestException("Should NOT be able to add blacklisted ability");
            }

            // Cleanup
            prismState.clearSelections(playerUuid);

            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void prismSelectionsLimitCorrectly(TestContext context) {
        ServerWorld world = context.getWorld();
        MinecraftServer server = world.getServer();
        if (server == null) {
            context.throwGameTestException("No server instance");
            return;
        }

        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        context.runAtTick(10L, () -> {
            PrismSelectionsState prismState = PrismSelectionsState.get(server);
            UUID playerUuid = player.getUuid();

            // Add 3 gem abilities (max)
            prismState.addGemAbility(playerUuid, PowerIds.AIR_DASH);
            prismState.addGemAbility(playerUuid, PowerIds.FIREBALL);
            prismState.addGemAbility(playerUuid, PowerIds.FLUX_BEAM);

            // 4th should fail
            boolean fourth = prismState.addGemAbility(playerUuid, PowerIds.DOUBLE_JUMP);
            if (fourth) {
                context.throwGameTestException("Should not allow more than 3 gem abilities");
            }

            // Add 2 bonus abilities (max)
            prismState.addBonusAbility(playerUuid, PowerIds.BONUS_THUNDERSTRIKE);
            prismState.addBonusAbility(playerUuid, PowerIds.BONUS_FROSTBITE);

            // 3rd bonus should fail
            boolean thirdBonus = prismState.addBonusAbility(playerUuid, PowerIds.BONUS_EARTHSHATTER);
            if (thirdBonus) {
                context.throwGameTestException("Should not allow more than 2 bonus abilities");
            }

            var selection = prismState.getSelection(playerUuid);
            if (selection.totalAbilities() != 5) {
                context.throwGameTestException("Expected 5 total abilities (3 gem + 2 bonus), got " + selection.totalAbilities());
            }

            // Cleanup
            prismState.clearSelections(playerUuid);

            context.complete();
        });
    }
}
