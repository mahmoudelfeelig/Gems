package com.feel.gems.mixin;

import com.feel.gems.power.gem.air.AirGaleSlamRuntime;
import com.feel.gems.power.gem.air.AirMacePassive;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.config.GemsBalance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(PlayerEntity.class)
public abstract class PlayerEntityAirGaleSlamMixin {
    @Inject(method = "attack", at = @At("TAIL"))
    private void gems$airGaleSlam(Entity target, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }
        if (target instanceof LivingEntity living
                && GemPowers.isPassiveActive(player, PowerIds.AIR_WIND_SHEAR)
                && AirMacePassive.isHoldingMace(player)) {
            double knockback = GemsBalance.v().air().windShearKnockback();
            if (knockback > 0.0D) {
                Vec3d away = living.getEntityPos().subtract(player.getEntityPos()).normalize();
                living.addVelocity(away.x * knockback, 0.1D, away.z * knockback);
                living.velocityDirty = true;
            }
            int slowDuration = GemsBalance.v().air().windShearSlownessDurationTicks();
            if (slowDuration > 0) {
                int slowAmp = GemsBalance.v().air().windShearSlownessAmplifier();
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, slowDuration, slowAmp, true, false, false));
            }
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.AIR_WINDBURST_MACE)) {
            return;
        }
        if (!AirMacePassive.isHoldingMace(player)) {
            return;
        }
        if (!AirGaleSlamRuntime.consumeIfActive(player)) {
            return;
        }
        AirGaleSlamRuntime.trigger(player, target);
    }
}

