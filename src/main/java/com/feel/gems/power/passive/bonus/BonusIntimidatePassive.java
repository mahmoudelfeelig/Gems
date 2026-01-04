package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Intimidate - Intimidate nearby enemies, reducing their effectiveness.
 * Implementation via event hook.
 */
public final class BonusIntimidatePassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_INTIMIDATE;
    }

    @Override
    public String name() {
        return "Intimidate";
    }

    @Override
    public String description() {
        return "Nearby enemies deal reduced damage and have slower attack speed.";
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
