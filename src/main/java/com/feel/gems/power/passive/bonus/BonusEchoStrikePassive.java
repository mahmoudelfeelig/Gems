package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Echo Strike - Attacks have 15% chance to strike twice.
 * Implementation via attack event hook.
 */
public final class BonusEchoStrikePassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_ECHO_STRIKE;
    }

    @Override
    public String name() {
        return "Echo Strike";
    }

    @Override
    public String description() {
        return "Attacks have a 15% chance to strike twice.";
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
