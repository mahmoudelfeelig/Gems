package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.util.GemsTooltipFormat;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import java.util.function.Consumer;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.item.equipment.trim.ArmorTrim;
import net.minecraft.item.equipment.trim.ArmorTrimMaterials;
import net.minecraft.item.equipment.trim.ArmorTrimPatterns;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;




public final class SupremeArmorItem extends Item implements LegendaryItem {
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

    public SupremeArmorItem(ArmorMaterial material, EquipmentType type, Piece piece, Item.Settings settings) {
        super(settings.armor(material, type).enchantable(15));
        this.piece = piece;
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, piece.id).toString();
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        GemsTooltipFormat.appendDescription(
                tooltip,
                Text.translatable("item.gems." + piece.id + ".desc"),
                Text.translatable("item.gems.supreme_set_bonus")
        );
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        applyTrimIfMissing(stack, world.getRegistryManager());
    }

    public static void applySupremeTrim(ItemStack stack, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        if (stack.contains(DataComponentTypes.TRIM)) {
            return;
        }
        if (lookup == null) {
            return;
        }
        RegistryKey<net.minecraft.item.equipment.trim.ArmorTrimMaterial> materialKey = ArmorTrimMaterials.NETHERITE;
        RegistryKey<net.minecraft.item.equipment.trim.ArmorTrimPattern> patternKey = ArmorTrimPatterns.SILENCE;
        var material = lookup.getOptionalEntry(materialKey).orElse(null);
        var pattern = lookup.getOptionalEntry(patternKey).orElse(null);
        if (material == null || pattern == null) {
            return;
        }
        stack.set(DataComponentTypes.TRIM, new ArmorTrim(material, pattern));
    }
	
    private static void applyTrimIfMissing(ItemStack stack, net.minecraft.registry.DynamicRegistryManager registries) {
        applySupremeTrim(stack, registries);
    }
}
