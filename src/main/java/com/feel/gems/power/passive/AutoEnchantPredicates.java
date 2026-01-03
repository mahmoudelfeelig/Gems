package com.feel.gems.power.passive;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.CrossbowItem;




public final class AutoEnchantPredicates {
    private AutoEnchantPredicates() {
    }

    public static boolean isMeleeWeapon(ItemStack stack) {
        Item item = stack.getItem();
        return stack.contains(DataComponentTypes.WEAPON) && !(item instanceof BowItem) && !(item instanceof CrossbowItem);
    }

    public static boolean isTool(ItemStack stack) {
        return stack.contains(DataComponentTypes.TOOL);
    }

    public static boolean isGearOrTool(ItemStack stack) {
        return isTool(stack) || stack.isDamageable();
    }

    public static boolean isBow(ItemStack stack) {
        return stack.getItem() instanceof BowItem;
    }

    public static boolean isCrossbow(ItemStack stack) {
        return stack.getItem() instanceof CrossbowItem;
    }
}
