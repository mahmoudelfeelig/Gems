package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Unbreakable - Equipment durability loss reduced by 50%.
 * Implementation via item damage event hook.
 */
public final class BonusUnbreakablePassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_UNBREAKABLE;
    }

    @Override
    public String name() {
        return "Unbreakable";
    }

    @Override
    public String description() {
        return "Equipment loses durability 50% slower.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        // Marker passive - implemented via item damage event hooks
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        // Marker passive
    }
}
