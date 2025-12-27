package com.feel.gems.power.ability.spy;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.gem.spy.SpyMimicSystem;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;


public final class SpySmokeBombAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPY_SMOKE_BOMB;
    }

    @Override
    public String name() {
        return "Smoke Bomb";
    }

    @Override
    public String description() {
        return "Blinds and slows nearby untrusted players, and briefly cloaks you.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().spyMimic().smokeBombCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        int radius = GemsBalance.v().spyMimic().smokeBombRadiusBlocks();
        int duration = GemsBalance.v().spyMimic().smokeBombDurationTicks();
        if (radius <= 0 || duration <= 0) {
            return false;
        }
        int blindAmp = GemsBalance.v().spyMimic().smokeBombBlindnessAmplifier();
        int slowAmp = GemsBalance.v().spyMimic().smokeBombSlownessAmplifier();

        Box box = new Box(player.getBlockPos()).expand(radius);
        for (ServerPlayerEntity other : world.getEntitiesByClass(ServerPlayerEntity.class, box, p -> p != player)) {
            if (!SpyMimicSystem.canAffect(player, other)) {
                continue;
            }
            other.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, duration, blindAmp, true, false, false));
            other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, slowAmp, true, false, false));
        }

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, Math.min(duration, 40), 0, true, false, false));
        AbilityFeedback.burst(player, ParticleTypes.CLOUD, 28, 0.45D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_PUFFER_FISH_BLOW_UP, 0.6F, 0.7F);
        return true;
    }
}

