package com.feel.gems.power.ability.trickster;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

import java.util.List;

/**
 * Puppet Master - briefly control an enemy's movement for 3s.
 * For mobs: makes them attack their own allies.
 * For players: marks them as puppeted (movement control via runtime).
 */
public final class TricksterPuppetMasterAbility implements GemAbility {
    public static final String PUPPET_TARGET_KEY = "trickster_puppet_target";
    public static final String PUPPET_END_KEY = "trickster_puppet_end";

    @Override
    public Identifier id() {
        return PowerIds.TRICKSTER_PUPPET_MASTER;
    }

    @Override
    public String name() {
        return "Puppet Master";
    }

    @Override
    public String description() {
        return "Briefly control an enemy's movement for 3s.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().trickster().puppetMasterCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int range = GemsBalance.v().trickster().puppetMasterRangeBlocks();
        int durationTicks = AugmentRuntime.applyDurationMultiplier(player, GemId.TRICKSTER, GemsBalance.v().trickster().puppetMasterDurationTicks());

        // Raycast to find target (any living entity), fallback to nearest if raycast misses.
        LivingEntity target = Targeting.raycastLiving(player, range);
        if (target == null) {
            Box searchBox = player.getBoundingBox().expand(range);
            double bestDist = Double.MAX_VALUE;
            for (LivingEntity candidate : world.getEntitiesByClass(LivingEntity.class, searchBox, e -> e != player && e.isAlive())) {
                if (candidate instanceof ServerPlayerEntity other && !VoidImmunity.canBeTargeted(player, other)) {
                    continue;
                }
                double dist = player.squaredDistanceTo(candidate);
                if (dist < bestDist) {
                    bestDist = dist;
                    target = candidate;
                }
            }
        }
        if (target == null) {
            AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        // Handle mob targets - make them attack their allies
        if (target instanceof MobEntity mob) {
            Box searchBox = mob.getBoundingBox().expand(16);

            // Prefer targeting other mobs so the effect looks like "turning on allies".
            List<MobEntity> nearbyMobs = world.getEntitiesByClass(
                    MobEntity.class,
                    searchBox,
                    e -> e != mob && e.isAlive()
            );

            if (!nearbyMobs.isEmpty()) {
                MobEntity newTarget = nearbyMobs.get(world.getRandom().nextInt(nearbyMobs.size()));
                mob.setTarget(newTarget);
            } else {
                // Fallback: pick any other living entity so the ability still does something in a solo encounter.
                List<LivingEntity> nearbyLiving = world.getEntitiesByClass(
                        LivingEntity.class,
                        searchBox,
                        e -> e != mob && e != player && e.isAlive()
                );
                mob.setTarget(nearbyLiving.isEmpty() ? null : nearbyLiving.get(world.getRandom().nextInt(nearbyLiving.size())));
            }

            TricksterPuppetRuntime.setPuppetedMob(mob, player.getUuid(), durationTicks);

            AbilityFeedback.burstAt(world, mob.getEntityPos().add(0, 2, 0), ParticleTypes.ENCHANT, 25, 0.8D);
            AbilityFeedback.sound(player, SoundEvents.ENTITY_VEX_CHARGE, 1.0F, 0.6F);
            return true;
        }

        // Handle player targets
        if (target instanceof ServerPlayerEntity targetPlayer) {
            if (!VoidImmunity.canBeTargeted(player, targetPlayer)) {
                AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
                return false;
            }
            long endTime = world.getTime() + durationTicks;
            PlayerStateManager.setPersistent(player, PUPPET_TARGET_KEY, targetPlayer.getUuidAsString());
            PlayerStateManager.setPersistent(player, PUPPET_END_KEY, String.valueOf(endTime));

            // Mark target as being puppeted
            TricksterPuppetRuntime.setPuppeted(targetPlayer, player.getUuid(), durationTicks);

            AbilityFeedback.burstAt(world, targetPlayer.getEntityPos().add(0, 2, 0), ParticleTypes.ENCHANT, 25, 0.8D);
            AbilityFeedback.sound(player, SoundEvents.ENTITY_VEX_CHARGE, 1.0F, 0.6F);
            AbilityFeedback.sound(targetPlayer, SoundEvents.ENTITY_VEX_CHARGE, 1.0F, 0.6F);
            return true;
        }

        AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
        return false;
    }

    public static ServerPlayerEntity getPuppetTarget(ServerPlayerEntity puppeteer) {
        String targetStr = PlayerStateManager.getPersistent(puppeteer, PUPPET_TARGET_KEY);
        if (targetStr == null || targetStr.isEmpty()) return null;

        String endStr = PlayerStateManager.getPersistent(puppeteer, PUPPET_END_KEY);
        if (endStr == null) return null;

        long endTime = Long.parseLong(endStr);
        if (puppeteer.getEntityWorld().getTime() > endTime) {
            clearPuppet(puppeteer);
            return null;
        }

        try {
            java.util.UUID targetId = java.util.UUID.fromString(targetStr);
            return puppeteer.getEntityWorld().getServer().getPlayerManager().getPlayer(targetId);
        } catch (Exception e) {
            return null;
        }
    }

    public static void clearPuppet(ServerPlayerEntity puppeteer) {
        PlayerStateManager.clearPersistent(puppeteer, PUPPET_TARGET_KEY);
        PlayerStateManager.clearPersistent(puppeteer, PUPPET_END_KEY);
    }
}
