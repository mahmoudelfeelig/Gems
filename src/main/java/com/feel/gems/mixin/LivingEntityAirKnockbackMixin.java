package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.gem.air.AirMacePassive;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;




@Mixin(LivingEntity.class)
public abstract class LivingEntityAirKnockbackMixin {
    @ModifyVariable(method = "takeKnockback", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double gems$airAerialGuardKnockback(double strength) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.getWorld().isClient) {
            return strength;
        }
        if (!(entity instanceof ServerPlayerEntity player)) {
            return strength;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.AIR_AERIAL_GUARD)) {
            return strength;
        }
        if (!AirMacePassive.isHoldingMace(player)) {
            return strength;
        }
        double mult = GemsBalance.v().air().aerialGuardKnockbackMultiplier();
        return strength * mult;
    }
}

