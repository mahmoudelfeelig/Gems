package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Culling Blade - Deal increased damage to low-health enemies.
 * Implementation via event hook.
 */
public final class BonusCullingBladePassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_CULLING_BLADE;
    }

    @Override
    public String name() {
        return "Culling Blade";
    }

    @Override
    public String description() {
        return "Deal increased damage to enemies below 25% health.";
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
