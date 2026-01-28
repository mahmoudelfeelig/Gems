package com.feel.gems.power.ability.astra;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRestrictions;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;



public final class DimensionalVoidAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.DIMENSIONAL_VOID;
    }

    @Override
    public String name() {
        return "Dimensional Void";
    }

    @Override
    public String description() {
        return "Suppresses enemy gem abilities in a radius for a short duration.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().astra().dimensionalVoidCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.ASTRA, GemsBalance.v().astra().dimensionalVoidDurationTicks());
        int radius = GemsBalance.v().astra().dimensionalVoidRadiusBlocks();
        int affected = 0;
        Box box = new Box(player.getBlockPos()).expand(radius);
        for (LivingEntity other : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
            if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                continue;
            }
            if (other instanceof ServerPlayerEntity otherPlayer) {
                if (!VoidImmunity.canBeTargeted(player, otherPlayer)) {
                    continue;
                }
                AbilityRestrictions.suppress(otherPlayer, duration);
            } else {
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, 1, true, false, false));
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration, 0, true, false, false));
            }
            AbilityFeedback.burstAt(world, other.getEntityPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.REVERSE_PORTAL, 10, 0.25D);
            affected++;
        }
        AbilityFeedback.sound(player, SoundEvents.BLOCK_BEACON_DEACTIVATE, 0.8F, 0.8F);
        AbilityFeedback.burst(player, ParticleTypes.REVERSE_PORTAL, 18, 0.35D);
        player.sendMessage(Text.translatable("gems.ability.astra.dimensional_void.suppressed", affected), true);
        return true;
    }
}
