package com.feel.gems.gametest.util;

import com.feel.gems.assassin.AssassinState;
import com.feel.gems.item.GemItem;
import com.feel.gems.state.GemsPersistentDataHolder;
import java.util.List;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;




public final class GemsGameTestUtil {
    private GemsGameTestUtil() {
    }

    public static boolean hasItem(ServerPlayerEntity player, Item item) {
        for (ItemStack stack : player.getInventory().main) {
            if (stack.isOf(item)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offHand) {
            if (stack.isOf(item)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.isOf(item)) {
                return true;
            }
        }
        return false;
    }

    public static int countItem(ServerPlayerEntity player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().main) {
            if (stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().offHand) {
            if (stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static int countGlint(ServerPlayerEntity player, Item item) {
        int glint = 0;
        for (ItemStack stack : player.getInventory().main) {
            if (stack.isOf(item) && stack.contains(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE)) {
                glint++;
            }
        }
        for (ItemStack stack : player.getInventory().offHand) {
            if (stack.isOf(item) && stack.contains(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE)) {
                glint++;
            }
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.isOf(item) && stack.contains(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE)) {
                glint++;
            }
        }
        return glint;
    }

    public static int countGemItems(List<ItemStack> stacks) {
        int gemItems = 0;
        for (ItemStack stack : stacks) {
            if (stack.getItem() instanceof GemItem) {
                gemItems++;
            }
        }
        return gemItems;
    }

    public static boolean containsAirMace(ServerPlayerEntity player) {
        for (ItemStack stack : player.getInventory().main) {
            if (isAirMace(stack)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offHand) {
            if (isAirMace(stack)) {
                return true;
            }
        }
        return false;
    }

    public static void resetAssassinState(ServerPlayerEntity player) {
        AssassinState.initIfNeeded(player);
        var data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        data.putBoolean("assassinIsAssassin", false);
        data.putBoolean("assassinEliminated", false);
        data.putInt("assassinHearts", AssassinState.maxHearts());
    }

    private static boolean isAirMace(ItemStack stack) {
        if (!stack.isOf(Items.MACE)) {
            return false;
        }
        var custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        return custom != null && custom.getNbt().getBoolean("gemsAirMace");
    }
}
