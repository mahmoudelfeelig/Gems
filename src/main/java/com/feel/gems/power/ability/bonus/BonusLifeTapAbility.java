package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public final class BonusLifeTapAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_LIFE_TAP;
    }

    @Override
    public String name() {
        return "Life Tap";
    }

    @Override
    public String description() {
        return "Sacrifice health to gain powerful buffs.";
    }

    @Override
    public int cooldownTicks() {
        return 600; // 30 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        
        // Sacrifice 6 hearts
        if (player.getHealth() <= 12.0f) {
            return false; // Not enough health
        }
        
        player.damage(world, world.getDamageSources().magic(), 12.0f);
        
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 400, 2));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 400, 2));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 400, 1));
        
        world.spawnParticles(ParticleTypes.DAMAGE_INDICATOR, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 1, 0.5, 0.1);
        return true;
    }
}
