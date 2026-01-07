package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * War Cry - Nearby allies gain Strength I when you're hit.
 * Implementation via damage receive event hook.
 */
public final class BonusWarCryPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_WAR_CRY;
    }

    @Override
    public String name() {
        return "War Cry";
    }

    @Override
    public String description() {
        return "When you take damage, nearby allies gain Strength I for 5s.";
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
