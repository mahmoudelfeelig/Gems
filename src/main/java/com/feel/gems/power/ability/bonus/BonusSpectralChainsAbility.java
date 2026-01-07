package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.List;

/**
 * Spectral Chains - Root enemies in place with ghostly chains.
 */
public final class BonusSpectralChainsAbility implements GemAbility {
    private static final double RANGE = 8.0;
    private static final int ROOT_DURATION = 60; // 3 seconds

    @Override
    public Identifier id() {
        return PowerIds.BONUS_SPECTRAL_CHAINS;
    }

    @Override
    public String name() {
        return "Spectral Chains";
    }

    @Override
    public String description() {
        return "Root all enemies within 8 blocks for 3 seconds.";
    }

    @Override
    public int cooldownTicks() {
        return 300; // 15 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        Box box = player.getBoundingBox().expand(RANGE);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !(e instanceof ServerPlayerEntity p && p.isCreative()));

        if (entities.isEmpty()) {
            return false;
        }

        for (LivingEntity entity : entities) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, ROOT_DURATION, 127, false, false, true));
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, ROOT_DURATION, 128, false, false, false));
            
            // Visual effect
            world.spawnParticles(ParticleTypes.SOUL, entity.getX(), entity.getY() + 1, entity.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PARTICLE_SOUL_ESCAPE, SoundCategory.PLAYERS, 1.0f, 0.5f);
        return true;
    }
}
