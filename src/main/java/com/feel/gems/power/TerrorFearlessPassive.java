package com.feel.gems.power;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class TerrorFearlessPassive implements GemMaintainedPassive {
    @Override
    public Identifier id() {
        return PowerIds.TERROR_FEARLESS;
    }

    @Override
    public String name() {
        return "Fearless";
    }

    @Override
    public String description() {
        return "Cleanses Blindness and Darkness from the holder.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        maintain(player);
    }

    @Override
    public void maintain(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.DARKNESS);
        player.removeStatusEffect(StatusEffects.BLINDNESS);
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        // No-op.
    }
}

