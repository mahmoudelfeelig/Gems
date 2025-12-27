package com.feel.gems.mixin;

import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(FurnaceOutputSlot.class)
public abstract class FurnaceOutputDoubleDebrisMixin {
    @Inject(method = "onTakeItem", at = @At("TAIL"))
    private void gems$doubleDebris(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        AbilityRuntime.setOwnerIfMissing(stack, serverPlayer.getUuid());

        if (!GemPowers.isPassiveActive(serverPlayer, PowerIds.DOUBLE_DEBRIS)) {
            return;
        }
        if (!stack.isOf(Items.NETHERITE_SCRAP)) {
            return;
        }

        ItemStack extra = stack.copy();
        boolean inserted = serverPlayer.getInventory().insertStack(extra);
        if (!inserted && !extra.isEmpty()) {
            serverPlayer.dropItem(extra, false);
        }
    }
}
