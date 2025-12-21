package com.feel.gems.mixin;

import com.feel.gems.power.SoulSummons;
import com.feel.gems.power.SummonerSummons;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntitySoulNoExperienceMixin {
    @Inject(method = "dropXp", at = @At("HEAD"), cancellable = true, require = 0)
    private void gems$soulNoXp(Entity attacker, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (SoulSummons.isSoul(self) || SummonerSummons.isSummon(self)) {
            ci.cancel();
        }
    }
}
