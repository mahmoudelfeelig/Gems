package com.feel.gems.mixin;

import com.feel.gems.event.GemsPlayerDeath;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityDeathMixin {
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void gems$stashGems(DamageSource source, CallbackInfo ci) {
        GemsPlayerDeath.onDeathHead((ServerPlayerEntity) (Object) this, source);
    }

    @Inject(method = "onDeath", at = @At("TAIL"))
    private void gems$onDeath(DamageSource source, CallbackInfo ci) {
        GemsPlayerDeath.onDeathTail((ServerPlayerEntity) (Object) this, source);
    }

}
