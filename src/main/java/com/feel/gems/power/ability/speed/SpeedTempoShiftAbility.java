package com.feel.gems.power.ability.speed;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


public final class SpeedTempoShiftAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPEED_TEMPO_SHIFT;
    }

    @Override
    public String name() {
        return "Tempo Shift";
    }

    @Override
    public String description() {
        return "Creates a tempo field that accelerates ally cooldowns and slows enemies nearby.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().speed().tempoShiftCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int duration = GemsBalance.v().speed().tempoShiftDurationTicks();
        if (duration <= 0) {
            player.sendMessage(Text.translatable("gems.message.ability_disabled_server"), true);
            return false;
        }

        AbilityRuntime.startSpeedTempoShift(player, duration);
        if (player.getEntityWorld() instanceof ServerWorld world) {
            AbilityFeedback.ring(world, player.getEntityPos().add(0.0D, 0.1D, 0.0D), 3.0D, ParticleTypes.END_ROD, 24);
        }
        AbilityFeedback.sound(player, SoundEvents.ITEM_TRIDENT_RETURN, 0.8F, 1.6F);
        return true;
    }
}
