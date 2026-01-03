package com.feel.gems.mixin;

import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;




@Mixin(ItemStack.class)
public abstract class PlayerEntityDoubleSaturationMixin {
    @Inject(method = "finishUsing", at = @At("TAIL"))
    private void gems$doubleSaturation(World world, net.minecraft.entity.LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (world == null || world.isClient()) {
            return;
        }
        if (!(user instanceof ServerPlayerEntity player)) {
            return;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.DOUBLE_SATURATION)) {
            return;
        }

        ItemStack self = (ItemStack) (Object) this;
        FoodComponent food = self.get(DataComponentTypes.FOOD);
        if (food == null) {
            return;
        }

        HungerManager hunger = player.getHungerManager();
        float boosted = hunger.getSaturationLevel() + food.saturation();
        hunger.setSaturationLevel(Math.min(hunger.getFoodLevel(), boosted));
    }
}
