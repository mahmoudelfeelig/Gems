package com.feel.gems.item;

import com.feel.gems.core.GemId;
import com.feel.gems.state.GemPlayerState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;




public final class GemItemGlint {
    private GemItemGlint() {
    }

    public static void sync(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);
        GemId activeGem = GemPlayerState.getActiveGem(player);
        boolean shouldGlint = GemPlayerState.getEnergy(player) >= GemPlayerState.MAX_ENERGY;

        syncInventorySection(player.getInventory().getMainStacks(), activeGem, shouldGlint);
        syncInventoryStack(player.getOffHandStack(), activeGem, shouldGlint);
        syncInventoryStack(player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD), activeGem, shouldGlint);
        syncInventoryStack(player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST), activeGem, shouldGlint);
        syncInventoryStack(player.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS), activeGem, shouldGlint);
        syncInventoryStack(player.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET), activeGem, shouldGlint);
    }

    private static void syncInventorySection(Iterable<ItemStack> stacks, GemId activeGem, boolean shouldGlint) {
        for (ItemStack stack : stacks) {
            syncInventoryStack(stack, activeGem, shouldGlint);
        }
    }

    private static void syncInventoryStack(ItemStack stack, GemId activeGem, boolean shouldGlint) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (!(stack.getItem() instanceof GemItem gemItem)) {
            return;
        }
        boolean isActiveGemItem = gemItem.gemId() == activeGem;
        setGlintFlag(stack, isActiveGemItem && shouldGlint);
    }

    private static void setGlintFlag(ItemStack stack, boolean value) {
        if (value) {
            stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        } else {
            stack.remove(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
        }
    }
}
