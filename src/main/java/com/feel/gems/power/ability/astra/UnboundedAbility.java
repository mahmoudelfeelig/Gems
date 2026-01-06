package com.feel.gems.power.ability.astra;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;



public final class UnboundedAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.UNBOUNDED;
    }

    @Override
    public String name() {
        return "Unbounded";
    }

    @Override
    public String description() {
        return "Briefly grants Spectator mode, then returns you to normal gameplay.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().astra().unboundedCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        AbilityRuntime.startUnbounded(player, GemsBalance.v().astra().unboundedDurationTicks());
        AbilityFeedback.sound(player, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.6F, 1.4F);
        AbilityFeedback.burst(player, ParticleTypes.PORTAL, 14, 0.25D);
        player.sendMessage(Text.translatable("gems.ability.astra.unbounded.active"), true);
        return true;
    }
}
