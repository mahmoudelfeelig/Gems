package com.feel.gems.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryRecipeBookMixin {
    private static final java.util.Set<String> OWNER_KEYS = java.util.Set.of(
            "gemsOwner",
            "gemsOwnerName",
            "gemsFirstOwner",
            "gemsFirstOwnerName",
            "gemsPrevOwner",
            "gemsPrevOwnerName",
            "gemsSignature",
            "signature",
            "GemOwner",
            "GemOwnerEpoch"
    );

    @Inject(method = "getSlotWithStack(Lnet/minecraft/item/ItemStack;)I", at = @At("HEAD"), cancellable = true, require = 0)
    private void gems$getSlotWithStackIgnoreOwner(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.isEmpty()) {
            cir.setReturnValue(-1);
            return;
        }
        PlayerInventory self = (PlayerInventory) (Object) this;
        var main = self.getMainStacks();
        boolean targetHasCustomData = stack.contains(DataComponentTypes.CUSTOM_DATA);
        for (int i = 0; i < main.size(); i++) {
            ItemStack candidate = main.get(i);
            if (candidate.isEmpty()) {
                continue;
            }
            if (!ItemStack.areItemsAndComponentsEqual(stack, candidate)) {
                if (targetHasCustomData || !matchesOwnerOnly(stack, candidate)) {
                    continue;
                }
            }
            if (candidate.isDamaged() || candidate.hasEnchantments() || candidate.contains(DataComponentTypes.CUSTOM_NAME)) {
                continue;
            }
            cir.setReturnValue(i);
            return;
        }
        cir.setReturnValue(-1);
    }

    private static boolean matchesOwnerOnly(ItemStack target, ItemStack candidate) {
        if (!candidate.isOf(target.getItem())) {
            return false;
        }
        NbtComponent custom = candidate.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) {
            return false;
        }
        NbtCompound nbt = custom.copyNbt();
        boolean hasOwnerKey = false;
        for (String key : nbt.getKeys()) {
            if (OWNER_KEYS.contains(key)) {
                hasOwnerKey = true;
                continue;
            }
            return false;
        }
        return hasOwnerKey;
    }
}
