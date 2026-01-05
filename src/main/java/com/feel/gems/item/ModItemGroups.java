package com.feel.gems.item;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemId;
import com.feel.gems.item.legendary.SupremeArmorItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;




public final class ModItemGroups {
    private static boolean initialized = false;

    public static ItemGroup GEMS;

    private ModItemGroups() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        GEMS = Registry.register(
                Registries.ITEM_GROUP,
                Identifier.of(GemsMod.MOD_ID, "gems"),
                FabricItemGroup.builder()
                        .displayName(Text.translatable("itemGroup.gems"))
                        .icon(() -> new ItemStack(ModItems.CREATIVE_TAB_ICON))
                        .entries((displayContext, entries) -> {
                            RegistryWrapper.WrapperLookup lookup = displayContext.lookup();
                            
                            // === GEMS (first) ===
                            for (GemId gemId : GemId.values()) {
                                entries.add(ModItems.gemItem(gemId));
                            }
                            
                            // === NORMAL ITEMS ===
                            entries.add(ModItems.HEART);
                            entries.add(ModItems.ENERGY_UPGRADE);
                            entries.add(ModItems.GEM_TRADER);
                            entries.add(ModItems.GEM_PURCHASE);
                            
                            // === LEGENDARY ITEMS ===
                            // Utility
                            entries.add(ModItems.TRACKER_COMPASS);
                            entries.add(ModItems.RECALL_RELIC);
                            entries.add(ModItems.GEM_SEER);
                            entries.add(ModItems.HYPNO_STAFF);
                            entries.add(ModItems.EARTHSPLITTER_PICK);
                            
                            // Armor (with trims)
                            entries.add(trimmedSupreme(ModItems.SUPREME_HELMET, lookup));
                            entries.add(trimmedSupreme(ModItems.SUPREME_CHESTPLATE, lookup));
                            entries.add(trimmedSupreme(ModItems.SUPREME_LEGGINGS, lookup));
                            entries.add(trimmedSupreme(ModItems.SUPREME_BOOTS, lookup));
                            
                            // Weapons
                            entries.add(ModItems.BLOOD_OATH_BLADE);
                            entries.add(ModItems.DEMOLITION_BLADE);
                            entries.add(ModItems.THIRD_STRIKE_BLADE);
                            entries.add(ModItems.VAMPIRIC_EDGE);
                            entries.add(ModItems.EXPERIENCE_BLADE);
                            entries.add(ModItems.DUELISTS_RAPIER);
                            entries.add(ModItems.HUNTERS_SIGHT_BOW);
                            
                            // PvP Items
                            entries.add(ModItems.REVERSAL_MIRROR);
                            entries.add(ModItems.HUNTERS_TROPHY_NECKLACE);
                            entries.add(ModItems.GLADIATORS_MARK);
                            entries.add(ModItems.SOUL_SHACKLE);
                            entries.add(ModItems.CHALLENGERS_GAUNTLET);
                        })
                        .build()
        );
    }

    private static ItemStack trimmedSupreme(Item item, RegistryWrapper.WrapperLookup lookup) {
        ItemStack stack = new ItemStack(item);
        SupremeArmorItem.applySupremeTrim(stack, lookup);
        return stack;
    }
}
