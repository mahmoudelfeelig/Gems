package com.feel.gems.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potions;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public final class FixedLootChest {
    private static final int SHULKER_SLOTS = 27;
    private static final BlockPos CHEST_POS = new BlockPos(4415, -20, -3024);
    private static final ChunkPos CHEST_CHUNK = new ChunkPos(CHEST_POS);

    private FixedLootChest() {
    }

    public static void placeIfTargetChunk(StructureWorldAccess world, Chunk chunk) {
        if (!chunk.getPos().equals(CHEST_CHUNK)) {
            return;
        }
        var serverWorld = world.toServerWorld();
        if (serverWorld == null || !serverWorld.getRegistryKey().equals(World.OVERWORLD)) {
            return;
        }

        // Replace whatever was there.
        carveChamber(world, CHEST_POS);
        world.setBlockState(CHEST_POS, Blocks.CHEST.getDefaultState(), Block.NOTIFY_LISTENERS);
        if (!(world.getBlockEntity(CHEST_POS) instanceof ChestBlockEntity chest)) {
            return;
        }

        fillChest(world, chest);
        chest.markDirty();
    }

    private static void carveChamber(StructureWorldAccess world, BlockPos center) {
        BlockPos min = center.add(-1, -1, -1);
        BlockPos max = center.add(1, 1, 1);
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    world.setBlockState(new BlockPos(x, y, z), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                }
            }
        }
    }

    private static void fillChest(StructureWorldAccess world, ChestBlockEntity chest) {
        DynamicRegistryManager registries = world.getRegistryManager();
        long seed = world.getSeed();

        List<ItemStack> contents = new ArrayList<>();
        contents.add(allGemsAndUpgradesShulker());
        contents.add(musicDiscsShulker());

        contents.add(shulkerBox("Gold & Iron", proportionalFill(List.of(
                stack(Items.GOLD_BLOCK, 64),
                stack(Items.IRON_BLOCK, 64)
        ), SHULKER_SLOTS)));

        contents.add(shulkerBox("Obsidian 1", fill(stack(Items.OBSIDIAN, 64), SHULKER_SLOTS)));
        contents.add(shulkerBox("Obsidian 2", fill(stack(Items.OBSIDIAN, 64), SHULKER_SLOTS)));
        contents.add(shulkerBox("Obsidian 3", fill(stack(Items.OBSIDIAN, 64), SHULKER_SLOTS)));

        contents.add(shulkerBox("Totems", fill(new ItemStack(Items.TOTEM_OF_UNDYING), SHULKER_SLOTS)));
        contents.add(shulkerBox("Totems 2", fill(new ItemStack(Items.TOTEM_OF_UNDYING), SHULKER_SLOTS)));
        
        contents.add(shulkerBox("Golden Carrots", fill(stack(Items.GOLDEN_CARROT, 64), SHULKER_SLOTS)));
        contents.add(shulkerBox("Ender Pearls", fill(stack(Items.ENDER_PEARL, 16), SHULKER_SLOTS)));

        contents.add(shulkerBox("XP Bottles 1", fill(stack(Items.EXPERIENCE_BOTTLE, 64), SHULKER_SLOTS)));
        contents.add(shulkerBox("XP Bottles 2", fill(stack(Items.EXPERIENCE_BOTTLE, 64), SHULKER_SLOTS)));
        contents.add(shulkerBox("XP Bottles 3", fill(stack(Items.EXPERIENCE_BOTTLE, 64), SHULKER_SLOTS)));
        contents.add(shulkerBox("XP Bottles 4", fill(stack(Items.EXPERIENCE_BOTTLE, 64), SHULKER_SLOTS)));
        contents.add(shulkerBox("XP Bottles 5", fill(stack(Items.EXPERIENCE_BOTTLE, 64), SHULKER_SLOTS)));

        contents.add(shulkerBox("Sand", fill(stack(Items.SAND, 64), SHULKER_SLOTS)));
        contents.add(shulkerBox("Gunpowder", fill(stack(Items.GUNPOWDER, 64), SHULKER_SLOTS)));

        contents.add(shulkerBox("Elytras", fill(new ItemStack(Items.ELYTRA), SHULKER_SLOTS)));
        contents.add(shulkerBox("Flight Rockets", fill(flightRockets(), SHULKER_SLOTS)));
        contents.add(shulkerBox("Crossbow Rockets", fill(maxDamageRockets(), SHULKER_SLOTS)));

        List<ItemStack> mix1 = new ArrayList<>(SHULKER_SLOTS);
        mix1.add(worldSeedBook(seed));
        mix1.addAll(proportionalFill(List.of(
                stack(Items.ANCIENT_DEBRIS, 64),
                stack(Items.SHULKER_SHELL, 64),
                stack(Items.WITHER_SKELETON_SKULL, 64),
                stack(Items.GOLDEN_APPLE, 64),
                stack(Items.DEEPSLATE_DIAMOND_ORE, 64)
        ), SHULKER_SLOTS - mix1.size()));
        contents.add(shulkerBox("Mix 1", mix1));

        List<ItemStack> mix2 = new ArrayList<>(SHULKER_SLOTS);
        mix2.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 32));
        mix2.add(stack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 64));
        mix2.add(stack(Items.TURTLE_SCUTE, 64));
        mix2.addAll(fill(stack(Items.EMERALD_BLOCK, 64), SHULKER_SLOTS - mix2.size()));
        contents.add(shulkerBox("Mix 2", mix2));
        contents.add(shulkerBox("Mix 3", proportionalFill(List.of(
                stack(Items.REDSTONE_BLOCK, 64),
                stack(Items.LAPIS_BLOCK, 64),
                stack(Items.SLIME_BLOCK, 64),
                stack(Items.BREEZE_ROD, 64),
                stack(Items.BLAZE_ROD, 64),
                stack(Items.NETHER_WART, 64),
                stack(Items.GLOWSTONE_DUST, 64)
        ), SHULKER_SLOTS)));

        for (int kit = 1; kit <= 4; kit++) {
            contents.add(pvpKitShulker(registries, kit));
        }

        for (int i = 0; i < contents.size() && i < chest.size(); i++) {
            chest.setStack(i, contents.get(i));
        }
    }

    private static ItemStack worldSeedBook(long seed) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        WrittenBookContentComponent content = new WrittenBookContentComponent(
                RawFilteredPair.of("World Seed"),
                "elfeel",
                0,
                List.of(RawFilteredPair.of(Text.literal("Seed: " + seed))),
                false
        );
        book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, content);
        return book;
    }

    private static ItemStack pvpKitShulker(DynamicRegistryManager registries, int kitNumber) {
        List<ItemStack> items = new ArrayList<>();
        boolean silkTools = kitNumber % 2 == 1;

        items.add(netHelmet(registries));
        items.add(netChestplate(registries));
        items.add(netLeggings(registries));
        items.add(silkTools ? netBootsFrostWalker(registries) : netBootsDepthStrider(registries));

        items.add(netSword(registries));
        items.add(netAxe(registries));
        items.add(netPickaxe(registries, silkTools));

        items.add(silkTools ? netBowMending(registries) : netBowInfinity(registries));
        items.add(netCrossbowMultishot(registries));
        items.add(netTridentRiptide(registries));
        items.add(netMace(registries, Map.of(
                Enchantments.BREACH, 4,
                Enchantments.WIND_BURST, 3
        )));

        items.add(netElytra(registries));
        items.add(stack(Items.GOLDEN_APPLE, 64));
        items.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 4));
        items.add(stack(Items.BEACON, 6));
        items.add(new ItemStack(Items.TOTEM_OF_UNDYING));
        items.add(new ItemStack(Items.TOTEM_OF_UNDYING));
        items.add(stack(Items.ENDER_PEARL, 16));
        items.add(maxDamageRockets());
        items.add(flightRockets());
        items.add(stack(Items.OBSIDIAN, 64));
        items.add(stack(Items.ENDER_CHEST, 64));
        items.add(strongHarmingArrows());
        items.add(stack(com.feel.gems.item.ModItems.ENERGY_UPGRADE, 16));
        items.add(stack(com.feel.gems.item.ModItems.HEART, 16));
        items.add(new ItemStack(com.feel.gems.item.ModItems.LIFE_GEM));
        items.add(new ItemStack(com.feel.gems.item.ModItems.ASTRA_GEM));

        renameKitItems(items, kitNumber);

        return shulkerBox("pvp kit " + kitNumber, items);
    }

    private static ItemStack netHelmet(DynamicRegistryManager registries) {
        ItemStack stack = new ItemStack(Items.NETHERITE_HELMET);
        applyEnchants(registries, stack, Map.of(
                Enchantments.PROTECTION, 4,
                Enchantments.UNBREAKING, 3,
                Enchantments.MENDING, 1,
                Enchantments.RESPIRATION, 3,
                Enchantments.AQUA_AFFINITY, 1
        ));
        return stack;
    }

    private static ItemStack netChestplate(DynamicRegistryManager registries) {
        ItemStack stack = new ItemStack(Items.NETHERITE_CHESTPLATE);
        applyEnchants(registries, stack, Map.of(
                Enchantments.PROTECTION, 4,
                Enchantments.UNBREAKING, 3,
                Enchantments.MENDING, 1,
                Enchantments.THORNS, 3
        ));
        return stack;
    }

    private static ItemStack netLeggings(DynamicRegistryManager registries) {
        ItemStack stack = new ItemStack(Items.NETHERITE_LEGGINGS);
        applyEnchants(registries, stack, Map.of(
                Enchantments.PROTECTION, 4,
                Enchantments.UNBREAKING, 3,
                Enchantments.MENDING, 1,
                Enchantments.SWIFT_SNEAK, 3
        ));
        return stack;
    }

    private static ItemStack netBootsFrostWalker(DynamicRegistryManager registries) {
        ItemStack stack = new ItemStack(Items.NETHERITE_BOOTS);
        applyEnchants(registries, stack, Map.of(
                Enchantments.PROTECTION, 4,
                Enchantments.UNBREAKING, 3,
                Enchantments.MENDING, 1,
                Enchantments.FEATHER_FALLING, 4,
                Enchantments.SOUL_SPEED, 3,
                Enchantments.FROST_WALKER, 2
        ));
        return stack;
    }

    private static ItemStack netBootsDepthStrider(DynamicRegistryManager registries) {
        ItemStack stack = new ItemStack(Items.NETHERITE_BOOTS);
        applyEnchants(registries, stack, Map.of(
                Enchantments.PROTECTION, 4,
                Enchantments.UNBREAKING, 3,
                Enchantments.MENDING, 1,
                Enchantments.FEATHER_FALLING, 4,
                Enchantments.SOUL_SPEED, 3,
                Enchantments.DEPTH_STRIDER, 3
        ));
        return stack;
    }

    private static ItemStack netSword(DynamicRegistryManager registries) {
        return enchanted(registries, Items.NETHERITE_SWORD, Map.of(
                Enchantments.SHARPNESS, 5,
                Enchantments.UNBREAKING, 3,
                Enchantments.MENDING, 1,
                Enchantments.SWEEPING_EDGE, 3,
                Enchantments.LOOTING, 3,
                Enchantments.FIRE_ASPECT, 2
        ));
    }

    private static ItemStack netAxe(DynamicRegistryManager registries) {
        return enchanted(registries, Items.NETHERITE_AXE, Map.of(
                Enchantments.SHARPNESS, 5,
                Enchantments.EFFICIENCY, 5,
                Enchantments.UNBREAKING, 3,
                Enchantments.MENDING, 1,
                Enchantments.SILK_TOUCH, 1
        ));
    }

    private static ItemStack netPickaxe(DynamicRegistryManager registries, boolean silk) {
        return enchanted(registries, Items.NETHERITE_PICKAXE, Map.of(
                Enchantments.EFFICIENCY, 5,
                Enchantments.UNBREAKING, 3,
                Enchantments.MENDING, 1,
                silk ? Enchantments.SILK_TOUCH : Enchantments.FORTUNE, silk ? 1 : 3
        ));
    }

    private static ItemStack netBowMending(DynamicRegistryManager registries) {
        return enchanted(registries, Items.BOW, Map.of(
                Enchantments.POWER, 5,
                Enchantments.PUNCH, 2,
                Enchantments.FLAME, 1,
                Enchantments.UNBREAKING, 3,
                Enchantments.MENDING, 1
        ));
    }

    private static ItemStack netBowInfinity(DynamicRegistryManager registries) {
        return enchanted(registries, Items.BOW, Map.of(
                Enchantments.POWER, 5,
                Enchantments.PUNCH, 2,
                Enchantments.FLAME, 1,
                Enchantments.UNBREAKING, 3,
                Enchantments.INFINITY, 1
        ));
    }

    private static ItemStack netCrossbowMultishot(DynamicRegistryManager registries) {
        return enchanted(registries, Items.CROSSBOW, Map.of(
                Enchantments.MULTISHOT, 1,
                Enchantments.QUICK_CHARGE, 3,
                Enchantments.UNBREAKING, 3,
                Enchantments.MENDING, 1
        ));
    }

    private static ItemStack netTridentRiptide(DynamicRegistryManager registries) {
        return enchanted(registries, Items.TRIDENT, Map.of(
                Enchantments.IMPALING, 5,
                Enchantments.UNBREAKING, 3,
                Enchantments.MENDING, 1,
                Enchantments.RIPTIDE, 3
        ));
    }

    private static ItemStack netMace(DynamicRegistryManager registries, Map<RegistryKey<Enchantment>, Integer> extra) {
        return enchanted(registries, Items.MACE, merge(
                Map.of(
                        Enchantments.UNBREAKING, 3,
                        Enchantments.MENDING, 1
                ),
                extra
        ));
    }

    private static ItemStack netElytra(DynamicRegistryManager registries) {
        return enchanted(registries, Items.ELYTRA, Map.of(
                Enchantments.UNBREAKING, 3,
                Enchantments.MENDING, 1
        ));
    }

    private static ItemStack enchanted(DynamicRegistryManager registries, Item item, Map<RegistryKey<Enchantment>, Integer> enchants) {
        ItemStack stack = new ItemStack(item);
        applyEnchants(registries, stack, enchants);
        return stack;
    }

    private static ItemStack flightRockets() {
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET, 64);
        stack.set(DataComponentTypes.FIREWORKS, new FireworksComponent(3, List.of()));
        return stack;
    }

    private static ItemStack maxDamageRockets() {
        ItemStack stack = new ItemStack(Items.FIREWORK_ROCKET, 64);
        List<FireworkExplosionComponent> explosions = List.of(
                explosion(0xFF0000, 0xFFAA00),
                explosion(0x00FF00, 0x00AAFF),
                explosion(0x0000FF, 0xAA00FF),
                explosion(0xFFFFFF, 0xAAAAAA),
                explosion(0xFF00FF, 0x00FFFF),
                explosion(0xFFFF00, 0xFF5500),
                explosion(0x55FF55, 0x5555FF)
        );
        stack.set(DataComponentTypes.FIREWORKS, new FireworksComponent(1, explosions));
        return stack;
    }

    private static ItemStack musicDiscsShulker() {
        List<ItemStack> items = new ArrayList<>();
        items.add(new ItemStack(Items.MUSIC_DISC_13));
        items.add(new ItemStack(Items.MUSIC_DISC_CAT));
        items.add(new ItemStack(Items.MUSIC_DISC_BLOCKS));
        items.add(new ItemStack(Items.MUSIC_DISC_CHIRP));
        items.add(new ItemStack(Items.MUSIC_DISC_FAR));
        items.add(new ItemStack(Items.MUSIC_DISC_MALL));
        items.add(new ItemStack(Items.MUSIC_DISC_MELLOHI));
        items.add(new ItemStack(Items.MUSIC_DISC_STAL));
        items.add(new ItemStack(Items.MUSIC_DISC_STRAD));
        items.add(new ItemStack(Items.MUSIC_DISC_WARD));
        items.add(new ItemStack(Items.MUSIC_DISC_11));
        items.add(new ItemStack(Items.MUSIC_DISC_WAIT));
        items.add(new ItemStack(Items.MUSIC_DISC_OTHERSIDE));
        items.add(new ItemStack(Items.MUSIC_DISC_PIGSTEP));
        items.add(new ItemStack(Items.MUSIC_DISC_5));
        items.add(new ItemStack(Items.MUSIC_DISC_RELIC));
        items.add(new ItemStack(Items.MUSIC_DISC_CREATOR));
        items.add(new ItemStack(Items.MUSIC_DISC_CREATOR_MUSIC_BOX));
        items.add(new ItemStack(Items.MUSIC_DISC_PRECIPICE));
        items.add(new ItemStack(Items.HEART_OF_THE_SEA));

        if (items.size() < SHULKER_SLOTS) {
            items.addAll(fill(stack(Items.TNT, 64), SHULKER_SLOTS - items.size()));
        }
        return shulkerBox("Music & TNT", items);
    }

    private static ItemStack allGemsAndUpgradesShulker() {
        List<ItemStack> items = new ArrayList<>(SHULKER_SLOTS);

        // One of each gem.
        items.add(new ItemStack(com.feel.gems.item.ModItems.ASTRA_GEM));
        items.add(new ItemStack(com.feel.gems.item.ModItems.FIRE_GEM));
        items.add(new ItemStack(com.feel.gems.item.ModItems.FLUX_GEM));
        items.add(new ItemStack(com.feel.gems.item.ModItems.LIFE_GEM));
        items.add(new ItemStack(com.feel.gems.item.ModItems.PUFF_GEM));
        items.add(new ItemStack(com.feel.gems.item.ModItems.SPEED_GEM));
        items.add(new ItemStack(com.feel.gems.item.ModItems.STRENGTH_GEM));
        items.add(new ItemStack(com.feel.gems.item.ModItems.WEALTH_GEM));
        items.add(new ItemStack(com.feel.gems.item.ModItems.TERROR_GEM));
        items.add(new ItemStack(com.feel.gems.item.ModItems.SUMMONER_GEM));
        items.add(new ItemStack(com.feel.gems.item.ModItems.SPACE_GEM));
        items.add(new ItemStack(com.feel.gems.item.ModItems.REAPER_GEM));
        items.add(new ItemStack(com.feel.gems.item.ModItems.PILLAGER_GEM));
        items.add(new ItemStack(com.feel.gems.item.ModItems.SPY_MIMIC_GEM));
        items.add(new ItemStack(com.feel.gems.item.ModItems.BEACON_GEM));
        items.add(new ItemStack(com.feel.gems.item.ModItems.AIR_GEM));

        // Fill remaining slots with full stacks of upgrades/hearts.
        boolean upgradesNext = true;
        while (items.size() < SHULKER_SLOTS) {
            items.add(stack(upgradesNext ? com.feel.gems.item.ModItems.ENERGY_UPGRADE : com.feel.gems.item.ModItems.HEART, 16));
            upgradesNext = !upgradesNext;
        }

        return shulkerBox("All Gems", items);
    }

    private static FireworkExplosionComponent explosion(int primaryRgb, int fadeRgb) {
        return new FireworkExplosionComponent(
                FireworkExplosionComponent.Type.LARGE_BALL,
                ints(primaryRgb),
                ints(fadeRgb),
                true,
                true
        );
    }

    private static IntList ints(int... rgb) {
        IntArrayList list = new IntArrayList(rgb.length);
        for (int value : rgb) {
            list.add(value);
        }
        return list;
    }

    private static ItemStack shulkerBox(String name, List<ItemStack> items) {
        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        box.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name));
        box.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(items));
        return box;
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }

    private static List<ItemStack> fill(ItemStack prototype, int slots) {
        List<ItemStack> stacks = new ArrayList<>(slots);
        for (int i = 0; i < slots; i++) {
            stacks.add(prototype.copy());
        }
        return stacks;
    }

    private static List<ItemStack> proportionalFill(List<ItemStack> prototypes, int slots) {
        if (prototypes.isEmpty() || slots <= 0) {
            return List.of();
        }
        List<ItemStack> stacks = new ArrayList<>(slots);
        int base = slots / prototypes.size();
        int remainder = slots % prototypes.size();
        for (int i = 0; i < prototypes.size(); i++) {
            int take = base + (i < remainder ? 1 : 0);
            for (int j = 0; j < take; j++) {
                stacks.add(prototypes.get(i).copy());
            }
        }
        return stacks;
    }

    private static ItemStack strongHarmingArrows() {
        ItemStack stack = new ItemStack(Items.TIPPED_ARROW, 64);
        stack.set(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT.with(Potions.STRONG_HARMING));
        return stack;
    }

    private static void renameKitItems(List<ItemStack> items, int kitNumber) {
        Text name = Text.literal(Integer.toString(kitNumber));
        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                continue;
            }
            stack.set(DataComponentTypes.CUSTOM_NAME, name);
        }
    }

    private static void applyEnchants(DynamicRegistryManager registries, ItemStack stack, Map<RegistryKey<Enchantment>, Integer> enchants) {
        if (registries == null || enchants.isEmpty()) {
            return;
        }
        var registry = registries.getOrThrow(RegistryKeys.ENCHANTMENT);
        for (Map.Entry<RegistryKey<Enchantment>, Integer> entry : enchants.entrySet()) {
            RegistryEntry<Enchantment> enchant = registry.getEntry(entry.getKey().getValue()).orElse(null);
            if (enchant == null) {
                continue;
            }
            int level = Math.max(1, entry.getValue());
            EnchantmentHelper.apply(stack, builder -> builder.set(enchant, level));
        }
    }

    private static Map<RegistryKey<Enchantment>, Integer> merge(Map<RegistryKey<Enchantment>, Integer> base, Map<RegistryKey<Enchantment>, Integer> extra) {
        Map<RegistryKey<Enchantment>, Integer> merged = new java.util.HashMap<>(base);
        merged.putAll(extra);
        return merged;
    }
}
