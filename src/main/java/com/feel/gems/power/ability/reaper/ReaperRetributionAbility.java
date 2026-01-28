package com.feel.gems.power.ability.reaper;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.sound.SoundEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ReaperRetributionAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.REAPER_RETRIBUTION;
    }

    @Override
    public String name() {
        return "Retribution";
    }

    @Override
    public String description() {
        return "For a short time, damage you take is redirected to the attacker.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().reaper().retributionCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.REAPER, GemsBalance.v().reaper().retributionDurationTicks());
        if (duration <= 0) {
            player.sendMessage(Text.translatable("gems.ability.reaper.retribution.disabled"), true);
            return false;
        }
        AbilityRuntime.startReaperRetribution(player, duration);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_WITHER_SPAWN, 0.6F, 1.2F);
        player.sendMessage(Text.translatable("gems.ability.reaper.retribution.active"), true);
        return true;
    }
}
