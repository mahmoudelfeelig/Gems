package com.feel.gems.mixin;

import com.feel.gems.power.GemPowers;
import com.feel.gems.power.PowerIds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityWealthDurabilityMixin {
    @Inject(method = "attack", at = @At("TAIL"))
    private void gems$durabilityPassives(Entity target, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity attacker)) {
            return;
        }

        boolean chip = GemPowers.isPassiveActive(attacker, PowerIds.DURABILITY_CHIP);
        boolean mend = GemPowers.isPassiveActive(attacker, PowerIds.ARMOR_MEND_ON_HIT);
        if (!chip && !mend) {
            return;
        }

        if (chip && target instanceof ServerPlayerEntity victim) {
            damageArmor(victim, 1);
        }
        if (mend) {
            mendArmor(attacker, 1);
        }
    }

    private static void damageArmor(ServerPlayerEntity player, int amount) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
                continue;
            }
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.isEmpty() || !stack.isDamageable()) {
                continue;
            }
            stack.damage(amount, player, slot);
        }
    }

    private static void mendArmor(ServerPlayerEntity player, int amount) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) {
                continue;
            }
            ItemStack stack = player.getEquippedStack(slot);
            if (stack.isEmpty() || !stack.isDamageable() || !stack.isDamaged()) {
                continue;
            }
            stack.setDamage(Math.max(0, stack.getDamage() - amount));
        }
    }
}
