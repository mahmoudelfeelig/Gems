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

public final class HunterNetShotAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.HUNTER_NET_SHOT;
    }

    @Override
    public String name() {
        return "Net Shot";
    }

    @Override
    public String description() {
        return "Fire a net that slows and grounds enemies (disables flight/elytra).";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().hunter().netShotCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int range = GemsBalance.v().hunter().netShotRangeBlocks();
        int slowTicks = GemsBalance.v().hunter().netShotSlowTicks();

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

        // Apply effects
        target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slowTicks, 2, false, true));

        // Ground the target (cancel any upward velocity)
        target.setVelocity(target.getVelocity().x * 0.5, Math.min(target.getVelocity().y, -0.5), target.getVelocity().z * 0.5);
        target.velocityDirty = true;

        // Visual effect - net particles
        AbilityFeedback.burstAt(world, target.getEntityPos().add(0, 1, 0), ParticleTypes.CLOUD, 30, 0.8D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_FISHING_BOBBER_THROW, 1.0F, 0.6F);
        AbilityFeedback.sound(target, SoundEvents.BLOCK_COBWEB_PLACE, 1.0F, 1.0F);
        return true;
    }
}
