package com.feel.gems.mixin;

import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.util.GemsTime;
import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;




@Mixin(ItemStack.class)
public abstract class PlayerEntityEatFoodMixin {
    private static final String KEY_ROT_EATER_UNTIL = "reaperRotEaterUntil";

    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void gems$reaperRotEater(World world, net.minecraft.entity.LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (world == null || world.isClient()) {
            return;
        }
        if (!(user instanceof ServerPlayerEntity player)) {
            return;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.REAPER_ROT_EATER)) {
            return;
        }

        ItemStack self = (ItemStack) (Object) this;

        if (self.isOf(Items.ROTTEN_FLESH)) {
            markRotEater(player);
        } else if (self.isOf(Items.SPIDER_EYE)) {
            markRotEater(player);
        }
    }

    @Inject(method = "finishUsing", at = @At("TAIL"))
    private void gems$reaperRotEaterTail(World world, net.minecraft.entity.LivingEntity user, CallbackInfoReturnable<ItemStack> cir) {
        if (world == null || world.isClient()) {
            return;
        }
        if (!(user instanceof ServerPlayerEntity player)) {
            return;
        }
        // Vanilla applies food status effects during finishUsing; clear again at TAIL to ensure Hunger/Poison
        // doesn't "win" after our HEAD removal.
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        long until = data.getLong(KEY_ROT_EATER_UNTIL, 0L);
        if (until <= 0L) {
            return;
        }
        long now = GemsTime.now(player);
        if (now > until) {
            return;
        }
        player.removeStatusEffect(StatusEffects.HUNGER);
        player.removeStatusEffect(StatusEffects.POISON);
    }

    private static void markRotEater(ServerPlayerEntity player) {
        long now = GemsTime.now(player);
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        data.putLong(KEY_ROT_EATER_UNTIL, now + 40L);
        player.removeStatusEffect(StatusEffects.HUNGER);
        player.removeStatusEffect(StatusEffects.POISON);
    }
}
