package com.feel.gems.power;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import com.feel.gems.state.GemsPersistentDataHolder;

public final class AirMacePassive implements GemMaintainedPassive {
    private static final String TAG_AIR_MACE = "gemsAirMace";
    private static final String KEY_GRANTED = "airMaceGranted";

    private final Identifier id;
    private final String name;
    private final String description;

    public AirMacePassive(Identifier id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
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
        if (hasAirMace(player)) {
            setGranted(player, true);
            return;
        }
        if (isGranted(player)) {
            // Player explicitly got rid of the mace (dropped/stashed/etc). Do not respawn spam;
            // they can re-enable the passive (energy/gem switch) to get a new one.
            return;
        }
        ItemStack mace = createAirMace(player);
        if (mace.isEmpty()) {
            return;
        }
        player.giveItemStack(mace);
        setGranted(player, true);
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        // Intentionally does not remove the mace to avoid deleting player-owned items.
        setGranted(player, false);
    }

    public static boolean isHoldingMace(ServerPlayerEntity player) {
        return player.getMainHandStack().isOf(Items.MACE);
    }

    private static boolean hasAirMace(ServerPlayerEntity player) {
        for (ItemStack stack : player.getInventory().main) {
            if (isAirMace(stack)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offHand) {
            if (isAirMace(stack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAirMace(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!stack.isOf(Items.MACE)) {
            return false;
        }
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) {
            return false;
        }
        return custom.getNbt().contains(TAG_AIR_MACE, NbtElement.BYTE_TYPE);
    }

    private static ItemStack createAirMace(ServerPlayerEntity player) {
        ItemStack mace = new ItemStack(Items.MACE);
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, mace, nbt -> nbt.putBoolean(TAG_AIR_MACE, true));

        RegistryEntry<Enchantment> breach = resolve(player, Enchantments.BREACH);
        RegistryEntry<Enchantment> windBurst = resolve(player, Enchantments.WIND_BURST);
        RegistryEntry<Enchantment> mending = resolve(player, Enchantments.MENDING);
        RegistryEntry<Enchantment> unbreaking = resolve(player, Enchantments.UNBREAKING);
        RegistryEntry<Enchantment> fireAspect = resolve(player, Enchantments.FIRE_ASPECT);

        EnchantmentHelper.apply(mace, builder -> {
            if (breach != null) {
                builder.set(breach, 4);
            }
            if (windBurst != null) {
                builder.set(windBurst, 3);
            }
            if (mending != null) {
                builder.set(mending, 1);
            }
            if (unbreaking != null) {
                builder.set(unbreaking, 3);
            }
            if (fireAspect != null) {
                builder.set(fireAspect, 2);
            }
        });
        return mace;
    }

    private static boolean isGranted(ServerPlayerEntity player) {
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        return data.getBoolean(KEY_GRANTED);
    }

    private static void setGranted(ServerPlayerEntity player, boolean granted) {
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        data.putBoolean(KEY_GRANTED, granted);
    }

    private static RegistryEntry<Enchantment> resolve(ServerPlayerEntity player, RegistryKey<Enchantment> key) {
        var registry = player.getServerWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
        var entry = registry.getEntry(key);
        return entry.orElse(null);
    }
}
