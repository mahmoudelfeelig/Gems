package com.feel.gems.power.ability.trickster;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.util.Targeting;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

import java.util.List;

/**
 * Mind Games - confuse enemies for 5s.
 * For mobs: applies confusion (Slowness + Blindness) and makes them attack random targets.
 * For players: reverses movement controls.
 */
public final class TricksterMindGamesAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TRICKSTER_MIND_GAMES;
    }

    @Override
    public String name() {
        return "Mind Games";
    }

    @Override
    public String description() {
        return "Confuse an enemy for 5s - mobs attack random targets, players have reversed controls.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().trickster().mindGamesCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int range = GemsBalance.v().trickster().mindGamesRangeBlocks();
        int durationTicks = AugmentRuntime.applyDurationMultiplier(player, GemId.TRICKSTER, GemsBalance.v().trickster().mindGamesDurationTicks());

        // Raycast to find target (any living entity)
        LivingEntity target = Targeting.raycastLiving(player, range);
        if (target == null) {
            AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        // Handle mob targets - confuse them with status effects and random targeting
        if (target instanceof MobEntity mob) {
            // Apply confusion effects
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, durationTicks, 1, false, true));
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, durationTicks, 0, false, true));

            // Find a random target nearby (could be ally, enemy, or even the caster)
            Box searchBox = mob.getBoundingBox().expand(16);
            List<LivingEntity> nearbyEntities = world.getEntitiesByClass(
                LivingEntity.class,
                searchBox,
                e -> e != mob && e.isAlive()
            );

            if (!nearbyEntities.isEmpty()) {
                // Pick a random entity to attack (including allies)
                LivingEntity randomTarget = nearbyEntities.get(world.getRandom().nextInt(nearbyEntities.size()));
                mob.setTarget(randomTarget);
            }

            // Store confused state for ongoing random targeting
            TricksterMindGamesRuntime.applyMindGamesMob(mob, durationTicks);

            AbilityFeedback.burstAt(world, mob.getEntityPos().add(0, 2, 0), ParticleTypes.WITCH, 30, 0.8D);
            AbilityFeedback.sound(player, SoundEvents.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0F, 1.2F);
            return true;
        }

        // Handle player targets - reverse movement controls
        if (target instanceof ServerPlayerEntity targetPlayer) {
            if (!VoidImmunity.canBeTargeted(player, targetPlayer)) {
                AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
                return false;
            }
            TricksterMindGamesRuntime.applyMindGames(targetPlayer, durationTicks);

            AbilityFeedback.burstAt(world, targetPlayer.getEntityPos().add(0, 2, 0), ParticleTypes.WITCH, 30, 0.8D);
            AbilityFeedback.sound(player, SoundEvents.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0F, 1.2F);
            AbilityFeedback.sound(targetPlayer, SoundEvents.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1.0F, 1.2F);
            return true;
        }

        AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
        return false;
    }
}
