package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemStack.class)
public abstract class ItemStackUnbreakableMixin {
    @ModifyVariable(method = "damage(ILnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/EquipmentSlot;)V",
            at = @At("HEAD"), argsOnly = true)
    private int gems$reduceDurability(int amount, int originalAmount, LivingEntity entity, EquipmentSlot slot) {
        if (amount <= 0 || entity == null) {
            return amount;
        }
        if (!(entity instanceof ServerPlayerEntity player)) {
            return amount;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_UNBREAKABLE)) {
            return amount;
        }
        float reduction = GemsBalance.v().bonusPool().unbreakableDurabilityReductionPercent / 100.0f;
        return Math.max(0, Math.round(amount * (1.0f - reduction)));
    }
}
