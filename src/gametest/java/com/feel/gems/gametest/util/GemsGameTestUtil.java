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
import net.minecraft.test.TestContext;
import net.minecraft.world.GameMode;




public final class GemsGameTestUtil {
    private GemsGameTestUtil() {
    }

    public static void forceSurvival(ServerPlayerEntity player) {
        player.changeGameMode(GameMode.SURVIVAL);
        player.setInvulnerable(false);
        var abilities = player.getAbilities();
        abilities.invulnerable = false;
        abilities.creativeMode = false;
        abilities.allowFlying = false;
        abilities.flying = false;
        player.sendAbilitiesUpdate();
    }

    public static boolean hasItem(ServerPlayerEntity player, Item item) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(item)) {
                return true;
            }
        }
        return false;
    }

    public static int countItem(ServerPlayerEntity player, Item item) {
        int count = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isOf(item)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    public static int countGlint(ServerPlayerEntity player, Item item) {
        int glint = 0;
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
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

    public static int countGemItems(ServerPlayerEntity player) {
        var inventory = player.getInventory();
        int gemItems = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.getItem() instanceof GemItem) {
                gemItems++;
            }
        }
        return gemItems;
    }

    public static boolean containsAirMace(ServerPlayerEntity player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            if (isAirMace(inventory.getStack(i))) {
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

    @SuppressWarnings("removal")
    public static ServerPlayerEntity createMockCreativeServerPlayer(TestContext context) {
        return context.createMockCreativeServerPlayerInWorld();
    }

    private static boolean isAirMace(ItemStack stack) {
        if (!stack.isOf(Items.MACE)) {
            return false;
        }
        var custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        return custom != null && custom.copyNbt().getBoolean("gemsAirMace", false);
    }
}
