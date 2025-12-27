package com.feel.gems.gametest.power;

import com.feel.gems.assassin.AssassinState;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemEnergyState;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.item.GemItemGlint;
import com.feel.gems.item.GemKeepOnDeath;
import com.feel.gems.item.ModItems;
import com.feel.gems.net.CooldownSnapshotPayload;
import com.feel.gems.power.ability.air.AirDashAbility;
import com.feel.gems.power.ability.air.AirUpdraftZoneAbility;
import com.feel.gems.power.ability.air.AirWindJumpAbility;
import com.feel.gems.power.ability.beacon.BeaconAuraAbility;
import com.feel.gems.power.ability.flux.FluxBeamAbility;
import com.feel.gems.power.ability.pillager.PillagerFangsAbility;
import com.feel.gems.power.ability.pillager.PillagerVolleyAbility;
import com.feel.gems.power.ability.spy.SpyMimicFormAbility;
import com.feel.gems.power.ability.spy.SpyStealAbility;
import com.feel.gems.power.ability.summoner.SummonRecallAbility;
import com.feel.gems.power.ability.summoner.SummonSlotAbility;
import com.feel.gems.power.ability.terror.PanicRingAbility;
import com.feel.gems.power.gem.beacon.BeaconAuraRuntime;
import com.feel.gems.power.gem.flux.FluxCharge;
import com.feel.gems.power.gem.pillager.PillagerDiscipline;
import com.feel.gems.power.gem.pillager.PillagerVolleyRuntime;
import com.feel.gems.power.gem.spy.SpyMimicSystem;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityDisables;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemAbilities;
import com.feel.gems.power.runtime.GemAbilityCooldowns;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.trade.GemTrading;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTime;
import io.netty.buffer.Unpooled;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
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
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.RegistryKeys;
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




public final class GemsFluxGameTests {
    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void fluxBeamConsumesCharge(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        player.teleport(world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        // Place a target in front of the player and explicitly aim at it (GameTest runs all tests in one batch,
        // so relying on default rotations or relative spawn helpers can be flaky).
        var target = EntityType.ARMOR_STAND.create(world);
        if (target == null) {
            context.throwGameTestException("Failed to create target entity");
            return;
        }
        // Keep the target very close so it stays within the default EMPTY_STRUCTURE bounds (avoids barrier occlusion).
        Vec3d targetPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 1.5D));
        target.refreshPositionAndAngles(targetPos.x, targetPos.y, targetPos.z, 0.0F, 0.0F);
        target.setNoGravity(true);
        world.spawnEntity(target);

        Vec3d aimAt = new Vec3d(target.getX(), target.getEyeY(), target.getZ());
        double dx = aimAt.x - player.getX();
        double dz = aimAt.z - player.getZ();
        double dy = aimAt.y - player.getEyeY();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        // teleport(...) is the most reliable way to apply yaw/pitch for server-side raycasts.
        player.teleport(world, startPos.x, startPos.y, startPos.z, yaw, pitch);

        FluxCharge.set(player, 100);

        context.runAtTick(5L, () -> {
            boolean activated = new FluxBeamAbility().activate(player);
            if (!activated) {
                context.throwGameTestException("Flux Beam did not activate (target not acquired)");
            }
            if (FluxCharge.get(player) != 0) {
                context.throwGameTestException("Flux Beam did not reset flux charge back to 0");
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void fluxChargeConsumesExactlyOneItem(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        player.teleport(world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 10);
        GemPowers.sync(player);

        player.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.DIAMOND_BLOCK, 3));
        FluxCharge.set(player, 0);

        context.runAtTick(2L, () -> {
            int before = player.getOffHandStack().getCount();
            boolean ok = FluxCharge.tryConsumeChargeItem(player);
            int after = player.getOffHandStack().getCount();

            if (!ok) {
                context.throwGameTestException("Flux charge did not consume a valid charge item");
            }
            if (after != before - 1) {
                context.throwGameTestException("Expected charge to consume exactly 1 item, before=" + before + " after=" + after);
            }
            if (FluxCharge.get(player) <= 0) {
                context.throwGameTestException("Flux charge did not increase after consuming a charge item");
            }
            context.complete();
        });
    }

    @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, tickLimit = 200)
    public void fluxChargeFuelItemsMatchBalanceConfig(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
        player.changeGameMode(GameMode.SURVIVAL);

        Vec3d startPos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        player.teleport(world, startPos.x, startPos.y, startPos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.FLUX);
        GemPlayerState.setEnergy(player, 10);
        GemPowers.sync(player);

        ItemStack enchantedDiamondTool = new ItemStack(Items.DIAMOND_PICKAXE, 1);
        var enchantmentRegistry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        var unbreaking = enchantmentRegistry.getEntry(Enchantments.UNBREAKING);
        if (unbreaking.isEmpty()) {
            context.throwGameTestException("Missing UNBREAKING enchantment registry entry");
            return;
        }
        EnchantmentHelper.apply(enchantedDiamondTool, builder -> builder.set(unbreaking.get(), 1));

        record Fuel(ItemStack stack, int expectedAdd, String label) {
        }
        var fuel = java.util.List.of(
                new Fuel(new ItemStack(Items.DIAMOND_BLOCK, 2), GemsBalance.v().flux().chargeDiamondBlock(), "diamond_block"),
                new Fuel(new ItemStack(Items.GOLD_BLOCK, 2), GemsBalance.v().flux().chargeGoldBlock(), "gold_block"),
                new Fuel(new ItemStack(Items.COPPER_BLOCK, 2), GemsBalance.v().flux().chargeCopperBlock(), "copper_block"),
                new Fuel(new ItemStack(Items.EMERALD_BLOCK, 2), GemsBalance.v().flux().chargeEmeraldBlock(), "emerald_block"),
                new Fuel(new ItemStack(Items.AMETHYST_BLOCK, 2), GemsBalance.v().flux().chargeAmethystBlock(), "amethyst_block"),
                new Fuel(new ItemStack(Items.NETHERITE_SCRAP, 2), GemsBalance.v().flux().chargeNetheriteScrap(), "netherite_scrap"),
                new Fuel(enchantedDiamondTool.copyWithCount(2), GemsBalance.v().flux().chargeEnchantedDiamondItem(), "enchanted_diamond_tool")
        );

        context.runAtTick(2L, () -> {
            for (Fuel f : fuel) {
                player.setStackInHand(Hand.OFF_HAND, f.stack.copy());
                FluxCharge.set(player, 0);
                FluxCharge.clearIfBelow100(player);

                int before = player.getOffHandStack().getCount();
                boolean ok = FluxCharge.tryConsumeChargeItem(player);
                int after = player.getOffHandStack().getCount();

                if (!ok) {
                    context.throwGameTestException("Fuel did not consume: " + f.label);
                    return;
                }
                if (after != before - 1) {
                    context.throwGameTestException("Expected fuel to consume exactly 1 item: " + f.label + " before=" + before + " after=" + after);
                    return;
                }

                int expectedCharge = Math.min(100, f.expectedAdd);
                int got = FluxCharge.get(player);
                if (got != expectedCharge) {
                    context.throwGameTestException("Unexpected charge after consuming fuel: " + f.label + " expected=" + expectedCharge + " got=" + got);
                    return;
                }
            }

            context.complete();
        });
    }

}

