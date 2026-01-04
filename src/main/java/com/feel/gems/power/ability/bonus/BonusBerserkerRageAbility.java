package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public final class BonusBerserkerRageAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_BERSERKER_RAGE;
    }

    @Override
    public String name() {
        return "Berserker Rage";
    }

    @Override
    public String description() {
        return "Enter a rage, gaining strength and resistance but losing some control.";
    }

    @Override
    public int cooldownTicks() {
        return 1200; // 60 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 2));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 1));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 200, 0)); // Downside
        
        world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 1, 0.5, 0.1);
        return true;
    }
}
