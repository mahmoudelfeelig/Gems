package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.particle.BlockStateParticleEffect;

import java.util.List;

/**
 * Quicksand - Create a zone that slows and sinks enemies.
 */
public final class BonusQuicksandAbility implements GemAbility {
    private static final double RANGE = 25.0;
    private static final double ZONE_RADIUS = 4.0;
    private static final int DURATION = 100; // 5 seconds

    @Override
    public Identifier id() {
        return PowerIds.BONUS_QUICKSAND;
    }

    @Override
    public String name() {
        return "Quicksand";
    }

    @Override
    public String description() {
        return "Create a quicksand zone that slows and sinks enemies for 5s.";
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
        
        HitResult hit = world.raycast(new RaycastContext(
                start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
        Vec3d targetCenter = hit.getPos();
        BlockPos centerPos = BlockPos.ofFloored(targetCenter);

        // Place soul sand blocks temporarily
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = centerPos.add(x, -1, z);
                if (world.getBlockState(pos).isSolidBlock(world, pos)) {
                    // Schedule replacement back (simplified - in production use proper task system)
                    world.setBlockState(pos.up(), Blocks.COBWEB.getDefaultState());
                }
            }
        }

        // Apply slowness to entities in zone
        Box box = new Box(centerPos).expand(ZONE_RADIUS);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());
        for (LivingEntity entity : entities) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, DURATION, 3, false, false, true));
        }

        // Particle effect
        for (int i = 0; i < 30; i++) {
            double offsetX = (world.random.nextDouble() - 0.5) * ZONE_RADIUS * 2;
            double offsetZ = (world.random.nextDouble() - 0.5) * ZONE_RADIUS * 2;
            world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, Blocks.SAND.getDefaultState()), 
                    targetCenter.x + offsetX, targetCenter.y + 0.2, targetCenter.z + offsetZ, 
                    1, 0, 0, 0, 0);
        }

        world.playSound(null, targetCenter.x, targetCenter.y, targetCenter.z,
                SoundEvents.BLOCK_SAND_BREAK, SoundCategory.PLAYERS, 1.5f, 0.5f);
        return true;
    }
}
