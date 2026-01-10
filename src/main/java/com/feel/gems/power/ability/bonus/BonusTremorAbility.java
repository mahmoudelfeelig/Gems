package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
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
 * Tremor - Cause the ground to shake, slowing enemies in area.
 */
public final class BonusTremorAbility implements GemAbility {
    private static final double RANGE = 10.0;
    private static final int SLOW_DURATION = 80; // 4 seconds

    @Override
    public Identifier id() {
        return PowerIds.BONUS_TREMOR;
    }

    @Override
    public String name() {
        return "Tremor";
    }

    @Override
    public String description() {
        return "Shake the ground, slowing all enemies within 10 blocks.";
    }

    @Override
    public int cooldownTicks() {
        return 200; // 10 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        Box box = player.getBoundingBox().expand(RANGE);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !(e instanceof ServerPlayerEntity p && p.isCreative()));

        for (LivingEntity entity : entities) {
            if (entity instanceof ServerPlayerEntity otherPlayer) {
                if (VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                    continue;
                }
                if (GemTrust.isTrusted(player, otherPlayer)) {
                    continue;
                }
            }
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, SLOW_DURATION, 1, false, false, true));
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, SLOW_DURATION, 1, false, false, true));
            
            // Small knockback/stagger
            entity.setVelocity(entity.getVelocity().add(0, 0.2, 0));
            entity.velocityDirty = true;
        }

        // Ground crack particles
        for (int i = 0; i < 40; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = world.random.nextDouble() * RANGE;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, player.getY() + 0.1, z, 
                    1, 0, 0.1, 0, 0.01);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_RAVAGER_STEP, SoundCategory.PLAYERS, 1.5f, 0.5f);
        return true;
    }
}
