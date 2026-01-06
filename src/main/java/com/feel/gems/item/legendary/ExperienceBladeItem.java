package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.state.PlayerStateManager;
import java.util.function.Consumer;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

/**
 * Experience Blade - consume XP to gain Sharpness.
 * 10 levels -> Sharpness II, 20 -> IV, 30 -> VI, max Sharpness XX at 100 levels.
 * Enchantment persists until death.
 */
public final class ExperienceBladeItem extends Item implements LegendaryItem {
    private static final String SHARPNESS_LEVEL_KEY = "experience_blade_sharpness";

    public ExperienceBladeItem(ToolMaterial material, Settings settings) {
        super(settings.sword(material, 3.0F, -2.4F).enchantable(15));
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "experience_blade").toString();
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient() || !(user instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }

        ItemStack stack = player.getStackInHand(hand);
        int currentSharpness = getCurrentSharpness(player);

        if (currentSharpness >= 20) {
            player.sendMessage(Text.translatable("gems.item.experience_blade.max").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        // Calculate XP needed for next tier
        int nextTier = (currentSharpness / 2) + 1;
        int xpLevelsNeeded = nextTier * 10; // 10, 20, 30, 40, 50, 60, 70, 80, 90, 100

        if (player.experienceLevel < xpLevelsNeeded) {
            player.sendMessage(Text.translatable("gems.item.experience_blade.need_xp", xpLevelsNeeded, player.experienceLevel).formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        // Consume XP and upgrade
        player.addExperienceLevels(-xpLevelsNeeded);
        int newSharpness = currentSharpness + 2;
        setCurrentSharpness(player, newSharpness);

        // Apply enchantment to the blade
        applySharpnessToStack(stack, newSharpness, player);

        player.sendMessage(Text.translatable("gems.item.experience_blade.upgraded", toRoman(newSharpness)).formatted(Formatting.GREEN), true);
        return ActionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        // Sync sharpness level with player state
        int storedSharpness = getCurrentSharpness(player);
        if (storedSharpness > 0) {
            applySharpnessToStack(stack, storedSharpness, player);
        }
    }

    private void applySharpnessToStack(ItemStack stack, int level, ServerPlayerEntity player) {
        RegistryEntry<Enchantment> sharpnessEntry = player.getEntityWorld().getRegistryManager()
                .getOptionalEntry(Enchantments.SHARPNESS).orElse(null);
        if (sharpnessEntry == null) return;

        int current = EnchantmentHelper.getLevel(sharpnessEntry, stack);
        if (current < level) {
            EnchantmentHelper.apply(stack, builder -> builder.set(sharpnessEntry, level));
        }
    }

    public static int getCurrentSharpness(ServerPlayerEntity player) {
        String levelStr = PlayerStateManager.getPersistent(player, SHARPNESS_LEVEL_KEY);
        return levelStr != null ? Integer.parseInt(levelStr) : 0;
    }

    public static void setCurrentSharpness(ServerPlayerEntity player, int level) {
        PlayerStateManager.setPersistent(player, SHARPNESS_LEVEL_KEY, String.valueOf(level));
    }

    public static void clearSharpness(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, SHARPNESS_LEVEL_KEY);
    }

    private static String toRoman(int number) {
        return switch (number) {
            case 2 -> "II";
            case 4 -> "IV";
            case 6 -> "VI";
            case 8 -> "VIII";
            case 10 -> "X";
            case 12 -> "XII";
            case 14 -> "XIV";
            case 16 -> "XVI";
            case 18 -> "XVIII";
            case 20 -> "XX";
            default -> String.valueOf(number);
        };
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        tooltip.accept(Text.translatable("gems.item.experience_blade.tooltip.1").formatted(Formatting.GRAY));
        tooltip.accept(Text.translatable("gems.item.experience_blade.tooltip.2").formatted(Formatting.DARK_GRAY));
    }
}
