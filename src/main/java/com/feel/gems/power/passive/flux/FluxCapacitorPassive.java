package com.feel.gems.power.passive.flux;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemMaintainedPassive;
import com.feel.gems.power.gem.flux.FluxCharge;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class FluxCapacitorPassive implements GemMaintainedPassive {
    @Override
    public Identifier id() {
        return PowerIds.FLUX_CAPACITOR;
    }

    @Override
    public String name() {
        return "Flux Capacitor";
    }

    @Override
    public String description() {
        return "At high charge, gain a temporary absorption shield.";
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        maintain(player);
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        player.removeStatusEffect(StatusEffects.ABSORPTION);
    }

    @Override
    public void maintain(ServerPlayerEntity player) {
        var cfg = GemsBalance.v().flux();
        if (FluxCharge.get(player) < cfg.fluxCapacitorChargeThreshold()) {
            return;
        }
        int amplifier = cfg.fluxCapacitorAbsorptionAmplifier();
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 60, amplifier, true, false, false));
    }
}
