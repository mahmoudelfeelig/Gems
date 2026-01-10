package com.feel.gems.power.gem.air;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemMaintainedPassive;
import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;


public final class AirMacePassive implements GemMaintainedPassive {
    private static final String TAG_AIR_MACE = "gemsAirMace";
    /** Persistent flag: mace was ever granted to this player (never resets except by admin). */
    private static final String KEY_EVER_GRANTED = "airMaceEverGranted";

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
        // If player was ever granted a mace, never give another.
        if (isEverGranted(player)) {
            return;
        }
        ItemStack mace = createAirMace(player);
        if (mace.isEmpty()) {
            return;
        }
        player.giveItemStack(mace);
        setEverGranted(player, true);
    }

    @Override
    public void remove(ServerPlayerEntity player) {
        // Intentionally does not remove the mace or reset the flag.
        // One mace ever - flag persists forever.
    }

    public static boolean isHoldingMace(ServerPlayerEntity player) {
        return player.getMainHandStack().isOf(Items.MACE);
    }

    private static ItemStack createAirMace(ServerPlayerEntity player) {
        ItemStack mace = new ItemStack(Items.MACE);
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, mace, nbt -> nbt.putBoolean(TAG_AIR_MACE, true));

        var cfg = GemsBalance.v().air();
        RegistryEntry<Enchantment> breach = resolve(player, Enchantments.BREACH);
        RegistryEntry<Enchantment> windBurst = resolve(player, Enchantments.WIND_BURST);
        RegistryEntry<Enchantment> mending = resolve(player, Enchantments.MENDING);
        RegistryEntry<Enchantment> unbreaking = resolve(player, Enchantments.UNBREAKING);
        RegistryEntry<Enchantment> fireAspect = resolve(player, Enchantments.FIRE_ASPECT);

        EnchantmentHelper.apply(mace, builder -> {
            if (breach != null && cfg.airMaceBreachLevel() > 0) {
                builder.set(breach, cfg.airMaceBreachLevel());
            }
            if (windBurst != null && cfg.airMaceWindBurstLevel() > 0) {
                builder.set(windBurst, cfg.airMaceWindBurstLevel());
            }
            if (mending != null && cfg.airMaceMendingLevel() > 0) {
                builder.set(mending, cfg.airMaceMendingLevel());
            }
            if (unbreaking != null && cfg.airMaceUnbreakingLevel() > 0) {
                builder.set(unbreaking, cfg.airMaceUnbreakingLevel());
            }
            if (fireAspect != null && cfg.airMaceFireAspectLevel() > 0) {
                builder.set(fireAspect, cfg.airMaceFireAspectLevel());
            }
        });
        return mace;
    }

    private static boolean isEverGranted(ServerPlayerEntity player) {
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        return data.getBoolean(KEY_EVER_GRANTED, false);
    }

    private static void setEverGranted(ServerPlayerEntity player, boolean granted) {
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        data.putBoolean(KEY_EVER_GRANTED, granted);
    }

    /**
     * Clears the "ever granted" flag, allowing the player to receive a new mace.
     * Only for admin commands.
     */
    public static void clearEverGranted(ServerPlayerEntity player) {
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        data.remove(KEY_EVER_GRANTED);
    }

    private static RegistryEntry<Enchantment> resolve(ServerPlayerEntity player, RegistryKey<Enchantment> key) {
        if (!(player.getEntityWorld() instanceof net.minecraft.server.world.ServerWorld world)) {
            return null;
        }
        var registry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        var entry = registry.getEntry(key.getValue());
        return entry.orElse(null);
    }
}
