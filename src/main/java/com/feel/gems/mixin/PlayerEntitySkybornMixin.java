package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.GemPowers;
import com.feel.gems.power.PowerIds;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.entity.LivingEntity.class)
public abstract class PlayerEntitySkybornMixin {
    private static final String KEY_SKYBORN_NEXT = "airSkybornNext";

    @Inject(method = "damage", at = @At("TAIL"))
    private void gems$airSkyborn(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            return;
        }
        if (!((Object) this instanceof ServerPlayerEntity player)) {
            return;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.AIR_SKYBORN)) {
            return;
        }
        if (player.isOnGround()) {
            return;
        }

        var nbt = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        long now = GemsTime.now(player);
        long next = nbt.getLong(KEY_SKYBORN_NEXT);
        if (next > now) {
            return;
        }

        int duration = GemsBalance.v().air().skybornDurationTicks();
        int cooldown = GemsBalance.v().air().skybornCooldownTicks();
        if (duration <= 0 || cooldown <= 0) {
            return;
        }

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, duration, 0, true, false, false));
        nbt.putLong(KEY_SKYBORN_NEXT, now + cooldown);
    }
}

