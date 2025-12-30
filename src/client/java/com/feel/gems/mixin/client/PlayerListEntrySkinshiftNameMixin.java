package com.feel.gems.mixin.client;

import com.feel.gems.client.ClientDisguiseState;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;




@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntrySkinshiftNameMixin {
    @Inject(method = "getDisplayName()Lnet/minecraft/text/Text;", at = @At("HEAD"), cancellable = true)
    private void gems$overrideDisplayName(CallbackInfoReturnable<Text> cir) {
        PlayerListEntry self = (PlayerListEntry) (Object) this;
        GameProfile profile = self.getProfile();
        UUID id = profile == null ? null : profile.getId();
        Text override = ClientDisguiseState.overrideName(id);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
