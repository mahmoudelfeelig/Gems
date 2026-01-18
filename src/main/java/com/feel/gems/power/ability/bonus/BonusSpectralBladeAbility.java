package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.astra.SoulSummons;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsTime;
import com.feel.gems.legendary.HypnoControl;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
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
        SummonerSummons.tuneControlledMob(blade);
        SummonerSummons.mark(blade, player.getUuid(), GemsTime.now(player) + BLADE_LIFETIME);
        
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

        LivingEntity threat = normalizeTarget(owner, owner.getAttacker());
        if (threat != null) {
            return threat;
        }
        LivingEntity nearest = null;
        double nearestSq = Double.MAX_VALUE;
        for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, searchBox, m -> m.getTarget() == owner)) {
            LivingEntity candidate = normalizeTarget(owner, mob);
            if (candidate == null) {
                continue;
            }
            double distSq = owner.squaredDistanceTo(candidate);
            if (distSq < nearestSq) {
                nearestSq = distSq;
                nearest = candidate;
            }
        }
        if (nearest != null) {
            return nearest;
        }

        // Then look for enemy players
        for (Entity e : world.getOtherEntities(owner, searchBox)) {
            if (e instanceof ServerPlayerEntity target) {
                LivingEntity candidate = normalizeTarget(owner, target);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        
        // Then look for hostile mobs
        for (Entity e : world.getOtherEntities(owner, searchBox)) {
            if (e instanceof HostileEntity hostile) {
                LivingEntity candidate = normalizeTarget(owner, hostile);
                if (candidate != null) {
                    return candidate;
                }
            }
        }
        
        return null;
    }

    private static LivingEntity normalizeTarget(ServerPlayerEntity owner, LivingEntity target) {
        if (target == null || !target.isAlive() || target.getEntityWorld() != owner.getEntityWorld()) {
            return null;
        }
        double maxSq = TARGET_RANGE * TARGET_RANGE;
        if (owner.squaredDistanceTo(target) > maxSq) {
            return null;
        }
        if (target instanceof ServerPlayerEntity player) {
            if (player == owner || GemTrust.isTrusted(owner, player) || !VoidImmunity.canBeTargeted(owner, player)) {
                return null;
            }
        }
        if (target instanceof MobEntity mob) {
            UUID summonOwner = SummonerSummons.ownerUuid(mob);
            if (summonOwner != null && summonOwner.equals(owner.getUuid()) && SummonerSummons.isSummon(mob)) {
                return null;
            }
            UUID soulOwner = SoulSummons.ownerUuid(mob);
            if (soulOwner != null && soulOwner.equals(owner.getUuid()) && SoulSummons.isSoul(mob)) {
                return null;
            }
            UUID hypnoOwner = HypnoControl.ownerUuid(mob);
            if (hypnoOwner != null && hypnoOwner.equals(owner.getUuid()) && HypnoControl.isHypno(mob)) {
                return null;
            }
        }
        return target;
    }
    
    /**
     * Check if an entity is a spectral blade owned by a specific player.
     */
    public static boolean isOwnedBy(VexEntity vex, ServerPlayerEntity player) {
        return vex.getCommandTags().contains("gems_spectral_blade") 
                && vex.getCommandTags().contains("owner:" + player.getUuidAsString());
    }
}
