package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityVoidImmunityStatusEffectMixin {

    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;)Z", at = @At("HEAD"), cancellable = true)
    private void gems$blockAmbientEffectsOnVoid(StatusEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayerEntity targetPlayer)) {
            return;
        }
        if (!VoidImmunity.hasImmunity(targetPlayer)) {
            return;
        }
        if (!GemsBalance.v().voidGem().blockAllStatusEffects()) {
            return;
        }
        // Void should be immune to all gem-driven effects, and many abilities/passives apply effects without a source.
        // We conservatively block all status effects while Void immunity is active.
        cir.setReturnValue(false);
    }

    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void gems$blockPlayerAppliedEffectsOnVoid(StatusEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayerEntity targetPlayer)) {
            return;
        }
        if (!VoidImmunity.hasImmunity(targetPlayer)) {
            return;
        }
        if (!GemsBalance.v().voidGem().blockAllStatusEffects()) {
            return;
        }

        ServerPlayerEntity sourcePlayer = null;
        if (source instanceof ServerPlayerEntity sp) {
            sourcePlayer = sp;
        } else if (source instanceof ProjectileEntity projectile) {
            if (projectile.getOwner() instanceof ServerPlayerEntity ownerPlayer) {
                sourcePlayer = ownerPlayer;
            }
        } else if (source instanceof AreaEffectCloudEntity cloud) {
            if (cloud.getOwner() instanceof ServerPlayerEntity ownerPlayer) {
                sourcePlayer = ownerPlayer;
            }
        }

        // If we can attribute the effect to a player (directly or via owned entities),
        // block it unless it is self-applied.
        if (sourcePlayer != null && sourcePlayer == targetPlayer) {
            return;
        }
        cir.setReturnValue(false);
    }
}
