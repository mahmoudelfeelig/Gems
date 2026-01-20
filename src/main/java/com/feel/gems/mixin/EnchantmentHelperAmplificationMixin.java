package com.feel.gems.mixin;

import com.feel.gems.power.gem.wealth.EnchantmentAmplification;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(EnchantmentHelper.class)
public final class EnchantmentHelperAmplificationMixin {
    @Inject(
            method = "getLevel(Lnet/minecraft/registry/entry/RegistryEntry;Lnet/minecraft/item/ItemStack;)I",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void gems$amplifyGetLevel(
            RegistryEntry<Enchantment> enchantment,
            ItemStack stack,
            CallbackInfoReturnable<Integer> cir
    ) {
        int bonus = EnchantmentAmplification.getBonusLevel(stack, enchantment);
        if (bonus > 0) {
            cir.setReturnValue(cir.getReturnValueI() + bonus);
        }
    }

    @ModifyArgs(
            method = "forEachEnchantment(Lnet/minecraft/item/ItemStack;Lnet/minecraft/enchantment/EnchantmentHelper$Consumer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/enchantment/EnchantmentHelper$Consumer;accept(Lnet/minecraft/registry/entry/RegistryEntry;I)V"
            )
    )
    private static void gems$amplifyForEach(
            Args args,
            ItemStack stack,
            EnchantmentHelper.Consumer consumer
    ) {
        RegistryEntry<Enchantment> enchantment = args.get(0);
        int level = args.get(1);
        int bonus = EnchantmentAmplification.getBonusLevel(stack, enchantment);
        if (bonus > 0) {
            args.set(1, level + bonus);
        }
    }

    @ModifyArgs(
            method = "forEachEnchantment(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/EquipmentSlot;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/enchantment/EnchantmentHelper$ContextAwareConsumer;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/enchantment/EnchantmentHelper$ContextAwareConsumer;accept(Lnet/minecraft/registry/entry/RegistryEntry;ILnet/minecraft/enchantment/EnchantmentEffectContext;)V"
            )
    )
    private static void gems$amplifyForEachContext(
            Args args,
            ItemStack stack,
            EquipmentSlot slot,
            LivingEntity entity,
            EnchantmentHelper.ContextAwareConsumer consumer
    ) {
        RegistryEntry<Enchantment> enchantment = args.get(0);
        int level = args.get(1);
        int bonus = EnchantmentAmplification.getBonusLevel(stack, enchantment);
        if (bonus > 0) {
            args.set(1, level + bonus);
        }
    }
}
