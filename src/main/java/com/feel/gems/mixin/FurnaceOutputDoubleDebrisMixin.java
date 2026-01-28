package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.PlayerStateManager;
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
    private static final String KEY_DOUBLE_DEBRIS = "gems_double_debris";

    @Inject(method = "onTakeItem", at = @At("TAIL"))
    private void gems$doubleDebris(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        String ownerName = serverPlayer.getName().getString();
        AbilityRuntime.setOwnerWithName(stack, serverPlayer.getUuid(), ownerName);

        if (!GemPowers.isPassiveActive(serverPlayer, PowerIds.DOUBLE_DEBRIS)) {
            return;
        }
        if (!stack.isOf(Items.NETHERITE_SCRAP)) {
            return;
        }

        if (AbilityRuntime.isRichRushActive(serverPlayer)) {
            int rolls = Math.max(1, GemsBalance.v().wealth().richRushLootRolls());
            int extraCount = (rolls - 1) * stack.getCount();
            if (extraCount > 0) {
                ItemStack extraRich = new ItemStack(Items.NETHERITE_SCRAP, extraCount);
                AbilityRuntime.setOwnerWithName(extraRich, serverPlayer.getUuid(), ownerName);
                boolean inserted = serverPlayer.getInventory().insertStack(extraRich);
                if (!inserted && !extraRich.isEmpty()) {
                    serverPlayer.dropItem(extraRich, false);
                }
            }
        }

        // Duplicate the entire stack count, not just one item.
        // This handles both normal clicks and shift-clicks.
        ItemStack extra = new ItemStack(Items.NETHERITE_SCRAP, stack.getCount());
        AbilityRuntime.setOwnerWithName(extra, serverPlayer.getUuid(), ownerName);
        boolean inserted = serverPlayer.getInventory().insertStack(extra);
        if (!inserted && !extra.isEmpty()) {
            serverPlayer.dropItem(extra, false);
        }
        PlayerStateManager.setTemporary(serverPlayer, KEY_DOUBLE_DEBRIS, 1);
    }
}
