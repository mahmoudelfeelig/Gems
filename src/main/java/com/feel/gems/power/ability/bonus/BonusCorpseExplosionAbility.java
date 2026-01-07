package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Corpse Explosion - Detonate nearby corpses (recently killed entities) for AoE damage.
 * For simplicity, we detect recently damaged entities at low health as "corpses".
 */
public final class BonusCorpseExplosionAbility implements GemAbility {
    private static final double SEARCH_RANGE = 15.0;
    private static final double EXPLOSION_RANGE = 5.0;
    private static final float DAMAGE = 8.0f;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_CORPSE_EXPLOSION;
    }

    @Override
    public String name() {
        return "Corpse Explosion";
    }

    @Override
    public String description() {
        return "Detonate nearby low-health enemies for 8 AoE damage.";
    }

    @Override
    public int cooldownTicks() {
        return 300; // 15 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        Box searchBox = player.getBoundingBox().expand(SEARCH_RANGE);
        
        // Find "corpse candidates" - entities below 25% HP
        List<LivingEntity> corpses = world.getEntitiesByClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive() && e.getHealth() / e.getMaxHealth() < 0.25);

        if (corpses.isEmpty()) {
            return false;
        }

        List<LivingEntity> exploded = new ArrayList<>();
        for (LivingEntity corpse : corpses) {
            // Kill the corpse
            corpse.damage(world, world.getDamageSources().magic(), Float.MAX_VALUE);
            exploded.add(corpse);

            // AoE damage around the corpse
            Box explosionBox = corpse.getBoundingBox().expand(EXPLOSION_RANGE);
            List<LivingEntity> nearby = world.getEntitiesByClass(LivingEntity.class, explosionBox,
                    e -> e != player && e.isAlive() && !corpses.contains(e));
            
            for (LivingEntity entity : nearby) {
                entity.damage(world, world.getDamageSources().explosion(null, player), DAMAGE);
            }

            // Explosion particles
            world.spawnParticles(ParticleTypes.EXPLOSION, corpse.getX(), corpse.getY() + 0.5, corpse.getZ(), 
                    5, 0.5, 0.5, 0.5, 0);
            world.spawnParticles(ParticleTypes.SOUL, corpse.getX(), corpse.getY() + 0.5, corpse.getZ(), 
                    20, 0.5, 0.5, 0.5, 0.05);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 1.2f);
        return true;
    }
}
