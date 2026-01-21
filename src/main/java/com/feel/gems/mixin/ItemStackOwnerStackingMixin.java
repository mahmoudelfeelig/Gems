package com.feel.gems.mixin;

import java.util.Set;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackOwnerStackingMixin {
    private static final Set<String> OWNER_KEYS = Set.of(
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

    @Unique
    private static final ThreadLocal<Boolean> GEMS_IGNORE_OWNER_COMPARE =
            ThreadLocal.withInitial(() -> false);

    @Inject(method = "areItemsAndComponentsEqual", at = @At("RETURN"), cancellable = true)
    private static void gems$ignoreOwnerTags(ItemStack left, ItemStack right, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() || GEMS_IGNORE_OWNER_COMPARE.get()) {
            return;
        }
        if (left.isEmpty() || right.isEmpty() || !left.isOf(right.getItem())) {
            return;
        }
        ItemStack leftCopy = left.copy();
        ItemStack rightCopy = right.copy();
        boolean changedLeft = stripOwnerTags(leftCopy);
        boolean changedRight = stripOwnerTags(rightCopy);
        if (!changedLeft && !changedRight) {
            return;
        }
        GEMS_IGNORE_OWNER_COMPARE.set(true);
        try {
            if (ItemStack.areItemsAndComponentsEqual(leftCopy, rightCopy)) {
                cir.setReturnValue(true);
            }
        } finally {
            GEMS_IGNORE_OWNER_COMPARE.set(false);
        }
    }

    @Unique
    private static boolean stripOwnerTags(ItemStack stack) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) {
            return false;
        }
        NbtCompound nbt = custom.copyNbt();
        boolean changed = false;
        for (String key : OWNER_KEYS) {
            if (nbt.contains(key)) {
                nbt.remove(key);
                changed = true;
            }
        }
        if (!changed) {
            return false;
        }
        if (nbt.isEmpty()) {
            stack.remove(DataComponentTypes.CUSTOM_DATA);
        } else {
            NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, out -> out.copyFrom(nbt));
        }
        return true;
    }
}
