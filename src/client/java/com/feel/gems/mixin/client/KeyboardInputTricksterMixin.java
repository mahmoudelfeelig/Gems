package com.feel.gems.mixin.client;

import com.feel.gems.client.ClientTricksterState;
import com.feel.gems.mixin.client.accessor.InputAccessor;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(KeyboardInput.class)
public abstract class KeyboardInputTricksterMixin {
    @Inject(method = "tick()V", at = @At("TAIL"))
    private void gems$applyTricksterControl(CallbackInfo ci) {
        InputAccessor input = (InputAccessor) (Object) this;
        if (ClientTricksterState.isPuppeted()) {
            input.gems$setPlayerInput(PlayerInput.DEFAULT);
            input.gems$setMovementVector(Vec2f.ZERO);
            return;
        }
        if (ClientTricksterState.isMindGames()) {
            // Reverse controls: invert movement direction and swap the logical pressed keys.
            PlayerInput current = input.gems$getPlayerInput();
            input.gems$setPlayerInput(new PlayerInput(
                    current.backward(),
                    current.forward(),
                    current.right(),
                    current.left(),
                    current.jump(),
                    current.sneak(),
                    current.sprint()
            ));
            Vec2f movement = input.gems$getMovementVector();
            input.gems$setMovementVector(new Vec2f(-movement.x, -movement.y));
        }
    }
}
