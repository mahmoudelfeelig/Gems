package com.feel.gems.power;

import net.minecraft.item.AxeItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolItem;

public final class AutoEnchantPredicates {
    private AutoEnchantPredicates() {
    }

    public static boolean isMeleeWeapon(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof SwordItem || item instanceof AxeItem;
    }

    public static boolean isTool(ItemStack stack) {
        return stack.getItem() instanceof ToolItem;
    }

    public static boolean isGearOrTool(ItemStack stack) {
        return isTool(stack) || stack.isDamageable();
    }

    public static boolean isBow(ItemStack stack) {
        return stack.getItem() instanceof BowItem;
    }
}
