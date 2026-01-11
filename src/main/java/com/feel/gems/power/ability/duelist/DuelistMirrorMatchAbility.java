package com.feel.gems.power.ability.duelist;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.block.Blocks;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public final class DuelistMirrorMatchAbility implements GemAbility {
    public static final String DUEL_PARTNER_KEY = "duelist_mirror_match_partner";
    public static final String DUEL_END_TIME_KEY = "duelist_mirror_match_end";
    public static final String CAGE_CENTER_X_KEY = "duelist_mirror_match_cage_x";
    public static final String CAGE_CENTER_Y_KEY = "duelist_mirror_match_cage_y";
    public static final String CAGE_CENTER_Z_KEY = "duelist_mirror_match_cage_z";
    
    // Cage dimensions: 20x20x10 (±10 on X/Z, +10 on Y from floor)
    public static final int CAGE_HALF_SIZE = 10;
    public static final int CAGE_HEIGHT = 10;

    @Override
    public Identifier id() {
        return PowerIds.DUELIST_MIRROR_MATCH;
    }

    @Override
    public String name() {
        return "Mirror Match";
    }

    @Override
    public String description() {
        return "Swap skins and names with a targeted player, trapping both in a barrier cage.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().duelist().mirrorMatchCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int range = GemsBalance.v().duelist().mirrorMatchRangeBlocks();

        // Raycast to find target
        ServerPlayerEntity target = Targeting.raycastPlayer(player, range);
        if (target == null) {
            AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        if (!VoidImmunity.canBeTargeted(player, target)) {
            AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        int durationTicks = GemsBalance.v().duelist().mirrorMatchDurationTicks();
        long endTime = world.getTime() + durationTicks;

        // Calculate cage center (midpoint between both players)
        BlockPos cageCenter = new BlockPos(
            (int) ((player.getX() + target.getX()) / 2),
            (int) Math.min(player.getY(), target.getY()),
            (int) ((player.getZ() + target.getZ()) / 2)
        );

        // Build barrier cage
        buildCage(world, cageCenter);

        // Store mirror state for both players
        PlayerStateManager.setPersistent(player, DUEL_PARTNER_KEY, target.getUuidAsString());
        PlayerStateManager.setPersistent(player, DUEL_END_TIME_KEY, String.valueOf(endTime));
        PlayerStateManager.setPersistent(player, CAGE_CENTER_X_KEY, String.valueOf(cageCenter.getX()));
        PlayerStateManager.setPersistent(player, CAGE_CENTER_Y_KEY, String.valueOf(cageCenter.getY()));
        PlayerStateManager.setPersistent(player, CAGE_CENTER_Z_KEY, String.valueOf(cageCenter.getZ()));

        // Target state
        PlayerStateManager.setPersistent(target, DUEL_PARTNER_KEY, player.getUuidAsString());
        PlayerStateManager.setPersistent(target, DUEL_END_TIME_KEY, String.valueOf(endTime));
        PlayerStateManager.setPersistent(target, CAGE_CENTER_X_KEY, String.valueOf(cageCenter.getX()));
        PlayerStateManager.setPersistent(target, CAGE_CENTER_Y_KEY, String.valueOf(cageCenter.getY()));
        PlayerStateManager.setPersistent(target, CAGE_CENTER_Z_KEY, String.valueOf(cageCenter.getZ()));

        DuelistMirrorMatchRuntime.start(player, target);

        // Visual effects
        AbilityFeedback.burstAt(world, player.getEntityPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.ENCHANT, 30, 1.0D);
        AbilityFeedback.burstAt(world, target.getEntityPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.ENCHANT, 30, 1.0D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 1.0F, 1.0F);
        AbilityFeedback.sound(target, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 1.0F, 1.0F);

        return true;
    }

    /**
     * Build a barrier cage around the center position (20x20x10).
     */
    public static void buildCage(ServerWorld world, BlockPos center) {
        int minX = center.getX() - CAGE_HALF_SIZE;
        int maxX = center.getX() + CAGE_HALF_SIZE;
        int minZ = center.getZ() - CAGE_HALF_SIZE;
        int maxZ = center.getZ() + CAGE_HALF_SIZE;
        int floorY = center.getY();
        int ceilingY = floorY + CAGE_HEIGHT;

        // Build floor and ceiling
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                world.setBlockState(new BlockPos(x, floorY, z), Blocks.BARRIER.getDefaultState());
                world.setBlockState(new BlockPos(x, ceilingY, z), Blocks.BARRIER.getDefaultState());
            }
        }

        // Build walls
        for (int y = floorY + 1; y < ceilingY; y++) {
            for (int x = minX; x <= maxX; x++) {
                world.setBlockState(new BlockPos(x, y, minZ), Blocks.BARRIER.getDefaultState());
                world.setBlockState(new BlockPos(x, y, maxZ), Blocks.BARRIER.getDefaultState());
            }
            for (int z = minZ + 1; z < maxZ; z++) {
                world.setBlockState(new BlockPos(minX, y, z), Blocks.BARRIER.getDefaultState());
                world.setBlockState(new BlockPos(maxX, y, z), Blocks.BARRIER.getDefaultState());
            }
        }
    }

    /**
     * Remove the barrier cage (restore air blocks).
     */
    public static void removeCage(ServerWorld world, BlockPos center) {
        int minX = center.getX() - CAGE_HALF_SIZE;
        int maxX = center.getX() + CAGE_HALF_SIZE;
        int minZ = center.getZ() - CAGE_HALF_SIZE;
        int maxZ = center.getZ() + CAGE_HALF_SIZE;
        int floorY = center.getY();
        int ceilingY = floorY + CAGE_HEIGHT;

        // Remove floor and ceiling
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (world.getBlockState(new BlockPos(x, floorY, z)).isOf(Blocks.BARRIER)) {
                    world.setBlockState(new BlockPos(x, floorY, z), Blocks.AIR.getDefaultState());
                }
                if (world.getBlockState(new BlockPos(x, ceilingY, z)).isOf(Blocks.BARRIER)) {
                    world.setBlockState(new BlockPos(x, ceilingY, z), Blocks.AIR.getDefaultState());
                }
            }
        }

        // Remove walls
        for (int y = floorY + 1; y < ceilingY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (world.getBlockState(new BlockPos(x, y, minZ)).isOf(Blocks.BARRIER)) {
                    world.setBlockState(new BlockPos(x, y, minZ), Blocks.AIR.getDefaultState());
                }
                if (world.getBlockState(new BlockPos(x, y, maxZ)).isOf(Blocks.BARRIER)) {
                    world.setBlockState(new BlockPos(x, y, maxZ), Blocks.AIR.getDefaultState());
                }
            }
            for (int z = minZ + 1; z < maxZ; z++) {
                if (world.getBlockState(new BlockPos(minX, y, z)).isOf(Blocks.BARRIER)) {
                    world.setBlockState(new BlockPos(minX, y, z), Blocks.AIR.getDefaultState());
                }
                if (world.getBlockState(new BlockPos(maxX, y, z)).isOf(Blocks.BARRIER)) {
                    world.setBlockState(new BlockPos(maxX, y, z), Blocks.AIR.getDefaultState());
                }
            }
        }
    }

    /**
     * Get the cage center from a player's state, if they are in a duel.
     */
    public static BlockPos getCageCenter(ServerPlayerEntity player) {
        String xStr = PlayerStateManager.getPersistent(player, CAGE_CENTER_X_KEY);
        String yStr = PlayerStateManager.getPersistent(player, CAGE_CENTER_Y_KEY);
        String zStr = PlayerStateManager.getPersistent(player, CAGE_CENTER_Z_KEY);
        if (xStr == null || yStr == null || zStr == null) {
            return null;
        }
        try {
            return new BlockPos(Integer.parseInt(xStr), Integer.parseInt(yStr), Integer.parseInt(zStr));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean isInDuel(ServerPlayerEntity player) {
        String partner = PlayerStateManager.getPersistent(player, DUEL_PARTNER_KEY);
        if (partner == null || partner.isEmpty()) {
            return false;
        }
        String endTimeStr = PlayerStateManager.getPersistent(player, DUEL_END_TIME_KEY);
        if (endTimeStr == null) {
            return false;
        }
        long endTime = Long.parseLong(endTimeStr);
        return player.getEntityWorld().getTime() < endTime;
    }

    public static void clearDuel(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, DUEL_PARTNER_KEY);
        PlayerStateManager.clearPersistent(player, DUEL_END_TIME_KEY);
        PlayerStateManager.clearPersistent(player, CAGE_CENTER_X_KEY);
        PlayerStateManager.clearPersistent(player, CAGE_CENTER_Y_KEY);
        PlayerStateManager.clearPersistent(player, CAGE_CENTER_Z_KEY);
    }
}
