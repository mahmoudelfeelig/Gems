package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Combat Meditate - Regenerate health faster while not in combat.
 * Implementation via event hook.
 */
public final class BonusCombatMeditatePassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_COMBAT_MEDITATE;
    }

    @Override
    public String name() {
        return "Combat Meditate";
    }

    @Override
    public String description() {
        return "Regenerate health faster while not in combat.";
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
