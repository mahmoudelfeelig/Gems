package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Elemental Harmony - Resistance to fire, ice, and lightning damage.
 */
public final class BonusElementalHarmonyPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_ELEMENTAL_HARMONY;
    }

    @Override
    public String name() {
        return "Elemental Harmony";
    }

    @Override
    public String description() {
        return "Take 50% less damage from fire, freezing, and lightning.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false, true));
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.FIRE_RESISTANCE);
    }
}
