package com.feel.gems.power.gem.fire;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Optional;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.server.world.ServerWorld;


/**
 * Shared recipe lookup cache for auto-smelt.
 *
 * <p>Kept outside mixins because some Mixin runtimes disallow non-private static methods in mixin classes.</p>
 */
public final class AutoSmeltCache {
    private static final Object2ObjectOpenHashMap<Item, ItemStack> SMELT_CACHE = new Object2ObjectOpenHashMap<>();

    private AutoSmeltCache() {
    }

    public static void clear() {
        SMELT_CACHE.clear();
    }

    public static ItemStack smeltResult(ServerWorld world, ItemStack input) {
        if (input == null || input.isEmpty()) {
            return null;
        }
        if (input.contains(DataComponentTypes.CUSTOM_DATA) || input.contains(DataComponentTypes.BLOCK_ENTITY_DATA)) {
            return null;
        }

        Item item = input.getItem();
        ItemStack cached = SMELT_CACHE.get(item);
        if (cached != null) {
            return cached.isEmpty() ? null : scaledCopy(cached, input.getCount());
        }

        SingleStackRecipeInput recipeInput = new SingleStackRecipeInput(new ItemStack(item));
        Optional<? extends RecipeEntry<?>> match = world.getRecipeManager().getFirstMatch(RecipeType.SMELTING, recipeInput, world);

        if (match.isEmpty()) {
            SMELT_CACHE.put(item, ItemStack.EMPTY);
            return null;
        }

        @SuppressWarnings("unchecked")
        var recipe = (net.minecraft.recipe.Recipe<SingleStackRecipeInput>) match.get().value();
        ItemStack result = recipe.craft(recipeInput, world.getRegistryManager());
        if (result == null || result.isEmpty()) {
            SMELT_CACHE.put(item, ItemStack.EMPTY);
            return null;
        }

        SMELT_CACHE.put(item, result.copy());
        return scaledCopy(result, input.getCount());
    }

    private static ItemStack scaledCopy(ItemStack baseResult, int inputCount) {
        ItemStack out = baseResult.copy();
        out.setCount(baseResult.getCount() * inputCount);
        return out;
    }
}

