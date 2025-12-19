package com.blissmc.gems.power;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class DimensionalDriftAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.DIMENSIONAL_DRIFT;
    }

    @Override
    public String name() {
        return "Dimensional Drift";
    }

    @Override
    public String description() {
        return "Short burst of invisibility and extreme speed.";
    }

    @Override
    public int cooldownTicks() {
        return 20 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int duration = 6 * 20;
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, duration, 0, true, false, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 3, true, false, false));
        player.sendMessage(Text.literal("Drifting..."), true);
        return true;
    }
}

