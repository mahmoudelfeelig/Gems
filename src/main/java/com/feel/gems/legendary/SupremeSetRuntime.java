package com.feel.gems.legendary;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.item.ModItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;




public final class SupremeSetRuntime {
    private SupremeSetRuntime() {
    }

    public static void tick(ServerPlayerEntity player) {
        boolean helmet = player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.SUPREME_HELMET);
        boolean chest = player.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.SUPREME_CHESTPLATE);
        boolean legs = player.getEquippedStack(EquipmentSlot.LEGS).isOf(ModItems.SUPREME_LEGGINGS);
        boolean boots = player.getEquippedStack(EquipmentSlot.FEET).isOf(ModItems.SUPREME_BOOTS);

        var cfg = GemsBalance.v().legendary();
        maintainEffect(player, StatusEffects.NIGHT_VISION, cfg.supremeHelmetNightVisionAmplifier(), helmet);
        maintainEffect(player, StatusEffects.WATER_BREATHING, cfg.supremeHelmetWaterBreathingAmplifier(), helmet);
        maintainEffect(player, StatusEffects.STRENGTH, cfg.supremeChestStrengthAmplifier(), chest);
        maintainEffect(player, StatusEffects.FIRE_RESISTANCE, cfg.supremeLeggingsFireResAmplifier(), legs);
        maintainEffect(player, StatusEffects.SPEED, cfg.supremeBootsSpeedAmplifier(), boots);

        boolean full = helmet && chest && legs && boots;
        maintainEffect(player, StatusEffects.RESISTANCE, cfg.supremeSetResistanceAmplifier(), full);
    }

    private static void maintainEffect(ServerPlayerEntity player, RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect, int amplifier, boolean shouldHave) {
        if (amplifier < 0) {
            return;
        }
        if (shouldHave) {
            applyIfMissingOrWeaker(player, effect, amplifier);
        } else {
            removeIfApplied(player, effect, amplifier);
        }
    }

    private static void applyIfMissingOrWeaker(ServerPlayerEntity player, RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect, int amplifier) {
        StatusEffectInstance current = player.getStatusEffect(effect);
        if (current != null && current.getAmplifier() > amplifier) {
            return;
        }
        player.addStatusEffect(new StatusEffectInstance(effect, StatusEffectInstance.INFINITE, amplifier, true, false, false));
    }

    private static void removeIfApplied(ServerPlayerEntity player, RegistryEntry<net.minecraft.entity.effect.StatusEffect> effect, int amplifier) {
        StatusEffectInstance current = player.getStatusEffect(effect);
        if (current == null) {
            return;
        }
        if (current.getAmplifier() > amplifier) {
            return;
        }
        if (current.getDuration() != StatusEffectInstance.INFINITE) {
            return;
        }
        player.removeStatusEffect(effect);
    }
}
