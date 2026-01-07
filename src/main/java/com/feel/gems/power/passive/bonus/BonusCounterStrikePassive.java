package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Counter Strike - After blocking an attack, your next hit deals 2x damage.
 * Implementation via block and attack event hooks.
 */
public final class BonusCounterStrikePassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_COUNTER_STRIKE;
    }

    @Override
    public String name() {
        return "Counter Strike";
    }

    @Override
    public String description() {
        return "After blocking an attack, your next hit deals double damage.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        player.removeCommandTag("gems_counter_strike_ready");
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        player.removeCommandTag("gems_counter_strike_ready");
    }
}
