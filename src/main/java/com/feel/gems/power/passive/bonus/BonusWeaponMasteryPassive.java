package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Weapon Mastery - Deal increased damage with all weapons.
 * Implementation via attribute modifier.
 */
public final class BonusWeaponMasteryPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_WEAPON_MASTERY;
    }

    @Override
    public String name() {
        return "Weapon Mastery";
    }

    @Override
    public String description() {
        return "Deal increased damage with all weapons.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        // Marker passive - implemented via attribute modifiers
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        // Marker passive
    }
}
