package com.feel.gems.gametest.gems;

import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemEnergyState;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.EnumSet;

/**
 * Game tests for special gems: Void, Chaos, and Prism.
 */
public final class GemsSpecialGemsGameTests {

    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    // ==================== Void Gem Tests ====================

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void voidGemRegisteredCorrectly(TestContext context) {
        context.runAtTick(5L, () -> {
            GemDefinition voidDef = GemRegistry.definition(GemId.VOID);
            if (voidDef == null) {
                context.throwGameTestException("Void gem definition should exist");
                return;
            }

            // Void gem should have the immunity passive
            boolean hasImmunity = voidDef.passives().contains(PowerIds.VOID_IMMUNITY);
            if (!hasImmunity) {
                context.throwGameTestException("Void gem should have VOID_IMMUNITY passive");
            }

            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void voidGemCanBeSetAsActive(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        context.runAtTick(10L, () -> {
            GemPlayerState.initIfNeeded(player);
            GemPlayerState.setActiveGem(player, GemId.VOID);
            GemPlayerState.setEnergy(player, 5);
            GemPowers.sync(player);

            GemId active = GemPlayerState.getActiveGem(player);
            if (active != GemId.VOID) {
                context.throwGameTestException("Active gem should be VOID, got " + active);
            }

            int energy = GemPlayerState.getEnergy(player);
            if (energy != 5) {
                context.throwGameTestException("Energy should be 5, got " + energy);
            }

            context.complete();
        });
    }

    // ==================== Chaos Gem Tests ====================

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void chaosGemRegisteredCorrectly(TestContext context) {
        context.runAtTick(5L, () -> {
            GemDefinition chaosDef = GemRegistry.definition(GemId.CHAOS);
            if (chaosDef == null) {
                context.throwGameTestException("Chaos gem definition should exist");
                return;
            }

            // Chaos gem should have the random rotation passive
            boolean hasRotation = chaosDef.passives().contains(PowerIds.CHAOS_RANDOM_ROTATION);
            if (!hasRotation) {
                context.throwGameTestException("Chaos gem should have CHAOS_RANDOM_ROTATION passive");
            }

            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void chaosGemCanBeSetAsActive(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        context.runAtTick(10L, () -> {
            GemPlayerState.initIfNeeded(player);
            GemPlayerState.setActiveGem(player, GemId.CHAOS);
            GemPlayerState.setEnergy(player, 8);
            GemPowers.sync(player);

            GemId active = GemPlayerState.getActiveGem(player);
            if (active != GemId.CHAOS) {
                context.throwGameTestException("Active gem should be CHAOS, got " + active);
            }

            context.complete();
        });
    }

    // ==================== Prism Gem Tests ====================

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void prismGemRegisteredCorrectly(TestContext context) {
        context.runAtTick(5L, () -> {
            GemDefinition prismDef = GemRegistry.definition(GemId.PRISM);
            if (prismDef == null) {
                context.throwGameTestException("Prism gem definition should exist");
                return;
            }

            // Prism gem is special - it gets abilities from selections, not fixed list
            // It should exist but may have different structure
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void prismGemCanBeSetAsActive(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        context.runAtTick(10L, () -> {
            GemPlayerState.initIfNeeded(player);
            GemPlayerState.setActiveGem(player, GemId.PRISM);
            GemPlayerState.setEnergy(player, 10);
            GemPowers.sync(player);

            GemId active = GemPlayerState.getActiveGem(player);
            if (active != GemId.PRISM) {
                context.throwGameTestException("Active gem should be PRISM, got " + active);
            }

            context.complete();
        });
    }

    // ==================== All Gems Registry Tests ====================

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void allGemsHaveDefinitions(TestContext context) {
        context.runAtTick(5L, () -> {
            for (GemId gemId : GemId.values()) {
                GemDefinition def = GemRegistry.definition(gemId);
                if (def == null) {
                    context.throwGameTestException("Gem " + gemId + " should have a definition");
                    return;
                }
                if (def.id() != gemId) {
                    context.throwGameTestException("Definition id mismatch for " + gemId);
                    return;
                }
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void energyUnlockProgressionWorksForAllGems(TestContext context) {
        context.runAtTick(5L, () -> {
            for (GemId gemId : GemId.values()) {
                GemDefinition def = GemRegistry.definition(gemId);
                if (def == null) continue;

                // At energy 0-1: no abilities
                var noAbilities = def.availableAbilities(new GemEnergyState(0));
                if (!noAbilities.isEmpty()) {
                    context.throwGameTestException(gemId + " should have no abilities at energy 0");
                    return;
                }

                // At energy 1: passives unlock
                var passivesAtOne = def.availablePassives(new GemEnergyState(1));
                if (passivesAtOne.size() != def.passives().size() && !def.passives().isEmpty()) {
                    context.throwGameTestException(gemId + " should unlock all passives at energy 1");
                    return;
                }

                // At energy 5+: all abilities unlock
                var allAtFive = def.availableAbilities(new GemEnergyState(5));
                if (allAtFive.size() != def.abilities().size()) {
                    context.throwGameTestException(gemId + " should have all abilities at energy 5");
                    return;
                }
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void gemSwitchingWorks(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        context.runAtTick(10L, () -> {
            GemPlayerState.initIfNeeded(player);
            
            // Switch through several gems
            GemId[] toTest = {GemId.FIRE, GemId.AIR, GemId.VOID, GemId.CHAOS, GemId.PRISM, GemId.ASTRA};
            for (GemId gem : toTest) {
                GemPlayerState.setActiveGem(player, gem);
                GemPowers.sync(player);
                
                GemId active = GemPlayerState.getActiveGem(player);
                if (active != gem) {
                    context.throwGameTestException("Failed to switch to " + gem + ", got " + active);
                    return;
                }
            }
            
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 100)
    public void ownedGemsCanIncludeSpecialGems(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        context.runAtTick(10L, () -> {
            GemPlayerState.initIfNeeded(player);
            
            // Set owned gems to include special ones
            EnumSet<GemId> owned = EnumSet.of(GemId.FIRE, GemId.VOID, GemId.CHAOS, GemId.PRISM);
            GemPlayerState.setOwnedGemsExact(player, owned);
            
            var retrieved = GemPlayerState.getOwnedGems(player);
            if (!retrieved.contains(GemId.VOID)) {
                context.throwGameTestException("Owned gems should include VOID");
            }
            if (!retrieved.contains(GemId.CHAOS)) {
                context.throwGameTestException("Owned gems should include CHAOS");
            }
            if (!retrieved.contains(GemId.PRISM)) {
                context.throwGameTestException("Owned gems should include PRISM");
            }
            
            context.complete();
        });
    }
}
