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

        syncInventorySection(player.getInventory().main, activeGem, shouldGlint);
        syncInventorySection(player.getInventory().offHand, activeGem, shouldGlint);
        syncInventorySection(player.getInventory().armor, activeGem, shouldGlint);
    }

    private static void syncInventorySection(Iterable<ItemStack> stacks, GemId activeGem, boolean shouldGlint) {
        for (ItemStack stack : stacks) {
            if (!(stack.getItem() instanceof GemItem gemItem)) {
                continue;
            }
            boolean isActiveGemItem = gemItem.gemId() == activeGem;
            setGlintFlag(stack, isActiveGemItem && shouldGlint);
        }
    }

    private static void setGlintFlag(ItemStack stack, boolean value) {
        if (value) {
            stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        } else {
            stack.remove(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
        }
    }
}
