package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

public final class TerrorBloodPrice {
    private TerrorBloodPrice() {
    }

    public static void onPlayerKill(ServerPlayerEntity killer) {
        int duration = GemsBalance.v().terror().bloodPriceDurationTicks();
        if (duration <= 0) {
            return;
        }
        int strengthAmp = GemsBalance.v().terror().bloodPriceStrengthAmplifier();
        int resistanceAmp = GemsBalance.v().terror().bloodPriceResistanceAmplifier();

        if (strengthAmp >= 0) {
            killer.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration, strengthAmp, true, false, false));
        }
        if (resistanceAmp >= 0) {
            killer.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, resistanceAmp, true, false, false));
        }
    }
}

