package com.feel.gems.power.ability.duelist;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public final class DuelistRapidStrikeAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.DUELIST_RAPID_STRIKE;
    }

    @Override
    public String name() {
        return "Rapid Strike";
    }

    @Override
    public String description() {
        return "Remove sword cooldowns for 5 seconds.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().duelist().rapidStrikeCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int duration = GemsBalance.v().duelist().rapidStrikeDurationTicks();
        if (duration <= 0) {
            return false;
        }
        AbilityRuntime.startDuelistRapidStrike(player, duration);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.9F, 1.2F);
        AbilityFeedback.burst(player, ParticleTypes.CRIT, 18, 0.35D);
        return true;
    }
}
