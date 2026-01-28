package com.feel.gems.mixin;

import com.feel.gems.power.bonus.BonusPassiveRuntime;
import com.feel.gems.power.gem.wealth.WealthFumble;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFumbleTotemMixin {
    @Inject(method = "tryUseDeathProtector", at = @At("HEAD"), cancellable = true)
    private void gems$disableOffhandTotem(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }
        ItemStack main = player.getMainHandStack();
        ItemStack offhand = player.getOffHandStack();
        boolean hasTotem = main.isOf(Items.TOTEM_OF_UNDYING) || offhand.isOf(Items.TOTEM_OF_UNDYING);
        if (hasTotem && BonusPassiveRuntime.shouldTriggerSecondWind(player)) {
            BonusPassiveRuntime.consumeSecondWind(player);
            player.setHealth(Math.max(1.0f, player.getMaxHealth() * 0.5f));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 4, false, true));
            cir.setReturnValue(true);
            return;
        }
        if (!WealthFumble.isActive(player)) {
            return;
        }
        if (!offhand.isOf(Items.TOTEM_OF_UNDYING)) {
            return;
        }
        if (main.isOf(Items.TOTEM_OF_UNDYING)) {
            return;
        }
        cir.setReturnValue(false);
    }
}
