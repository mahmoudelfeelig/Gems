package com.feel.gems.mixin;

import com.feel.gems.power.runtime.AbilityRestrictions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(PlayerEntity.class)
public abstract class PlayerEntityRestrictionsMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void gems$stunOrLockAttack(Entity target, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }
        if (AbilityRestrictions.isStunned(player)) {
            ci.cancel();
        }
    }
}
