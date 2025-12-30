package com.feel.gems.mixin.client;

import com.feel.gems.client.ClientDisguiseState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntitySkinMixin {
    @Inject(method = "getSkinTextures()Lnet/minecraft/client/util/SkinTextures;", at = @At("HEAD"), cancellable = true)
    private void gems$overrideSkin(CallbackInfoReturnable<SkinTextures> cir) {
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
        SkinTextures override = ClientDisguiseState.overrideSkin(self);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
