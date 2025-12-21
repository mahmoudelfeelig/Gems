package com.feel.gems.mixin;

import com.feel.gems.power.SoulSummons;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonEntity.class)
public abstract class EnderDragonSoulNoExperienceMixin {
    @Shadow
    protected int experiencePoints;

    @Inject(method = "updatePostDeath", at = @At("HEAD"), require = 0)
    private void gems$soulDisableDragonXp(CallbackInfo ci) {
        EnderDragonEntity self = (EnderDragonEntity) (Object) this;
        if (SoulSummons.isSoul(self)) {
            // Best-effort: ensure any XP paths that respect experiencePoints drop 0.
            this.experiencePoints = 0;
        }
    }
}

