package com.feel.gems.power.ability.sentinel;

import com.feel.gems.trust.GemTrust;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Identifier;
import java.util.*;

public final class SentinelShieldWallRuntime {
    private static final Map<UUID, WallData> ACTIVE_WALLS = new HashMap<>();
    private static final double REPEL_STRENGTH = 0.5;
    private static final int SLOW_TICKS = 30;
    private static final int SLOW_AMPLIFIER = 1;

    private SentinelShieldWallRuntime() {}

    public static void createWall(ServerPlayerEntity owner, ServerWorld world, BlockPos basePos, Direction perpendicular, int width, int height, long endTime) {
        WallData existing = ACTIVE_WALLS.remove(owner.getUuid());
        if (existing != null) {
            ServerWorld restoreWorld = world;
            if (!existing.worldId.equals(world.getRegistryKey().getValue().toString())) {
                var server = world.getServer();
                Identifier id = Identifier.tryParse(existing.worldId);
                if (server != null && id != null) {
                    restoreWorld = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, id));
                }
            }
            if (restoreWorld != null) {
                restoreBlocks(restoreWorld, existing);
            }
        }

        Set<BlockPos> wallPositions = new HashSet<>();
        Map<BlockPos, BlockState> replaced = new HashMap<>();
        for (int w = -width / 2; w <= width / 2; w++) {
            for (int h = 0; h < height; h++) {
                BlockPos pos = basePos.offset(perpendicular, w).up(h);
                wallPositions.add(pos);
                BlockState atPos = world.getBlockState(pos);
                if (atPos.isOf(Blocks.BARRIER)) {
                    replaced.put(pos, Blocks.AIR.getDefaultState());
                    world.setBlockState(pos, Blocks.BARRIER.getDefaultState());
                } else if (atPos.isAir() || atPos.isReplaceable()) {
                    replaced.put(pos, atPos);
                    world.setBlockState(pos, Blocks.BARRIER.getDefaultState());
                }
            }
        }

        ACTIVE_WALLS.put(owner.getUuid(), new WallData(owner.getUuid(), wallPositions, replaced, endTime, owner.getEntityWorld().getRegistryKey().getValue().toString()));
    }

    public static boolean isInWall(Vec3d position, String worldId) {
        BlockPos blockPos = BlockPos.ofFloored(position);
        for (WallData wall : ACTIVE_WALLS.values()) {
            if (!wall.worldId.equals(worldId)) continue;
            if (wall.positions.contains(blockPos)) {
                return true;
            }
        }
        return false;
    }

    public static UUID getWallOwner(Vec3d position, String worldId) {
        BlockPos blockPos = BlockPos.ofFloored(position);
        for (WallData wall : ACTIVE_WALLS.values()) {
            if (!wall.worldId.equals(worldId)) continue;
            if (wall.positions.contains(blockPos)) {
                return wall.ownerId;
            }
        }
        return null;
    }

    /**
     * Tick the shield walls - removes expired walls and repels hostile entities.
     */
    public static void tick(long currentTime, String worldId, ServerWorld world) {
        if (ACTIVE_WALLS.isEmpty()) {
            return;
        }
        // Remove expired walls
        ACTIVE_WALLS.entrySet().removeIf(entry -> {
            WallData wall = entry.getValue();
            if (!wall.worldId.equals(worldId)) {
                return false;
            }
            if (currentTime <= wall.endTime) {
                return false;
            }
            restoreBlocks(world, wall);
            return true;
        });
        
        // Repel entities from active walls
        for (WallData wall : ACTIVE_WALLS.values()) {
            if (!wall.worldId.equals(worldId)) continue;
            
            ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(wall.ownerId);
            if (owner == null) continue;
            
            // Calculate wall bounding box
            Vec3d wallCenter = calculateWallCenter(wall.positions);
            Box wallBox = calculateWallBox(wall.positions).expand(0.5);
            
            // Find entities in or near the wall
            for (Entity entity : world.getOtherEntities(null, wallBox)) {
                if (entity instanceof ProjectileEntity projectile) {
                    Entity shooter = projectile.getOwner();
                    if (shooter instanceof ServerPlayerEntity shooterPlayer) {
                        if (shooterPlayer.getUuid().equals(wall.ownerId) || GemTrust.isTrusted(owner, shooterPlayer)) {
                            continue;
                        }
                    }
                    world.spawnParticles(ParticleTypes.END_ROD,
                            entity.getX(), entity.getY(), entity.getZ(),
                            6, 0.2, 0.2, 0.2, 0.05);
                    projectile.discard();
                    continue;
                }
                if (!(entity instanceof LivingEntity living)) continue;
                
                // Don't repel the wall owner or their trusted allies
                if (entity instanceof ServerPlayerEntity targetPlayer) {
                    if (targetPlayer.getUuid().equals(wall.ownerId)) continue;
                    if (VoidImmunity.shouldBlockEffect(owner, targetPlayer)) continue;
                    if (GemTrust.isTrusted(owner, targetPlayer)) continue;
                }
                
                // Only repel hostile mobs and enemy players
                if (!(entity instanceof HostileEntity) && !(entity instanceof ServerPlayerEntity)) {
                    continue;
                }
                
                // Calculate repulsion direction (away from wall center)
                Vec3d entityPos = entity.getEntityPos();
                Vec3d repelDir = entityPos.subtract(wallCenter).normalize();
                if (repelDir.lengthSquared() < 0.01) {
                    // Entity is at wall center, push in random direction
                    repelDir = new Vec3d(world.random.nextDouble() - 0.5, 0, world.random.nextDouble() - 0.5).normalize();
                }
                
                // Apply repulsion velocity
                Vec3d velocity = entity.getVelocity();
                entity.setVelocity(velocity.add(repelDir.multiply(REPEL_STRENGTH)));
                living.velocityDirty = true;
                
                // Spawn particles at collision point
                world.spawnParticles(ParticleTypes.END_ROD, 
                        entityPos.x, entityPos.y + 1, entityPos.z, 
                        3, 0.2, 0.2, 0.2, 0.02);

                if (wall.positions.contains(BlockPos.ofFloored(entityPos))) {
                    living.addStatusEffect(new StatusEffectInstance(
                            StatusEffects.SLOWNESS,
                            SLOW_TICKS,
                            SLOW_AMPLIFIER,
                            true, false, false
                    ));
                }
            }
        }
    }
    
    /**
     * Backwards-compatible tick method for worlds that don't pass ServerWorld.
     */
    public static void tick(long currentTime, String worldId) {
        if (ACTIVE_WALLS.isEmpty()) {
            return;
        }
        ACTIVE_WALLS.entrySet().removeIf(entry -> {
            WallData wall = entry.getValue();
            return wall.worldId.equals(worldId) && currentTime > wall.endTime;
        });
    }

    public static boolean hasAnyWalls() {
        return !ACTIVE_WALLS.isEmpty();
    }
    
    private static Vec3d calculateWallCenter(Set<BlockPos> positions) {
        if (positions.isEmpty()) return Vec3d.ZERO;
        double x = 0, y = 0, z = 0;
        for (BlockPos pos : positions) {
            x += pos.getX() + 0.5;
            y += pos.getY() + 0.5;
            z += pos.getZ() + 0.5;
        }
        int count = positions.size();
        return new Vec3d(x / count, y / count, z / count);
    }
    
    private static Box calculateWallBox(Set<BlockPos> positions) {
        if (positions.isEmpty()) return new Box(0, 0, 0, 0, 0, 0);
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return new Box(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    public static void clearWall(UUID ownerId, ServerWorld world) {
        WallData wall = ACTIVE_WALLS.remove(ownerId);
        if (wall != null && world != null) {
            restoreBlocks(world, wall);
        }
    }
    
    public static Collection<WallData> getActiveWalls() {
        return Collections.unmodifiableCollection(ACTIVE_WALLS.values());
    }

    private static void restoreBlocks(ServerWorld world, WallData wall) {
        if (wall.replaced.isEmpty()) {
            return;
        }
        for (Map.Entry<BlockPos, BlockState> entry : wall.replaced.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState current = world.getBlockState(pos);
            if (current.isOf(Blocks.BARRIER)) {
                world.setBlockState(pos, entry.getValue());
            }
        }
    }

    public record WallData(UUID ownerId, Set<BlockPos> positions, Map<BlockPos, BlockState> replaced, long endTime, String worldId) {}
}
