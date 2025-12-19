package com.blissmc.gems.mixin;

import com.blissmc.gems.core.GemId;
import com.blissmc.gems.power.StaticBurstAbility;
import com.blissmc.gems.state.GemPlayerState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFluxDamageStoreMixin {
    @Inject(method = "damage", at = @At("TAIL"))
    private void gems$storeFluxDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!(cir.getReturnValue())) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }
        GemPlayerState.initIfNeeded(player);
        if (GemPlayerState.getEnergy(player) <= 0) {
            return;
        }
        if (GemPlayerState.getActiveGem(player) != GemId.FLUX) {
            return;
        }
        StaticBurstAbility.onDamaged(player, amount);
    }
}

