package com.feel.gems.mixin;

import com.feel.gems.power.gem.space.SpaceLunarScaling;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;




@Mixin(LivingEntity.class)
public abstract class LivingEntityHealScalingMixin {
    @ModifyVariable(method = "heal", at = @At("HEAD"), argsOnly = true)
    private float gems$scaleHeal(float amount) {
        if (amount <= 0.0F) {
            return amount;
        }

        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getEntityWorld().isClient()) {
            return amount;
        }

        if (!(self instanceof ServerPlayerEntity player)) {
            return amount;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.SPACE_LUNAR_SCALING)) {
            return amount;
        }
        return amount * SpaceLunarScaling.multiplier(player.getEntityWorld());
    }
}
