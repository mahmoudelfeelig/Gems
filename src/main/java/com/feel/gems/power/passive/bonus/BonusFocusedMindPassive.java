package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Focused Mind - Ability cooldowns reduced by 15%.
 * Implementation via cooldown calculation in ability system.
 */
public final class BonusFocusedMindPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_FOCUSED_MIND;
    }

    @Override
    public String name() {
        return "Focused Mind";
    }

    @Override
    public String description() {
        return "All ability cooldowns are reduced by 15%.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        // Marker passive - implemented in cooldown calculation
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        // Marker passive
    }
}
