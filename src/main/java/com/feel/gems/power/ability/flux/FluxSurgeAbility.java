package com.feel.gems.power.ability.flux;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.net.GemExtraStateSync;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.flux.FluxCharge;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class FluxSurgeAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.FLUX_SURGE;
    }

    @Override
    public String name() {
        return "Flux Surge";
    }

    @Override
    public String description() {
        return "Spend charge to gain a short speed/resistance burst and shove nearby enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().flux().fluxSurgeCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        var cfg = GemsBalance.v().flux();
        int charge = FluxCharge.get(player);
        int cost = cfg.fluxSurgeChargeCost();
        if (charge < cost) {
            player.sendMessage(Text.literal("Not enough Flux charge."), true);
            return false;
        }

        FluxCharge.set(player, charge - cost);
        FluxCharge.clearIfBelow100(player);
        GemExtraStateSync.send(player);

        int duration = cfg.fluxSurgeDurationTicks();
        if (duration > 0) {
            int speedAmp = cfg.fluxSurgeSpeedAmplifier();
            int resistAmp = cfg.fluxSurgeResistanceAmplifier();
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, speedAmp, true, false, false));
            if (resistAmp >= 0) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, resistAmp, true, false, false));
            }
        }

        ServerWorld world = player.getServerWorld();
        int radius = cfg.fluxSurgeRadiusBlocks();
        if (radius > 0) {
            double knockback = cfg.fluxSurgeKnockback();
            Box box = new Box(player.getBlockPos()).expand(radius);
            for (LivingEntity other : world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive() && e != player)) {
                if (other instanceof ServerPlayerEntity otherPlayer && GemTrust.isTrusted(player, otherPlayer)) {
                    continue;
                }
                Vec3d delta = other.getPos().subtract(player.getPos()).normalize();
                other.addVelocity(delta.x * knockback, 0.2D, delta.z * knockback);
                other.velocityModified = true;
            }
        }

        AbilityFeedback.burst(player, net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK, 20, 0.45D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.4F, 1.6F);
        return true;
    }
}
