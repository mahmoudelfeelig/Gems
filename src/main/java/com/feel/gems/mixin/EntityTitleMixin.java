package com.feel.gems.mixin;

import com.feel.gems.mastery.TitleDisplay;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityTitleMixin {
    @Inject(method = "getDisplayName()Lnet/minecraft/text/Text;", at = @At("RETURN"), cancellable = true)
    private void gems$addTitleToDisplayName(CallbackInfoReturnable<Text> cir) {
        Text base = cir.getReturnValue();
        if (base == null) {
            return;
        }
        if (!((Object) this instanceof ServerPlayerEntity player)) {
            return;
        }
        cir.setReturnValue(TitleDisplay.withTitlePrefix(player, base));
    }
}
