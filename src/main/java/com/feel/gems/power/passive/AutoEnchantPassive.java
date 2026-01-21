package com.feel.gems.power.passive;

import com.feel.gems.power.api.GemMaintainedPassive;
import java.util.function.Predicate;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;




public final class AutoEnchantPassive implements GemMaintainedPassive {
    private final Identifier id;
    private final String name;
    private final String description;
    private final RegistryKey<Enchantment> enchantmentKey;
    private final int level;
    private final Predicate<ItemStack> predicate;

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
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        ensureEnchanted(world, player.getMainHandStack(), enchantment);
        ensureEnchanted(world, player.getOffHandStack(), enchantment);
        ensureEnchanted(world, player.getEquippedStack(EquipmentSlot.HEAD), enchantment);
        ensureEnchanted(world, player.getEquippedStack(EquipmentSlot.CHEST), enchantment);
        ensureEnchanted(world, player.getEquippedStack(EquipmentSlot.LEGS), enchantment);
        ensureEnchanted(world, player.getEquippedStack(EquipmentSlot.FEET), enchantment);
        ensureEnchanted(world, player.getEquippedStack(EquipmentSlot.BODY), enchantment);
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        // Intentionally does not remove enchantments to avoid deleting player-owned enchants.
    }

    private void ensureEnchanted(ServerWorld world, ItemStack stack, RegistryEntry<Enchantment> enchantment) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        normalizeEnchantments(world, stack);
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

    private void normalizeEnchantments(ServerWorld world, ItemStack stack) {
        var enchants = EnchantmentHelper.getEnchantments(stack);
        if (enchants.isEmpty()) {
            return;
        }
        var registry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        java.util.HashMap<RegistryEntry<Enchantment>, Integer> normalized = new java.util.HashMap<>();
        boolean changed = false;
        for (var entry : enchants.getEnchantmentEntries()) {
            Enchantment value = entry.getKey().value();
            var id = registry.getId(value);
            if (id == null) {
                changed = true;
                continue;
            }
            var regEntry = registry.getEntry(id);
            if (regEntry.isEmpty()) {
                changed = true;
                continue;
            }
            RegistryEntry<Enchantment> canonical = regEntry.get();
            int level = entry.getIntValue();
            normalized.merge(canonical, level, Math::max);
            if (canonical != entry.getKey()) {
                changed = true;
            }
        }
        if (!changed) {
            return;
        }
        EnchantmentHelper.set(stack, net.minecraft.component.type.ItemEnchantmentsComponent.DEFAULT);
        for (var entry : normalized.entrySet()) {
            EnchantmentHelper.apply(stack, builder -> builder.set(entry.getKey(), entry.getValue()));
        }
    }

    private RegistryEntry<Enchantment> resolve(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof net.minecraft.server.world.ServerWorld world)) {
            return null;
        }
        var registry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        var entry = registry.getEntry(enchantmentKey.getValue());
        if (entry.isEmpty()) {
            return null;
        }
        return entry.get();
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

    public static AutoEnchantPassive quickCharge(Identifier id, String name, String description, int level) {
        return new AutoEnchantPassive(id, name, description, Enchantments.QUICK_CHARGE, level, AutoEnchantPredicates::isCrossbow);
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
