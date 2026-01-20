package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.item.ModItems;
import com.feel.gems.item.legendary.ThirdStrikeBladeItem;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.util.GemsTime;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(PlayerEntity.class)
public abstract class PlayerEntityLegendaryCritMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void gems$legendaryCriticals(Entity target, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity attacker)) {
            return;
        }
        if (!(target instanceof LivingEntity living)) {
            return;
        }
        
        if (!gems$isVanillaCritical(attacker)) {
            return;
        }

        if (attacker.getMainHandStack().isOf(ModItems.THIRD_STRIKE_BLADE)) {
            ThirdStrikeBladeItem.recordCriticalHit(attacker);
        }

        if (attacker.getMainHandStack().isOf(ModItems.VAMPIRIC_EDGE)) {
            float heal = GemsBalance.v().legendary().vampiricHealAmount();
            if (heal > 0.0F) {
                attacker.heal(heal);
            }
        }
    }

    @Inject(method = "attack", at = @At("TAIL"))
    private void gems$legendaryCrits(Entity target, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity attacker)) {
            return;
        }
        if (!(target instanceof LivingEntity living)) {
            return;
        }

        long now = GemsTime.now(attacker);
        if (AbilityRuntime.isReaperWitheringStrikesActive(attacker, now)) {
            int duration = GemsBalance.v().reaper().witheringStrikesWitherDurationTicks();
            int amp = GemsBalance.v().reaper().witheringStrikesWitherAmplifier();
            if (duration > 0) {
                living.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, duration, amp, true, false, false));
            }
        }
    }

    private static boolean gems$isVanillaCritical(ServerPlayerEntity attacker) {
        if (attacker.getAbilities().flying) {
            return false;
        }
        if (attacker.isOnGround()) {
            return false;
        }
        if (attacker.fallDistance <= 0.0F) {
            return false;
        }
        if (attacker.isClimbing()) {
            return false;
        }
        if (attacker.isTouchingWater()) {
            return false;
        }
        if (attacker.hasVehicle()) {
            return false;
        }
        if (attacker.hasStatusEffect(StatusEffects.BLINDNESS)) {
            return false;
        }
        if (attacker.isSprinting()) {
            return false;
        }
        if (attacker.isUsingItem()) {
            return false;
        }
        return true;
    }
}
