package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

/**
 * Ethereal Step - Become intangible briefly, immune to damage but unable to attack.
 */
public final class BonusEtherealStepAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_ETHEREAL_STEP;
    }

    @Override
    public String name() {
        return "Ethereal Step";
    }

    @Override
    public String description() {
        return "Become intangible briefly, immune to damage but unable to attack.";
    }

    @Override
    public int cooldownTicks() {
        return 800; // 40 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 60, 0));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 4)); // Near immunity
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 4)); // Can't attack
        
        world.spawnParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(), 50, 0.5, 1, 0.5, 0.2);
        return true;
    }
}
