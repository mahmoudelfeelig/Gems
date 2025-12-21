package com.feel.gems.mixin;

import com.feel.gems.power.GemPowers;
import com.feel.gems.power.PowerIds;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityEatFoodMixin {
    @Inject(method = "eatFood", at = @At("TAIL"))
    private void gems$reaperRotEater(World world, ItemStack stack, FoodComponent component, CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient) {
            return;
        }
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.REAPER_ROT_EATER)) {
            return;
        }

        if (stack.isOf(Items.ROTTEN_FLESH)) {
            player.removeStatusEffect(StatusEffects.HUNGER);
        } else if (stack.isOf(Items.SPIDER_EYE)) {
            player.removeStatusEffect(StatusEffects.POISON);
        }
    }
}
