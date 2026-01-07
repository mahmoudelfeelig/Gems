package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

/**
 * Spectral Blade - Summon a ghostly sword (vex) that attacks nearby enemies.
 */
public final class BonusSpectralBladeAbility implements GemAbility {
    private static final int BLADE_LIFETIME = 200; // 10 seconds
    private static final double TARGET_RANGE = 16.0;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_SPECTRAL_BLADE;
    }

    @Override
    public String name() {
        return "Spectral Blade";
    }

    @Override
    public String description() {
        return "Summon a spectral blade that attacks nearby enemies for 10s.";
    }

    @Override
    public int cooldownTicks() {
        return 400; // 20 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        
        // Spawn a vex as the spectral blade
        VexEntity blade = EntityType.VEX.create(world, SpawnReason.MOB_SUMMONED);
        if (blade == null) {
            return false;
        }

        blade.setPosition(player.getX(), player.getY() + 1, player.getZ());
        blade.setOwner(null); // No evoker owner
        blade.setLifeTicks(BLADE_LIFETIME);
        blade.addCommandTag("gems_spectral_blade");
        blade.addCommandTag("owner:" + player.getUuidAsString());
        
        // Find a valid target - hostile mob or enemy player
        LivingEntity target = findTarget(player, world);
        if (target != null) {
            blade.setTarget(target);
        }
        
        world.spawnEntity(blade);

        world.spawnParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1, player.getZ(), 
                20, 0.5, 0.5, 0.5, 0.1);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_VEX_CHARGE, SoundCategory.PLAYERS, 1.0f, 1.5f);
        return true;
    }
    
    /**
     * Find a valid target for the spectral blade - prioritize players, then hostile mobs.
     */
    private static LivingEntity findTarget(ServerPlayerEntity owner, ServerWorld world) {
        Box searchBox = owner.getBoundingBox().expand(TARGET_RANGE);
        
        // First, look for enemy players
        for (Entity e : world.getOtherEntities(owner, searchBox)) {
            if (e instanceof ServerPlayerEntity target && target.isAlive()) {
                if (!GemTrust.isTrusted(owner, target) && VoidImmunity.canBeTargeted(owner, target)) {
                    return target;
                }
            }
        }
        
        // Then look for hostile mobs
        for (Entity e : world.getOtherEntities(owner, searchBox)) {
            if (e instanceof HostileEntity hostile && hostile.isAlive()) {
                return hostile;
            }
        }
        
        return null;
    }
    
    /**
     * Check if an entity is a spectral blade owned by a specific player.
     */
    public static boolean isOwnedBy(VexEntity vex, ServerPlayerEntity player) {
        return vex.getCommandTags().contains("gems_spectral_blade") 
                && vex.getCommandTags().contains("owner:" + player.getUuidAsString());
    }
}
