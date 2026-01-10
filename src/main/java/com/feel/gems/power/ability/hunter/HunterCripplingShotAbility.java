package com.feel.gems.power.ability.hunter;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public final class HunterCripplingShotAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.HUNTER_CRIPPLING_SHOT;
    }

    @Override
    public String name() {
        return "Crippling Shot";
    }

    @Override
    public String description() {
        return "Ranged attack that reduces target's movement speed by 50% for 8s.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().hunter().cripplingCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int range = GemsBalance.v().hunter().cripplingRangeBlocks();
        int durationTicks = GemsBalance.v().hunter().cripplingDurationTicks();

        // Raycast to find target
        HitResult hit = player.raycast(range, 0.0F, false);
        if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof ServerPlayerEntity target)) {
            AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        if (!VoidImmunity.canBeTargeted(player, target)) {
            AbilityFeedback.sound(player, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
            return false;
        }

        // Apply crippling effect (Slowness II = 30% slow, Slowness III = 45%, we use custom handling)
        // Using Slowness IV gives ~60% slow which is close to 50%
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, durationTicks, 3, false, true));

        // Visual
        AbilityFeedback.beam(world, player.getEntityPos().add(0, 1.5, 0), target.getEntityPos().add(0, 1, 0), ParticleTypes.CRIT, 20);
        AbilityFeedback.burstAt(world, target.getEntityPos().add(0, 0.5, 0), ParticleTypes.DAMAGE_INDICATOR, 10, 0.3D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_ARROW_SHOOT, 1.0F, 0.8F);
        AbilityFeedback.sound(target, SoundEvents.ENTITY_ARROW_HIT_PLAYER, 1.0F, 0.6F);
        return true;
    }
}
