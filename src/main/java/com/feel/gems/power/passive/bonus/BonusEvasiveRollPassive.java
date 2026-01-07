package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Evasive Roll - When hit while sprinting, automatically dodge backward.
 * Different from Dodge Chance which is passive 10% dodge.
 * This triggers a visible backward roll animation/motion.
 */
public final class BonusEvasiveRollPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_EVASIVE_ROLL;
    }

    @Override
    public String name() {
        return "Evasive Roll";
    }

    @Override
    public String description() {
        return "When hit while sprinting, dodge backward and avoid the damage.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        // Marker passive - implemented via event hooks
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        // Marker passive
    }
}
