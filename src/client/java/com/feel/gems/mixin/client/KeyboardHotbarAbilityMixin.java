package com.feel.gems.mixin.client;

import com.feel.gems.client.GemsKeybinds;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(Keyboard.class)
public abstract class KeyboardHotbarAbilityMixin {
    @Inject(method = "onKey(JIIII)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void gems$consumeHotbarWhenModifierDown(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (action != GLFW.GLFW_PRESS) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen != null) {
            return;
        }
        if (!GemsKeybinds.isModifierDown()) {
            return;
        }

        GameOptions options = client.options;
        if (options == null || options.hotbarKeys == null) {
            return;
        }

        int limit = Math.min(9, options.hotbarKeys.length);
        for (int i = 0; i < limit; i++) {
            KeyBinding hotbar = options.hotbarKeys[i];
            if (hotbar.matchesKey(key, scancode)) {
                GemsKeybinds.activateSlotChord(client, i + 1);
                ci.cancel();
                return;
            }
        }
    }
}
