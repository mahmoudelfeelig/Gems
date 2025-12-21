package com.feel.gems.mixin;

import com.feel.gems.power.GemPowers;
import com.feel.gems.power.PowerIds;
import com.feel.gems.power.AirMacePassive;
import com.feel.gems.config.GemsBalance;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFallDamageMixin {
    @Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
    private void gems$handleFallDamage(float fallDistance, float damageMultiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.getWorld().isClient) {
            return;
        }
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }
        if (GemPowers.isPassiveActive(player, PowerIds.FALL_DAMAGE_IMMUNITY)
                || GemPowers.isPassiveActive(player, PowerIds.SPACE_LOW_GRAVITY)) {
            cir.setReturnValue(false);
        }
    }

    @ModifyVariable(method = "handleFallDamage", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private float gems$airAerialGuardFallDamage(float damageMultiplier) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.getWorld().isClient) {
            return damageMultiplier;
        }
        if (!(entity instanceof ServerPlayerEntity player)) {
            return damageMultiplier;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.AIR_AERIAL_GUARD)) {
            return damageMultiplier;
        }
        if (!AirMacePassive.isHoldingMace(player)) {
            return damageMultiplier;
        }
        float mult = GemsBalance.v().air().aerialGuardFallDamageMultiplier();
        return damageMultiplier * mult;
    }
}
