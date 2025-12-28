package com.feel.gems.mixin;

import com.feel.gems.power.gem.spy.SpyBackstab;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(PlayerEntity.class)
public abstract class PlayerEntityBackstabMixin {
    @Inject(method = "attack", at = @At("TAIL"))
    private void gems$backstab(Entity target, CallbackInfo ci) {
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity attacker)) {
            return;
        }
        SpyBackstab.apply(attacker, living);
    }
}
