package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityBonusEffectDurationMixin {
    @ModifyVariable(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z",
            at = @At("HEAD"), argsOnly = true)
    private StatusEffectInstance gems$adjustDebuffDurations(StatusEffectInstance effect) {
        if (effect == null) {
            return null;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) {
            return effect;
        }
        int duration = effect.getDuration();
        if (duration <= 0) {
            return effect;
        }
        var cfg = GemsBalance.v().bonusPool();

        if (GemPowers.isPassiveActive(player, PowerIds.BONUS_CHAIN_BREAKER)
                && (effect.getEffectType() == StatusEffects.SLOWNESS || effect.getEffectType() == StatusEffects.MINING_FATIGUE)) {
            float reduction = cfg.chainBreakerDurationReductionPercent / 100.0f;
            duration = Math.max(1, Math.round(duration * (1.0f - reduction)));
        }

        if (GemPowers.isPassiveActive(player, PowerIds.BONUS_QUICK_RECOVERY)
                && effect.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL) {
            float reduction = cfg.quickRecoveryDebuffReductionPercent / 100.0f;
            duration = Math.max(1, Math.round(duration * (1.0f - reduction)));
        }

        if (duration == effect.getDuration()) {
            return effect;
        }
        return new StatusEffectInstance(effect.getEffectType(), duration, effect.getAmplifier(),
                effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon());
    }
}
