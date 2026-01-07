package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Icicle Barrage - Fire a volley of piercing icicles.
 */
public final class BonusIcicleBarrageAbility implements GemAbility {
    private static final int ICICLE_COUNT = 5;
    private static final double RANGE = 25.0;
    private static final float DAMAGE_PER_ICICLE = 4.0f;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_ICICLE_BARRAGE;
    }

    @Override
    public String name() {
        return "Icicle Barrage";
    }

    @Override
    public String description() {
        return "Fire 5 piercing icicles that each deal 4 damage.";
    }

    @Override
    public int cooldownTicks() {
        return 250; // 12.5 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        Vec3d start = player.getEyePos();
        Vec3d baseDirection = player.getRotationVector();

        for (int i = 0; i < ICICLE_COUNT; i++) {
            // Spread icicles in a fan pattern
            double spreadAngle = (i - 2) * 0.1; // -0.2 to 0.2 radians
            Vec3d direction = baseDirection.rotateY((float) spreadAngle);
            
            // Trace icicle path and damage entities
            Vec3d current = start;
            for (int step = 0; step < RANGE; step++) {
                current = current.add(direction);
                
                // Check for entity hits along the path
                Box hitBox = new Box(current.subtract(0.5, 0.5, 0.5), current.add(0.5, 0.5, 0.5));
                List<LivingEntity> hits = world.getEntitiesByClass(LivingEntity.class, hitBox,
                        e -> e != player && e.isAlive());
                
                for (LivingEntity hit : hits) {
                    hit.damage(world, world.getDamageSources().freeze(), DAMAGE_PER_ICICLE);
                    hit.setFrozenTicks(hit.getFrozenTicks() + 100);
                }
                
                // Icicle particle trail
                if (step % 2 == 0) {
                    world.spawnParticles(ParticleTypes.SNOWFLAKE, current.x, current.y, current.z, 
                            1, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0f, 1.5f);
        return true;
    }
}
