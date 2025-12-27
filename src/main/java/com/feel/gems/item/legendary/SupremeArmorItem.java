package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.legendary.LegendaryItem;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import java.util.List;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.item.trim.ArmorTrimMaterials;
import net.minecraft.item.trim.ArmorTrimPatterns;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;




public final class SupremeArmorItem extends ArmorItem implements LegendaryItem {
    public enum Piece {
        HELMET("supreme_helmet"),
        CHESTPLATE("supreme_chestplate"),
        LEGGINGS("supreme_leggings"),
        BOOTS("supreme_boots");

        private final String id;

        Piece(String id) {
            this.id = id;
        }
    }

    private final Piece piece;

    public SupremeArmorItem(RegistryEntry<ArmorMaterial> material, Type type, Piece piece, Item.Settings settings) {
        super(material, type, settings);
        this.piece = piece;
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, piece.id).toString();
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("item.gems." + piece.id + ".desc"));
        tooltip.add(Text.translatable("item.gems.supreme_set_bonus"));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient) {
            return;
        }
        applyTrimIfMissing(stack, world.getRegistryManager());
    }

    public static void applySupremeTrim(ItemStack stack, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        if (stack.contains(DataComponentTypes.TRIM)) {
            return;
        }
        if (lookup == null) {
            return;
        }
        RegistryEntryLookup.RegistryLookup registryLookup = lookup.createRegistryLookup();
        RegistryKey<net.minecraft.item.trim.ArmorTrimMaterial> materialKey = ArmorTrimMaterials.NETHERITE;
        RegistryKey<net.minecraft.item.trim.ArmorTrimPattern> patternKey = ArmorTrimPatterns.SILENCE;
        var material = registryLookup.getOptionalEntry(RegistryKeys.TRIM_MATERIAL, materialKey).orElse(null);
        var pattern = registryLookup.getOptionalEntry(RegistryKeys.TRIM_PATTERN, patternKey).orElse(null);
        if (material == null || pattern == null) {
            return;
        }
        stack.set(DataComponentTypes.TRIM, new ArmorTrim(material, pattern, true));
    }

    private static void applyTrimIfMissing(ItemStack stack, net.minecraft.registry.DynamicRegistryManager registries) {
        if (stack.contains(DataComponentTypes.TRIM)) {
            return;
        }
        if (registries == null) {
            return;
        }
        var materials = registries.get(RegistryKeys.TRIM_MATERIAL);
        var patterns = registries.get(RegistryKeys.TRIM_PATTERN);
        var material = materials.getEntry(ArmorTrimMaterials.NETHERITE).orElse(null);
        var pattern = patterns.getEntry(ArmorTrimPatterns.SILENCE).orElse(null);
        if (material == null || pattern == null) {
            return;
        }
        stack.set(DataComponentTypes.TRIM, new ArmorTrim(material, pattern, true));
    }
}
