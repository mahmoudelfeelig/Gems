package com.feel.gems.mixin.client.accessor;

import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Input.class)
public interface InputAccessor {
    @Accessor("playerInput")
    PlayerInput gems$getPlayerInput();

    @Accessor("playerInput")
    void gems$setPlayerInput(PlayerInput input);

    @Accessor("movementVector")
    Vec2f gems$getMovementVector();

    @Accessor("movementVector")
    void gems$setMovementVector(Vec2f movementVector);
}

