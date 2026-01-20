package com.feel.gems.mixin;

import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.AbstractFurnaceScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceScreenHandler.class)
public abstract class AbstractFurnaceScreenHandlerDoubleDebrisMixin {
    private static final String KEY_DOUBLE_DEBRIS = "gems_double_debris";
    private static final int OUTPUT_SLOT = 2;

    @Inject(method = "quickMove", at = @At("RETURN"))
    private void gems$doubleDebrisShiftClick(PlayerEntity player, int slot, CallbackInfoReturnable<ItemStack> cir) {
        if (slot != OUTPUT_SLOT) {
            return;
        }
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        if (PlayerStateManager.getTemporary(serverPlayer, KEY_DOUBLE_DEBRIS) > 0) {
            return;
        }
        ItemStack moved = cir.getReturnValue();
        if (moved == null || moved.isEmpty() || !moved.isOf(Items.NETHERITE_SCRAP)) {
            return;
        }
        if (!GemPowers.isPassiveActive(serverPlayer, PowerIds.DOUBLE_DEBRIS)) {
            return;
        }
        String ownerName = serverPlayer.getName().getString();
        ItemStack extra = new ItemStack(Items.NETHERITE_SCRAP, moved.getCount());
        AbilityRuntime.setOwnerWithName(extra, serverPlayer.getUuid(), ownerName);
        boolean inserted = serverPlayer.getInventory().insertStack(extra);
        if (!inserted && !extra.isEmpty()) {
            serverPlayer.dropItem(extra, false);
        }
        PlayerStateManager.setTemporary(serverPlayer, KEY_DOUBLE_DEBRIS, 1);
    }
}
