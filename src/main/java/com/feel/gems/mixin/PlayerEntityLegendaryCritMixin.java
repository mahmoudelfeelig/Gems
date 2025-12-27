package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.item.ModItems;
import com.feel.gems.util.GemsTime;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(PlayerEntity.class)
public abstract class PlayerEntityLegendaryCritMixin {
    private static final String KEY_THIRD_STRIKE_COUNT = "legendaryThirdStrikeCount";
    private static final String KEY_THIRD_STRIKE_LAST = "legendaryThirdStrikeLast";

    @Inject(method = "attack", at = @At("TAIL"))
    private void gems$legendaryCrits(Entity target, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity attacker)) {
            return;
        }
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }

        boolean isCrit = isCritical(attacker);

        if (isCrit && attacker.getMainHandStack().isOf(ModItems.THIRD_STRIKE_BLADE)) {
            long now = GemsTime.now(attacker);
            var data = ((com.feel.gems.state.GemsPersistentDataHolder) attacker).gems$getPersistentData();
            long last = data.getLong(KEY_THIRD_STRIKE_LAST);
            int window = GemsBalance.v().legendary().thirdStrikeWindowTicks();
            int count = data.getInt(KEY_THIRD_STRIKE_COUNT);
            if (window > 0 && now - last > window) {
                count = 0;
            }
            count += 1;
            data.putInt(KEY_THIRD_STRIKE_COUNT, count);
            data.putLong(KEY_THIRD_STRIKE_LAST, now);
            if (count % 3 == 0) {
                float bonus = GemsBalance.v().legendary().thirdStrikeBonusDamage();
                if (bonus > 0.0F) {
                    living.damage(attacker.getDamageSources().playerAttack(attacker), bonus);
                }
            }
        }

        if (isCrit && attacker.getMainHandStack().isOf(ModItems.VAMPIRIC_EDGE)) {
            float heal = GemsBalance.v().legendary().vampiricHealAmount();
            if (heal > 0.0F) {
                attacker.heal(heal);
            }
        }
    }

    private static boolean isCritical(ServerPlayerEntity attacker) {
        if (attacker.isOnGround()) {
            return false;
        }
        if (attacker.isClimbing() || attacker.isTouchingWater() || attacker.hasVehicle()) {
            return false;
        }
        if (attacker.hasStatusEffect(StatusEffects.BLINDNESS)) {
            return false;
        }
        return attacker.getAttackCooldownProgress(0.5F) > 0.9F;
    }
}
