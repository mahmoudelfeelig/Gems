package com.feel.gems.mixin;

import com.feel.gems.power.gem.voidgem.VoidImmunity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityVoidImmunityStatusEffectMixin {

    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void gems$blockPlayerAppliedEffectsOnVoid(StatusEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (!(source instanceof ServerPlayerEntity sourcePlayer)) {
            return;
        }
        if (!((Object) this instanceof ServerPlayerEntity targetPlayer)) {
            return;
        }
        if (sourcePlayer == targetPlayer) {
            return;
        }
        // Avoid interfering with vanilla/other-mod direct potion effects; Gems mostly uses ambient effects for auras.
        if (!effect.isAmbient()) {
            return;
        }
        if (!VoidImmunity.hasImmunity(targetPlayer)) {
            return;
        }
        cir.setReturnValue(false);
    }
}
