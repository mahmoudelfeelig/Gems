package com.feel.gems.mixin;

import com.feel.gems.mastery.TitleDisplay;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityTitleMixin {
    @Inject(method = "getPlayerListName()Lnet/minecraft/text/Text;", at = @At("RETURN"), cancellable = true)
    private void gems$addTitleToListName(CallbackInfoReturnable<Text> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        Text base = cir.getReturnValue();
        if (base == null) {
            base = player.getName();
        }
        Text prefix = TitleDisplay.titlePrefix(player);
        if (prefix == null) {
            return;
        }
        cir.setReturnValue(TitleDisplay.withTitlePrefix(player, base));
    }
}
