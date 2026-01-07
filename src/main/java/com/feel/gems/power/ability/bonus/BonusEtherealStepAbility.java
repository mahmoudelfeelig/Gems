package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.util.GemsTeleport;
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Ethereal Step - Short dash that passes through one wall.
 */
public final class BonusEtherealStepAbility implements GemAbility {
    private static final int COOLDOWN_TICKS = 200; // 10 seconds
    private static final int MAX_DISTANCE = 8;

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
        return "Phase through solid blocks, teleporting up to 8 blocks in your facing direction.";
    }

    @Override
    public int cooldownTicks() {
        return COOLDOWN_TICKS;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        
        int maxDistance = MAX_DISTANCE;
        Vec3d start = player.getEntityPos();
        Vec3d direction = player.getRotationVector().normalize();
        
        // Find destination by phasing through blocks
        Vec3d destination = findPhaseDestination(world, start, direction, maxDistance, player);
        
        if (destination == null || destination.squaredDistanceTo(start) < 1.0) {
            player.sendMessage(net.minecraft.text.Text.translatable("gems.ability.cannot_phase"), true);
            return false;
        }
        
        // Particles at start (ghost trail)
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, start.x, start.y + 1, start.z, 
                25, 0.3, 0.5, 0.3, 0.05);
        world.spawnParticles(ParticleTypes.SOUL, start.x, start.y + 0.5, start.z,
                10, 0.2, 0.3, 0.2, 0.02);

        // Teleport
        GemsTeleport.teleport(player, world, destination.x, destination.y, destination.z, player.getYaw(), player.getPitch());

        // Particles at end
        world.spawnParticles(ParticleTypes.PORTAL, destination.x, destination.y + 1, destination.z, 
                25, 0.3, 0.5, 0.3, 0.05);
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, destination.x, destination.y + 0.5, destination.z,
                8, 0.2, 0.3, 0.2, 0.02);

        world.playSound(null, destination.x, destination.y, destination.z,
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.7f, 1.3f);
        world.playSound(null, start.x, start.y, start.z,
                SoundEvents.PARTICLE_SOUL_ESCAPE.value(), SoundCategory.PLAYERS, 0.5f, 0.8f);
        return true;
    }
    
    /**
     * Find a valid destination by phasing through solid blocks.
     * The ability should pass through walls and emerge on the other side.
     */
    private Vec3d findPhaseDestination(ServerWorld world, Vec3d start, Vec3d direction, int maxDistance, ServerPlayerEntity player) {
        boolean inWall = false;
        boolean passedThroughWall = false;
        Vec3d lastValidPos = null;
        
        for (int i = 1; i <= maxDistance; i++) {
            Vec3d checkPos = start.add(direction.multiply(i));
            BlockPos blockPos = BlockPos.ofFloored(checkPos);
            BlockPos headPos = blockPos.up();
            
            BlockState feetBlock = world.getBlockState(blockPos);
            BlockState headBlock = world.getBlockState(headPos);
            
            boolean feetClear = !feetBlock.blocksMovement();
            boolean headClear = !headBlock.blocksMovement();
            boolean positionClear = feetClear && headClear;
            
            if (!positionClear) {
                // We're in a wall
                inWall = true;
            } else if (inWall) {
                // We just exited a wall - this is our phase destination
                passedThroughWall = true;
                lastValidPos = checkPos;
            } else {
                // Open space, keep tracking as potential destination
                lastValidPos = checkPos;
            }
        }
        
        // If we passed through a wall, use that exit point
        // Otherwise, use the last valid position we found (normal teleport)
        if (passedThroughWall && lastValidPos != null) {
            return lastValidPos;
        }
        
        // Fallback: find the furthest open position
        for (int i = maxDistance; i >= 1; i--) {
            Vec3d checkPos = start.add(direction.multiply(i));
            BlockPos blockPos = BlockPos.ofFloored(checkPos);
            BlockPos headPos = blockPos.up();
            
            BlockState feetBlock = world.getBlockState(blockPos);
            BlockState headBlock = world.getBlockState(headPos);
            
            if (!feetBlock.blocksMovement() && !headBlock.blocksMovement()) {
                return checkPos;
            }
        }
        
        return null;
    }
}
