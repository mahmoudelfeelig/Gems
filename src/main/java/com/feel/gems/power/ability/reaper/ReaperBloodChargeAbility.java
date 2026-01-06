package com.feel.gems.power.ability.reaper;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.reaper.ReaperBloodCharge;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;



public final class ReaperBloodChargeAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.REAPER_BLOOD_CHARGE;
    }

    @Override
    public String name() {
        return "Blood Charge";
    }

    @Override
    public String description() {
        return "Press once to begin charging by sacrificing health; press again to store a damage boost for your next hit.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().reaper().bloodChargeCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int maxCharge = GemsBalance.v().reaper().bloodChargeMaxChargeTicks();
        if (maxCharge <= 0) {
            player.sendMessage(Text.translatable("gems.ability.reaper.blood_charge.disabled"), true);
            return false;
        }
        if (!ReaperBloodCharge.isCharging(player)) {
            AbilityRuntime.startReaperBloodChargeCharging(player, maxCharge);
            player.sendMessage(Text.translatable("gems.ability.reaper.blood_charge.charging"), true);
            return false; // do not consume cooldown on start
        }

        boolean ok = AbilityRuntime.finishReaperBloodChargeCharging(player);
        if (ok) {
            player.sendMessage(Text.translatable("gems.ability.reaper.blood_charge.stored"), true);
        }
        return ok;
    }
}
