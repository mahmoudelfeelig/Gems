package com.feel.gems.power.gem.wealth;

import com.feel.gems.util.GemsTime;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;


public final class EnchantmentAmplification {
    private static final String CUSTOM_DATA_KEY_AMPLIFY = "gemsAmplify";

    private EnchantmentAmplification() {
    }

    public static void apply(ServerPlayerEntity player, int durationTicks) {
        long until = GemsTime.now(player) + durationTicks;

        amplifyStack(player.getServerWorld(), player.getMainHandStack(), until);
        amplifyStack(player.getServerWorld(), player.getOffHandStack(), until);
        for (ItemStack armor : player.getInventory().armor) {
            amplifyStack(player.getServerWorld(), armor, until);
        }
    }

    private static void amplifyStack(ServerWorld world, ItemStack stack, long until) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (!stack.hasEnchantments()) {
            return;
        }

        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom != null && custom.getNbt().contains(CUSTOM_DATA_KEY_AMPLIFY, NbtElement.COMPOUND_TYPE)) {
            return;
        }

        ItemEnchantmentsComponent enchants = stack.getEnchantments();
        if (enchants.isEmpty()) {
            return;
        }

        NbtList originalList = new NbtList();
        for (var entry : enchants.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> enchantment = entry.getKey();
            int level = entry.getIntValue();

            NbtCompound e = new NbtCompound();
            e.putString("id", world.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getId(enchantment.value()).toString());
            e.putInt("lvl", level);
            originalList.add(e);
        }

        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> {
            NbtCompound marker = new NbtCompound();
            marker.putLong("until", until);
            marker.put("enchants", originalList);
            nbt.put(CUSTOM_DATA_KEY_AMPLIFY, marker);
        });

        for (var entry : enchants.getEnchantmentEntries()) {
            RegistryEntry<Enchantment> enchantment = entry.getKey();
            int current = entry.getIntValue();
            int boosted = Math.min(enchantment.value().getMaxLevel(), current + 1);
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
        var registry = world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound e = list.getCompound(i);
            String rawId = e.getString("id");
            int lvl = e.getInt("lvl");
            var id = net.minecraft.util.Identifier.tryParse(rawId);
            if (id == null) {
                continue;
            }
            var entry = registry.getEntry(net.minecraft.registry.RegistryKey.of(RegistryKeys.ENCHANTMENT, id));
            if (entry.isEmpty()) {
                continue;
            }
            int clamped = Math.max(1, Math.min(entry.get().value().getMaxLevel(), lvl));
            EnchantmentHelper.apply(stack, builder -> builder.set(entry.get(), clamped));
        }
    }
}
