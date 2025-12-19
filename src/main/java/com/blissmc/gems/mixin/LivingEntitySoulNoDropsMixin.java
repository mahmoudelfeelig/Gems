package com.blissmc.gems.mixin;

import com.blissmc.gems.power.SoulSummons;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySoulNoDropsMixin {
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void gems$soulNoDrops(ServerWorld world, DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (SoulSummons.isSoul(self)) {
            ci.cancel();
        }
    }
}

