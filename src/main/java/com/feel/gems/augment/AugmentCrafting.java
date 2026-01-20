package com.feel.gems.augment;

import com.feel.gems.item.AugmentItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public final class AugmentCrafting {
    private AugmentCrafting() {
    }

    public static void onCrafted(ServerPlayerEntity player, ItemStack stack) {
        if (!(stack.getItem() instanceof AugmentItem augmentItem)) {
            return;
        }
        if (AugmentRuntime.getInstance(stack) != null) {
            return;
        }
        AugmentInstance instance = AugmentRuntime.rollInstance(augmentItem.augmentId());
        if (instance == null) {
            return;
        }
        AugmentRuntime.assignInstance(stack, instance);
    }
}
