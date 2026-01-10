package com.feel.gems.power.passive.bonus;

import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Second Wind - Once per life, survive a killing blow with 1 HP.
 * Implementation via damage event hook.
 */
public final class BonusSecondWindPassive implements GemPassive {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_SECOND_WIND;
    }

    @Override
    public String name() {
        return "Second Wind";
    }

    @Override
    public String description() {
        return "Once per life, survive a killing blow with 1 HP.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        // Reset second wind availability
        player.removeCommandTag("gems_second_wind_used");
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        player.removeCommandTag("gems_second_wind_used");
    }
}
