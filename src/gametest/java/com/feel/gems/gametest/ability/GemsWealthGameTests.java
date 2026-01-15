package com.feel.gems.gametest.ability;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.gametest.util.GemsGameTestUtil;
import com.feel.gems.power.ability.wealth.AmplificationAbility;
import com.feel.gems.power.ability.wealth.FumbleAbility;
import com.feel.gems.power.ability.wealth.HotbarLockAbility;
import com.feel.gems.power.ability.wealth.PocketsAbility;
import com.feel.gems.power.ability.wealth.RichRushAbility;
import com.feel.gems.power.gem.wealth.HotbarLock;
import com.feel.gems.power.gem.wealth.WealthFumble;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import java.util.EnumSet;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.Vec3d;
import com.feel.gems.screen.PocketsScreenHandler;
import com.feel.gems.trust.GemTrust;

public final class GemsWealthGameTests {

    private static void teleport(ServerPlayerEntity player, ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        player.teleport(world, x, y, z, EnumSet.noneOf(PositionFlag.class), yaw, pitch, false);
    }

    private static int countItem(ServerPlayerEntity player, Item item) {
        int count = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        return count;
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
    public void fumbleDisorientatesEnemies(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity enemy = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity ally = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(enemy);
        GemsGameTestUtil.forceSurvival(ally);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(enemy, world, pos.x + 3.0D, pos.y, pos.z, 0.0F, 0.0F);
        teleport(ally, world, pos.x + 2.0D, pos.y, pos.z + 2.0D, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.WEALTH);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        GemTrust.trust(player, ally.getUuid());

        context.runAtTick(5L, () -> {
            boolean ok = new FumbleAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Fumble did not activate");
            }
        });

        context.runAtTick(20L, () -> {
            if (!WealthFumble.isActive(enemy)) {
                context.throwGameTestException("Fumble should affect enemies in range");
            }
            if (WealthFumble.isActive(ally)) {
                context.throwGameTestException("Fumble should not affect trusted allies");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void hotbarLockLocksTargetHotbar(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(target, world, pos.x, pos.y, pos.z + 3.0D, 180.0F, 0.0F);
        aimAt(player, world, target.getEntityPos().add(0.0D, 1.2D, 0.0D));

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.WEALTH);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new HotbarLockAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Hotbar Lock did not activate");
            }
            if (!HotbarLock.isLocked(target)) {
                context.throwGameTestException("Hotbar Lock should lock the target hotbar");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void amplificationIncreasesEnchantmentPower(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.WEALTH);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        var fortune = world.getRegistryManager().getOptionalEntry(Enchantments.FORTUNE).orElseThrow();
        EnchantmentHelper.apply(pickaxe, builder -> builder.set(fortune, 1));
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, pickaxe);

        context.runAtTick(5L, () -> {
            boolean ok = new AmplificationAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Amplification did not activate");
            }
            int level = EnchantmentHelper.getLevel(fortune, pickaxe);
            if (level <= 1) {
                context.throwGameTestException("Amplification should boost enchantment levels");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void richRushGrantsLuckEffects(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.WEALTH);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new RichRushAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Rich Rush did not activate");
            }
            if (!AbilityRuntime.isRichRushActive(player)) {
                context.throwGameTestException("Rich Rush should mark the runtime as active");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void pocketsExpandsInventory(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.WEALTH);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            boolean ok = new PocketsAbility().activate(player);
            if (!ok) {
                context.throwGameTestException("Pockets did not activate");
            }
            ScreenHandler handler = player.currentScreenHandler;
            if (!(handler instanceof PocketsScreenHandler)) {
                context.throwGameTestException("Pockets should open the pockets screen");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void wealthPassivesApply(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        ServerPlayerEntity target = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);
        GemsGameTestUtil.forceSurvival(target);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);
        teleport(target, world, pos.x + 2.0D, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.WEALTH);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        player.setStackInHand(net.minecraft.util.Hand.MAIN_HAND, sword);
        player.setStackInHand(net.minecraft.util.Hand.OFF_HAND, pickaxe);

        ItemStack helmet = new ItemStack(Items.DIAMOND_HELMET);
        helmet.setDamage(helmet.getMaxDamage() / 2);
        player.equipStack(net.minecraft.entity.EquipmentSlot.HEAD, helmet);
        ItemStack targetChest = new ItemStack(Items.DIAMOND_CHESTPLATE);
        target.equipStack(net.minecraft.entity.EquipmentSlot.CHEST, targetChest);

        context.runAtTick(5L, () -> {
            GemPowers.maintain(player);
            if (!player.hasStatusEffect(StatusEffects.LUCK) || !player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE)) {
                context.throwGameTestException("Wealth status effect passives should apply");
            }
            var mending = world.getRegistryManager().getOptionalEntry(Enchantments.MENDING).orElseThrow();
            var looting = world.getRegistryManager().getOptionalEntry(Enchantments.LOOTING).orElseThrow();
            var fortune = world.getRegistryManager().getOptionalEntry(Enchantments.FORTUNE).orElseThrow();
            int mendingLevel = EnchantmentHelper.getLevel(mending, sword);
            int lootingLevel = EnchantmentHelper.getLevel(looting, sword);
            int fortuneLevel = EnchantmentHelper.getLevel(fortune, pickaxe);
            if (mendingLevel <= 0 || lootingLevel < 3 || fortuneLevel < 3) {
                context.throwGameTestException("Wealth auto-enchants should apply");
            }

            int beforeMend = helmet.getDamage();
            int beforeTargetDamage = targetChest.getDamage();
            player.attack(target);
            if (helmet.getDamage() >= beforeMend) {
                context.throwGameTestException("Armor Mend on Hit should repair armor");
            }
            if (targetChest.getDamage() <= beforeTargetDamage) {
                context.throwGameTestException("Durability Chip should damage enemy armor");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void doubleDebrisDuplicatesScrap(TestContext context) {
        ServerWorld world = context.getWorld();
        ServerPlayerEntity player = GemsGameTestUtil.createMockCreativeServerPlayer(context);
        GemsGameTestUtil.forceSurvival(player);

        Vec3d pos = context.getAbsolute(new Vec3d(0.5D, 2.0D, 0.5D));
        teleport(player, world, pos.x, pos.y, pos.z, 0.0F, 0.0F);

        GemPlayerState.initIfNeeded(player);
        GemPlayerState.setActiveGem(player, GemId.WEALTH);
        GemPlayerState.setEnergy(player, 5);
        GemPowers.sync(player);

        context.runAtTick(5L, () -> {
            player.getInventory().clear();
            int before = countItem(player, Items.NETHERITE_SCRAP);
            ItemStack taken = new ItemStack(Items.NETHERITE_SCRAP, 2);
            player.getInventory().insertStack(taken.copy());

            FurnaceOutputSlot slot = new FurnaceOutputSlot(player, new SimpleInventory(1), 0, 0, 0);
            slot.onTakeItem(player, taken);

            int after = countItem(player, Items.NETHERITE_SCRAP);
            if (after - before < 4) {
                context.throwGameTestException("Double Debris should duplicate netherite scrap output");
            }
            context.complete();
        });
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void fumbleHasCooldownConfigured(TestContext context) {
        // Direct activate() bypasses cooldown system (handled by GemAbilities.activateByIndex)
        // Just verify the cooldown config is valid
        var cfg = GemsBalance.v().wealth();
        if (cfg.fumbleCooldownTicks() <= 0) {
            context.throwGameTestException("Fumble cooldown must be positive");
        }
        context.complete();
    }

    @GameTest(structure = "fabric-gametest-api-v1:empty", maxTicks = 200)
    public void wealthConfigValuesValid(TestContext context) {
        var cfg = GemsBalance.v().wealth();
        
        if (cfg.fumbleCooldownTicks() < 0) {
            context.throwGameTestException("Fumble cooldown cannot be negative");
        }
        if (cfg.fumbleDurationTicks() < 0) {
            context.throwGameTestException("Fumble duration cannot be negative");
        }
        if (cfg.fumbleRadiusBlocks() < 0) {
            context.throwGameTestException("Fumble radius cannot be negative");
        }
        if (cfg.hotbarLockCooldownTicks() < 0) {
            context.throwGameTestException("Hotbar Lock cooldown cannot be negative");
        }
        if (cfg.amplificationCooldownTicks() < 0) {
            context.throwGameTestException("Amplification cooldown cannot be negative");
        }
        if (cfg.richRushCooldownTicks() < 0) {
            context.throwGameTestException("Rich Rush cooldown cannot be negative");
        }
        if (cfg.pocketsRows() < 0) {
            context.throwGameTestException("Pockets rows cannot be negative");
        }
        
        context.complete();
    }
}
