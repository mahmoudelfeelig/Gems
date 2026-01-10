package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Purge - Remove all buffs from target enemy.
 */
public final class BonusPurgeAbility implements GemAbility {
    private static final double RANGE = 15.0;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_PURGE;
    }

    @Override
    public String name() {
        return "Purge";
    }

    @Override
    public String description() {
        return "Remove all positive status effects from a target enemy.";
    }

    @Override
    public int cooldownTicks() {
        return 300; // 15 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVector();

        LivingEntity target = null;
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, 
                player.getBoundingBox().expand(RANGE), e -> e != player && e.isAlive())) {
            Vec3d toEntity = entity.getEyePos().subtract(start);
            double dot = toEntity.normalize().dotProduct(direction);
            if (dot > 0.9 && toEntity.length() < RANGE) {
                target = entity;
                break;
            }
        }

        if (target == null) {
            return false;
        }

        // Remove all beneficial effects
        List<StatusEffectInstance> toRemove = new ArrayList<>();
        for (StatusEffectInstance effect : target.getStatusEffects()) {
            if (effect.getEffectType().value().isBeneficial()) {
                toRemove.add(effect);
            }
        }

        if (toRemove.isEmpty()) {
            return false;
        }

        for (StatusEffectInstance effect : toRemove) {
            target.removeStatusEffect(effect.getEffectType());
        }

        // Visual effect
        world.spawnParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1, target.getZ(), 
                20, 0.3, 0.5, 0.3, 0.1);

        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.0f, 1.5f);
        return true;
    }
}
