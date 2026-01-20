package com.feel.gems.mixin;

import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(ItemEntity.class)
public abstract class ItemEntityOwnerTagMixin {
    @Inject(method = "onPlayerCollision", at = @At("HEAD"))
    private void gems$tagPickedUp(PlayerEntity player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        ItemEntity self = (ItemEntity) (Object) this;
        AbilityRuntime.setOwnerWithName(self.getStack(), serverPlayer.getUuid(), serverPlayer.getName().getString());
    }
}

