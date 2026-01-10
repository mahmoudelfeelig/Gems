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
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Soul Link - Link with target, sharing damage taken.
 * Target takes 50% of damage dealt to you for 10 seconds.
 */
public final class BonusSoulLinkAbility implements GemAbility {
    private static final double RANGE = 20.0;
    private static final int DURATION = 200; // 10 seconds

    @Override
    public Identifier id() {
        return PowerIds.BONUS_SOUL_LINK;
    }

    @Override
    public String name() {
        return "Soul Link";
    }

    @Override
    public String description() {
        return "Link with a target. They take 50% of damage you receive for 10s.";
    }

    @Override
    public int cooldownTicks() {
        return 400; // 20 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        Vec3d start = player.getEyePos();
        Vec3d direction = player.getRotationVector();
        Vec3d end = start.add(direction.multiply(RANGE));

        LivingEntity target = null;
        for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, 
                player.getBoundingBox().expand(RANGE), e -> e != player && e.isAlive())) {
            Vec3d toEntity = entity.getEntityPos().add(0, entity.getHeight() / 2, 0).subtract(start);
            double dot = toEntity.normalize().dotProduct(direction);
            if (dot > 0.9 && toEntity.length() < RANGE) {
                target = entity;
                break;
            }
        }

        if (target == null) {
            return false;
        }

        // Mark both with glowing to show the link
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, DURATION, 0, false, false, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, DURATION, 0, false, false, true));

        // Store link data (implementation via event handler checks for this effect combo)
        // The actual damage sharing is handled in BonusPassiveHandler

        world.spawnParticles(ParticleTypes.ENCHANT, 
                (player.getX() + target.getX()) / 2, 
                (player.getY() + target.getY()) / 2 + 1, 
                (player.getZ() + target.getZ()) / 2, 
                30, 1, 1, 1, 0.1);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.0f, 1.2f);
        return true;
    }
}
