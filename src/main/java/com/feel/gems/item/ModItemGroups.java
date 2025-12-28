package com.feel.gems.item;

import com.feel.gems.GemsMod;
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
                            entries.add(ModItems.ASTRA_GEM);
                            entries.add(ModItems.FIRE_GEM);
                            entries.add(ModItems.FLUX_GEM);
                            entries.add(ModItems.LIFE_GEM);
                            entries.add(ModItems.PUFF_GEM);
                            entries.add(ModItems.SPEED_GEM);
                            entries.add(ModItems.STRENGTH_GEM);
                            entries.add(ModItems.WEALTH_GEM);
                            entries.add(ModItems.TERROR_GEM);
                            entries.add(ModItems.SUMMONER_GEM);
                            entries.add(ModItems.SPACE_GEM);
                    entries.add(ModItems.REAPER_GEM);
                    entries.add(ModItems.PILLAGER_GEM);
                    entries.add(ModItems.SPY_MIMIC_GEM);
                    entries.add(ModItems.BEACON_GEM);
                    entries.add(ModItems.AIR_GEM);

                            entries.add(ModItems.ENERGY_UPGRADE);
                            entries.add(ModItems.HEART);
                            entries.add(ModItems.GEM_TRADER);
                            entries.add(ModItems.GEM_PURCHASE);

                            entries.add(ModItems.TRACKER_COMPASS);
                            entries.add(ModItems.RECALL_RELIC);
                            entries.add(ModItems.HYPNO_STAFF);
                            entries.add(ModItems.EARTHSPLITTER_PICK);
                            RegistryWrapper.WrapperLookup lookup = displayContext.lookup();
                            entries.add(trimmedSupreme(ModItems.SUPREME_HELMET, lookup));
                            entries.add(trimmedSupreme(ModItems.SUPREME_CHESTPLATE, lookup));
                            entries.add(trimmedSupreme(ModItems.SUPREME_LEGGINGS, lookup));
                            entries.add(trimmedSupreme(ModItems.SUPREME_BOOTS, lookup));
                            entries.add(ModItems.BLOOD_OATH_BLADE);
                            entries.add(ModItems.DEMOLITION_BLADE);
                            entries.add(ModItems.HUNTERS_SIGHT_BOW);
                            entries.add(ModItems.THIRD_STRIKE_BLADE);
                            entries.add(ModItems.VAMPIRIC_EDGE);
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
