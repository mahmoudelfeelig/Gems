package com.feel.gems.power;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.function.Predicate;

public final class AutoEnchantPassive implements GemMaintainedPassive {
    private final Identifier id;
    private final String name;
    private final String description;
    private final RegistryKey<Enchantment> enchantmentKey;
    private final int level;
    private final Predicate<ItemStack> predicate;

    private RegistryEntry<Enchantment> cachedEnchantment;

    public AutoEnchantPassive(
            Identifier id,
            String name,
            String description,
            RegistryKey<Enchantment> enchantmentKey,
            int level,
            Predicate<ItemStack> predicate
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.enchantmentKey = enchantmentKey;
        this.level = level;
        this.predicate = predicate;
    }

    @Override
    public Identifier id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public void apply(ServerPlayerEntity player) {
        maintain(player);
    }

    @Override
    public void maintain(ServerPlayerEntity player) {
        RegistryEntry<Enchantment> enchantment = resolve(player);
        if (enchantment == null) {
            return;
        }
        ensureEnchanted(player.getMainHandStack(), enchantment);
        ensureEnchanted(player.getOffHandStack(), enchantment);
        for (ItemStack armor : player.getInventory().armor) {
            ensureEnchanted(armor, enchantment);
        }
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        // Intentionally does not remove enchantments to avoid deleting player-owned enchants.
    }

    private void ensureEnchanted(ItemStack stack, RegistryEntry<Enchantment> enchantment) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (!predicate.test(stack)) {
            return;
        }
        Enchantment enchantmentValue = enchantment.value();
        if (!enchantmentValue.isAcceptableItem(stack)) {
            return;
        }
        if (!EnchantmentHelper.canHaveEnchantments(stack)) {
            return;
        }
        int current = EnchantmentHelper.getLevel(enchantment, stack);
        if (current >= level) {
            return;
        }
        if (current <= 0) {
            var existing = EnchantmentHelper.getEnchantments(stack);
            for (RegistryEntry<Enchantment> other : existing.getEnchantments()) {
                if (other == enchantment) {
                    continue;
                }
                if (!Enchantment.canBeCombined(other, enchantment)) {
                    return;
                }
            }
        }

        EnchantmentHelper.apply(stack, builder -> builder.set(enchantment, level));
    }

    private RegistryEntry<Enchantment> resolve(ServerPlayerEntity player) {
        if (cachedEnchantment != null) {
            return cachedEnchantment;
        }
        var registry = player.getServerWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        var entry = registry.getEntry(enchantmentKey);
        if (entry.isEmpty()) {
            return null;
        }
        cachedEnchantment = entry.get();
        return cachedEnchantment;
    }

    public static AutoEnchantPassive fireAspect(Identifier id, String name, String description, int level) {
        return new AutoEnchantPassive(id, name, description, Enchantments.FIRE_ASPECT, level, AutoEnchantPredicates::isMeleeWeapon);
    }

    public static AutoEnchantPassive sharpness(Identifier id, String name, String description, int level) {
        return new AutoEnchantPassive(id, name, description, Enchantments.SHARPNESS, level, AutoEnchantPredicates::isMeleeWeapon);
    }

    public static AutoEnchantPassive unbreaking(Identifier id, String name, String description, int level) {
        return new AutoEnchantPassive(id, name, description, Enchantments.UNBREAKING, level, AutoEnchantPredicates::isGearOrTool);
    }

    public static AutoEnchantPassive power(Identifier id, String name, String description, int level) {
        return new AutoEnchantPassive(id, name, description, Enchantments.POWER, level, AutoEnchantPredicates::isBow);
    }

    public static AutoEnchantPassive punch(Identifier id, String name, String description, int level) {
        return new AutoEnchantPassive(id, name, description, Enchantments.PUNCH, level, AutoEnchantPredicates::isBow);
    }

    public static AutoEnchantPassive mending(Identifier id, String name, String description) {
        return new AutoEnchantPassive(id, name, description, Enchantments.MENDING, 1, AutoEnchantPredicates::isGearOrTool);
    }

    public static AutoEnchantPassive fortune(Identifier id, String name, String description, int level) {
        return new AutoEnchantPassive(id, name, description, Enchantments.FORTUNE, level, AutoEnchantPredicates::isTool);
    }

    public static AutoEnchantPassive looting(Identifier id, String name, String description, int level) {
        return new AutoEnchantPassive(id, name, description, Enchantments.LOOTING, level, AutoEnchantPredicates::isMeleeWeapon);
    }
}
