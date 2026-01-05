package com.feel.gems.power.passive.sentinel;

import com.feel.gems.power.api.GemMaintainedPassive;
import com.feel.gems.power.gem.sentinel.SentinelPassiveRuntime;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Fortress: While standing still for 2 seconds, gain Resistance II.
 */
public final class SentinelFortressPassive implements GemMaintainedPassive {
    @Override
    public Identifier id() {
        return PowerIds.SENTINEL_FORTRESS;
    }

    @Override
    public String name() {
        return "Fortress";
    }

    @Override
    public String description() {
        return "While standing still for 2 seconds, gain Resistance II.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        maintain(player);
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.RESISTANCE);
        SentinelPassiveRuntime.clearFortress(player);
    }

    @Override
    public void maintain(ServerPlayerEntity player) {
        SentinelPassiveRuntime.tickFortress(player);
    }
}
