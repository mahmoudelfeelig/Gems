package com.feel.gems.mixin.client;

import com.feel.gems.client.ClientDisguiseState;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(PlayerListEntry.class)
public abstract class AbstractClientPlayerEntitySkinMixin {
    @Inject(method = "getSkinTextures()Lnet/minecraft/entity/player/SkinTextures;", at = @At("HEAD"), cancellable = true)
    private void gems$overrideSkin(CallbackInfoReturnable<SkinTextures> cir) {
        PlayerListEntry self = (PlayerListEntry) (Object) this;
        GameProfile profile = self.getProfile();
        UUID id = profile == null ? null : profile.id();
        SkinTextures override = ClientDisguiseState.overrideSkin(id);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
