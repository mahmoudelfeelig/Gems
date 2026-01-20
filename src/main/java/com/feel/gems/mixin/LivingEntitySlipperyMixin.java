package com.feel.gems.mixin;

import com.feel.gems.power.gem.trickster.TricksterPassiveRuntime;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySlipperyMixin {
    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void gems$slipperyIgnoreSlow(StatusEffectInstance effect, net.minecraft.entity.Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (effect == null) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }
        if (effect.getEffectType() == StatusEffects.POISON
                && GemPowers.isPassiveActive(player, PowerIds.BONUS_POISON_IMMUNITY)) {
            cir.setReturnValue(false);
            return;
        }
        if (effect.getEffectType() == StatusEffects.SLOWNESS && TricksterPassiveRuntime.shouldIgnoreSlow(player)) {
            cir.setReturnValue(false);
        }
    }
}
