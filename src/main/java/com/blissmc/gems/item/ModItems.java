package com.feel.gems.item;

import com.feel.gems.GemsMod;
import com.feel.gems.core.GemId;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
    public static final Item HEART = register("heart", new HeartItem(new Item.Settings().maxCount(16)));
    public static final Item ENERGY_UPGRADE = register("energy_upgrade", new EnergyUpgradeItem(new Item.Settings().maxCount(16)));
    public static final Item TRADER = register("trader", new TraderItem(new Item.Settings().maxCount(1)));

    public static final Item ASTRA_GEM = registerGem(GemId.ASTRA);
    public static final Item FIRE_GEM = registerGem(GemId.FIRE);
    public static final Item FLUX_GEM = registerGem(GemId.FLUX);
    public static final Item LIFE_GEM = registerGem(GemId.LIFE);
    public static final Item PUFF_GEM = registerGem(GemId.PUFF);
    public static final Item SPEED_GEM = registerGem(GemId.SPEED);
    public static final Item STRENGTH_GEM = registerGem(GemId.STRENGTH);
    public static final Item WEALTH_GEM = registerGem(GemId.WEALTH);

    private ModItems() {
    }

    public static void init() {
        // Triggers static initialization.
        ModItemGroups.init();
    }

    public static Item gemItem(GemId gemId) {
        return switch (gemId) {
            case ASTRA -> ASTRA_GEM;
            case FIRE -> FIRE_GEM;
            case FLUX -> FLUX_GEM;
            case LIFE -> LIFE_GEM;
            case PUFF -> PUFF_GEM;
            case SPEED -> SPEED_GEM;
            case STRENGTH -> STRENGTH_GEM;
            case WEALTH -> WEALTH_GEM;
        };
    }

    private static Item registerGem(GemId gemId) {
        return register(gemId.name().toLowerCase() + "_gem", new GemItem(gemId, new Item.Settings().maxCount(1)));
    }

    private static Item register(String path, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(GemsMod.MOD_ID, path), item);
    }
}
