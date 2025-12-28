package com.feel.gems.mixin;

import com.feel.gems.legendary.LegendaryCrafting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;




@Mixin(Slot.class)
public abstract class SlotLegendaryCraftMixin {
    @Inject(method = "canTakeItems", at = @At("HEAD"), cancellable = true)
    private void gems$blockLegendary(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if (player.getWorld().isClient) {
            return;
        }
        if (!((Object) this instanceof CraftingResultSlot slot)) {
            return;
        }
        if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)) {
            return;
        }
        ItemStack stack = slot.getStack();
        if (stack.isEmpty()) {
            return;
        }
        if (!LegendaryCrafting.canStartCraft(serverPlayer, stack)) {
            cir.setReturnValue(false);
        }
    }
}
