package com.feel.gems.gametest.item;

import java.util.EnumSet;
import com.feel.gems.assassin.AssassinState;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemEnergyState;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.item.GemKeepOnDeath;
import com.feel.gems.item.GemItemGlint;
import com.feel.gems.item.ModItems;
import com.feel.gems.power.AbilityDisables;
import com.feel.gems.power.AbilityRuntime;
import com.feel.gems.power.AirDashAbility;
import com.feel.gems.power.AirUpdraftZoneAbility;
import com.feel.gems.power.AirWindJumpAbility;
import com.feel.gems.power.BeaconAuraAbility;
import com.feel.gems.power.BeaconAuraRuntime;
import com.feel.gems.power.FluxBeamAbility;
import com.feel.gems.power.FluxCharge;
import com.feel.gems.power.GemPowers;
import com.feel.gems.power.GemAbilities;
import com.feel.gems.power.PanicRingAbility;
import com.feel.gems.power.PillagerDiscipline;
import com.feel.gems.power.PillagerFangsAbility;
import com.feel.gems.power.PillagerVolleyAbility;
import com.feel.gems.power.PillagerVolleyRuntime;
import com.feel.gems.power.PowerIds;
import com.feel.gems.power.SpyMimicFormAbility;
import com.feel.gems.power.SpyMimicSystem;
import com.feel.gems.power.SpyStealAbility;
import com.feel.gems.power.SummonRecallAbility;
import com.feel.gems.power.SummonSlotAbility;
import com.feel.gems.power.SummonerSummons;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.trade.GemTrading;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTime;
import com.feel.gems.power.GemAbilityCooldowns;
import com.feel.gems.net.CooldownSnapshotPayload;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.EvokerFangsEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryByteBuf;

public final class GemsItemGameTests {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void recipesAreRegistered(TestContext context) {
        ServerWorld world = context.getWorld();
        var server = world.getServer();
        if (server == null) {
            context.throwGameTestException("No server instance");
            return;
        }
        var manager = server.getRecipeManager();
        for (String path : new String[]{"heart", "energy_upgrade", "trader"}) {
            Identifier id = Identifier.of("gems", path);
            if (manager.get(id).isEmpty()) {
                context.throwGameTestException("Missing recipe: " + id);
                return;
            }
        }
        context.complete();
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 120)
    public void glintAppliesOnlyToActiveGemAtCap(TestContext context) {
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setEnergy(player, GemPlayerState.MAX_ENERGY);
        GemPlayerState.setActiveGem(player, GemId.ASTRA);

        player.getInventory().clear();
        player.giveItemStack(new ItemStack(ModItems.ASTRA_GEM));
        player.giveItemStack(new ItemStack(ModItems.FIRE_GEM));

        context.runAtTick(2L, () -> {
            GemItemGlint.sync(player);
            if (GemsGameTestUtil.countGlint(player, ModItems.ASTRA_GEM) != 1) {
                context.throwGameTestException("Active gem should glint when at energy cap");
                return;
            }
            if (GemsGameTestUtil.countGlint(player, ModItems.FIRE_GEM) != 0) {
                context.throwGameTestException("Inactive gems should not glint when another gem is active");
                return;
            }

            GemPlayerState.setEnergy(player, GemPlayerState.MIN_ENERGY);
            GemItemGlint.sync(player);
            if (GemsGameTestUtil.countGlint(player, ModItems.ASTRA_GEM) != 0) {
                context.throwGameTestException("Glint should clear when energy drops below cap");
                return;
            }

            GemPlayerState.setActiveGem(player, GemId.FIRE);
            GemPlayerState.setEnergy(player, GemPlayerState.MAX_ENERGY);
            GemItemGlint.sync(player);
            if (GemsGameTestUtil.countGlint(player, ModItems.ASTRA_GEM) != 0) {
                context.throwGameTestException("Old active gem should lose glint after switching");
                return;
            }
            if (GemsGameTestUtil.countGlint(player, ModItems.FIRE_GEM) != 1) {
                context.throwGameTestException("New active gem should gain glint at energy cap");
                return;
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 120)
    public void energyUpgradeIncreasesEnergyUntilCap(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setEnergy(player, GemPlayerState.MAX_ENERGY - 1);

        ItemStack upgrades = new ItemStack(ModItems.ENERGY_UPGRADE, 2);
        player.setStackInHand(Hand.MAIN_HAND, upgrades);

        context.runAtTick(2L, () -> {
            ModItems.ENERGY_UPGRADE.use(world, player, Hand.MAIN_HAND);
            if (GemPlayerState.getEnergy(player) != GemPlayerState.MAX_ENERGY) {
                context.throwGameTestException("Energy upgrade did not raise energy to cap");
                return;
            }
            if (player.getMainHandStack().getCount() != 1) {
                context.throwGameTestException("Energy upgrade should consume exactly one item when applied");
                return;
            }

            ModItems.ENERGY_UPGRADE.use(world, player, Hand.MAIN_HAND);
            if (GemPlayerState.getEnergy(player) != GemPlayerState.MAX_ENERGY) {
                context.throwGameTestException("Energy should stay capped after extra upgrade use");
                return;
            }
            if (player.getMainHandStack().getCount() != 1) {
                context.throwGameTestException("Energy upgrade should not consume when at cap");
                return;
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 120)
    public void heartItemRespectsMaxCap(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setMaxHearts(player, GemPlayerState.MAX_MAX_HEARTS - 1);
        GemPlayerState.applyMaxHearts(player);

        ItemStack hearts = new ItemStack(ModItems.HEART, 2);
        player.setStackInHand(Hand.MAIN_HAND, hearts);

        context.runAtTick(2L, () -> {
            ModItems.HEART.use(world, player, Hand.MAIN_HAND);
            if (GemPlayerState.getMaxHearts(player) != GemPlayerState.MAX_MAX_HEARTS) {
                context.throwGameTestException("Heart did not increase max hearts to the cap");
                return;
            }
            if (player.getMainHandStack().getCount() != 1) {
                context.throwGameTestException("Heart should consume exactly one item when applied");
                return;
            }

            ModItems.HEART.use(world, player, Hand.MAIN_HAND);
            if (GemPlayerState.getMaxHearts(player) != GemPlayerState.MAX_MAX_HEARTS) {
                context.throwGameTestException("Hearts should stay at cap after extra use");
                return;
            }
            if (player.getMainHandStack().getCount() != 1) {
                context.throwGameTestException("Heart should not consume when already capped");
                return;
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 120)
    public void pocketsInventoryPersistsAcrossSaves(TestContext context) {
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        context.runAtTick(2L, () -> {
            var pockets = com.feel.gems.screen.PocketsStorage.load(player);
            pockets.setStack(0, new ItemStack(Items.DIAMOND, 3));
            com.feel.gems.screen.PocketsStorage.save(player, pockets);

            var reloaded = com.feel.gems.screen.PocketsStorage.load(player);
            if (!ItemStack.areEqual(reloaded.getStack(0), new ItemStack(Items.DIAMOND, 3))) {
                context.throwGameTestException("Pockets inventory did not persist items through save/load");
                return;
            }
            context.complete();
        });
    }

}

