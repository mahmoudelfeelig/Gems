package com.blissmc.gems.mixin;

import com.blissmc.gems.power.AbilityRuntime;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityRichRushLootMixin {
    @Unique
    private static final ThreadLocal<Boolean> gems$inDoubleLoot = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Shadow
    protected abstract void dropLoot(DamageSource source, boolean causedByPlayer);

    @Inject(method = "dropLoot", at = @At("TAIL"))
    private void gems$richRushDoubleLoot(DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
        if (!causedByPlayer) {
            return;
        }
        if (Boolean.TRUE.equals(gems$inDoubleLoot.get())) {
            return;
        }
        if (!(source.getAttacker() instanceof ServerPlayerEntity player)) {
            return;
        }
        if (!AbilityRuntime.isRichRushActive(player)) {
            return;
        }
        try {
            gems$inDoubleLoot.set(Boolean.TRUE);
            dropLoot(source, true);
        } finally {
            gems$inDoubleLoot.set(Boolean.FALSE);
        }
    }
}

