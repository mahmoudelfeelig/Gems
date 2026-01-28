package com.feel.gems.power.gem.wealth;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.util.GemsTime;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;


public final class EnchantmentAmplification {
    private static final String CUSTOM_DATA_KEY_AMPLIFY = "gemsAmplify";
    private static final String CUSTOM_DATA_KEY_BONUS = "bonus";

    private EnchantmentAmplification() {
    }

    public static void apply(ServerPlayerEntity player, int durationTicks) {
        long until = GemsTime.now(player) + durationTicks;

        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        amplifyStack(world, player.getMainHandStack(), until);
        amplifyStack(world, player.getOffHandStack(), until);
        amplifyStack(world, player.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD), until);
        amplifyStack(world, player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST), until);
        amplifyStack(world, player.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS), until);
        amplifyStack(world, player.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET), until);
        amplifyStack(world, player.getEquippedStack(net.minecraft.entity.EquipmentSlot.BODY), until);
    }

    private static void amplifyStack(ServerWorld world, ItemStack stack, long until) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (!stack.hasEnchantments()) {
            return;
        }

        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom != null && custom.copyNbt().contains(CUSTOM_DATA_KEY_AMPLIFY)) {
            return;
        }

        ItemEnchantmentsComponent enchants = stack.getEnchantments();
        if (enchants.isEmpty()) {
            return;
        }

        int bonusLevels = Math.max(1, GemsBalance.v().wealth().amplificationBonusLevels());
        NbtList originalList = new NbtList();
        NbtCompound bonus = new NbtCompound();
        for (var entry : enchants.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> enchantment = entry.getKey();
            int level = entry.getIntValue();
            if (level >= enchantment.value().getMaxLevel() && isOverMaxEligible(enchantment)) {
                bonus.putInt(enchantment.getIdAsString(), bonusLevels);
            }

            NbtCompound e = new NbtCompound();
            e.putString("id", world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getId(enchantment.value()).toString());
            e.putInt("lvl", level);
            originalList.add(e);
        }

        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            NbtCompound marker = new NbtCompound();
            marker.putLong("until", until);
            marker.put("enchants", originalList);
            if (!bonus.isEmpty()) {
                marker.put(CUSTOM_DATA_KEY_BONUS, bonus);
            }
            nbt.put(CUSTOM_DATA_KEY_AMPLIFY, marker);
        });

        for (var entry : enchants.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> enchantment = entry.getKey();
            int current = entry.getIntValue();
            int boosted = Math.min(enchantment.value().getMaxLevel(), current + bonusLevels);
            if (boosted == current) {
                continue;
            }
            EnchantmentHelper.apply(stack, builder -> builder.set(enchantment, boosted));
        }
    }

    public static void restoreFromList(ServerWorld world, ItemStack stack, NbtList list) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        EnchantmentHelper.set(stack, ItemEnchantmentsComponent.DEFAULT);
        var registry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound e = list.getCompound(i).orElse(null);
            if (e == null) {
                continue;
            }
            String rawId = e.getString("id", "");
            int lvl = e.getInt("lvl", 0);
            var id = net.minecraft.util.Identifier.tryParse(rawId);
            if (id == null) {
                continue;
            }
            var entry = registry.getEntry(id);
            if (entry.isEmpty()) {
                continue;
            }
            int clamped = Math.max(1, Math.min(entry.get().value().getMaxLevel(), lvl));
            EnchantmentHelper.apply(stack, builder -> builder.set(entry.get(), clamped));
        }
    }

    public static int getBonusLevel(ItemStack stack, RegistryEntry<Enchantment> enchantment) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) {
            return 0;
        }
        NbtCompound nbt = custom.copyNbt();
        NbtCompound marker = nbt.getCompound(CUSTOM_DATA_KEY_AMPLIFY).orElse(null);
        if (marker == null || marker.isEmpty()) {
            return 0;
        }
        NbtCompound bonus = marker.getCompound(CUSTOM_DATA_KEY_BONUS).orElse(null);
        if (bonus == null || bonus.isEmpty()) {
            return 0;
        }
        String key = enchantment.getIdAsString();
        if (key == null || key.isEmpty()) {
            return 0;
        }
        return bonus.getInt(key, 0);
    }

    private static boolean isOverMaxEligible(RegistryEntry<Enchantment> enchantment) {
        return enchantment.matchesKey(Enchantments.UNBREAKING)
                || enchantment.matchesKey(Enchantments.PROTECTION)
                || enchantment.matchesKey(Enchantments.MENDING)
                || enchantment.matchesKey(Enchantments.LOOTING)
                || enchantment.matchesKey(Enchantments.SHARPNESS)
                || enchantment.matchesKey(Enchantments.EFFICIENCY)
                || enchantment.matchesKey(Enchantments.FORTUNE);
    }
}
