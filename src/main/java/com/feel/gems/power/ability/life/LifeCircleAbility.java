package com.feel.gems.power.ability.life;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;



public final class LifeCircleAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.LIFE_CIRCLE;
    }

    @Override
    public String name() {
        return "Life Circle";
    }

    @Override
    public String description() {
        return "Aura that boosts trusted players' max health and reduces enemies' max health.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().life().lifeCircleCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.LIFE, GemsBalance.v().life().lifeCircleDurationTicks());
        AbilityRuntime.startLifeCircle(player, duration);
        AbilityFeedback.sound(player, SoundEvents.BLOCK_BEACON_ACTIVATE, 0.8F, 1.0F);
        AbilityFeedback.burst(player, ParticleTypes.HEART, 16, 0.35D);
        player.sendMessage(Text.translatable("gems.ability.life.life_circle.active"), true);
        return true;
    }
}
