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
 * Bloodlust - Gain attack speed based on nearby enemies.
 */
public final class BonusBloodlustAbility implements GemAbility {
    private static final double RANGE = 10.0;
    private static final int DURATION = 200; // 10 seconds
    private static final int MAX_AMPLIFIER = 4; // Haste V max

    @Override
    public Identifier id() {
        return PowerIds.BONUS_BLOODLUST;
    }

    @Override
    public String name() {
        return "Bloodlust";
    }

    @Override
    public String description() {
        return "Gain attack speed based on nearby enemies (up to Haste V).";
    }

    @Override
    public int cooldownTicks() {
        return 400; // 20 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        Box box = player.getBoundingBox().expand(RANGE);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !(e instanceof ServerPlayerEntity p && p.isCreative()));

        int enemyCount = 0;
        for (LivingEntity entity : entities) {
            if (entity instanceof ServerPlayerEntity otherPlayer) {
                if (VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                    continue;
                }
                if (GemTrust.isTrusted(player, otherPlayer)) {
                    continue;
                }
            }
            enemyCount++;
        }
        if (enemyCount == 0) {
            return false;
        }

        int amplifier = Math.min(enemyCount - 1, MAX_AMPLIFIER);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, DURATION, amplifier, false, true, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, DURATION, Math.min(amplifier / 2, 1), false, true, true));

        world.spawnParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1, player.getZ(), 
                10 + enemyCount * 2, 0.5, 0.5, 0.5, 0.1);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.PLAYERS, 0.8f, 1.5f);
        return true;
    }
}
