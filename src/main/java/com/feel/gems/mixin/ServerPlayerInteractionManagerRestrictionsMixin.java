package com.feel.gems.mixin;

import com.feel.gems.power.gem.wealth.WealthFumble;
import com.feel.gems.power.runtime.AbilityRestrictions;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;




@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerRestrictionsMixin {
    @Shadow
    @Final
    protected ServerPlayerEntity player;

    @Inject(method = "tryBreakBlock", at = @At("HEAD"), cancellable = true)
    private void gems$stunBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (AbilityRestrictions.isStunned(player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
    private void gems$stunOrLockItem(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (AbilityRestrictions.isStunned(player)) {
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }
        if (WealthFumble.isActive(player)) {
            if (hand == Hand.OFF_HAND || stack.contains(DataComponentTypes.FOOD)) {
                cir.setReturnValue(ActionResult.FAIL);
            }
            return;
        }
    }

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void gems$stunOrFumbleBlock(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if (AbilityRestrictions.isStunned(player)) {
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }
        if (WealthFumble.isActive(player)) {
            if (hand == Hand.OFF_HAND || stack.contains(DataComponentTypes.FOOD)) {
                cir.setReturnValue(ActionResult.FAIL);
            }
            return;
        }
    }

}
