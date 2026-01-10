package com.feel.gems.mixin.client;

import com.feel.gems.client.ClientDisguiseState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;




@Mixin(Entity.class)
public abstract class AbstractClientPlayerEntityNameMixin {
    @Inject(method = "getName()Lnet/minecraft/text/Text;", at = @At("HEAD"), cancellable = true)
    private void gems$overrideName(CallbackInfoReturnable<Text> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof AbstractClientPlayerEntity)) {
            return;
        }
        Text override = ClientDisguiseState.overrideName(self.getUuid());
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    @Inject(method = "getDisplayName()Lnet/minecraft/text/Text;", at = @At("HEAD"), cancellable = true)
    private void gems$overrideDisplayName(CallbackInfoReturnable<Text> cir) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof AbstractClientPlayerEntity)) {
            return;
        }
        Text override = ClientDisguiseState.overrideName(self.getUuid());
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
