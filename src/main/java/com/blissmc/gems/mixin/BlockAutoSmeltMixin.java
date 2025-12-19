package com.blissmc.gems.mixin;

import com.blissmc.gems.power.AbilityRuntime;
import com.blissmc.gems.power.GemPowers;
import com.blissmc.gems.power.PowerIds;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.recipe.RecipeEntry;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(Block.class)
public abstract class BlockAutoSmeltMixin {
    private static final Object2ObjectOpenHashMap<Item, ItemStack> SMELT_CACHE = new Object2ObjectOpenHashMap<>();

    public static void gems$clearSmeltCache() {
        SMELT_CACHE.clear();
    }

    @Inject(
            method = "getDroppedStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void gems$autoSmelt(
            BlockState state,
            ServerWorld world,
            BlockPos pos,
            BlockEntity blockEntity,
            Entity entity,
            ItemStack tool,
            CallbackInfoReturnable<List<ItemStack>> cir
    ) {
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }
        boolean autoSmelt = GemPowers.isPassiveActive(player, PowerIds.AUTO_SMELT);
        boolean richRush = AbilityRuntime.isRichRushActive(player);
        if (!autoSmelt && !richRush) {
            return;
        }

        List<ItemStack> original = cir.getReturnValue();
        if (original.isEmpty()) {
            return;
        }

        List<ItemStack> out = new ArrayList<>(original.size() + 2);
        boolean changed = false;
        for (ItemStack stack : original) {
            ItemStack next = stack;

            if (autoSmelt) {
                ItemStack smelted = smeltResult(world, stack);
                if (smelted != null) {
                    next = smelted;
                    changed = true;
                }
            }

            if (richRush && isOre(state)) {
                ItemStack doubled = next.copy();
                doubled.setCount(next.getCount() * 2);
                next = doubled;
                changed = true;
            }

            if (next == stack) {
                out.add(stack);
            } else {
                splitAndAdd(out, next);
            }
        }

        if (changed) {
            cir.setReturnValue(out);
        }
    }

    private static boolean isOre(BlockState state) {
        return state.isIn(BlockTags.COAL_ORES)
                || state.isIn(BlockTags.COPPER_ORES)
                || state.isIn(BlockTags.DIAMOND_ORES)
                || state.isIn(BlockTags.EMERALD_ORES)
                || state.isIn(BlockTags.GOLD_ORES)
                || state.isIn(BlockTags.IRON_ORES)
                || state.isIn(BlockTags.LAPIS_ORES)
                || state.isIn(BlockTags.REDSTONE_ORES);
    }

    private static void splitAndAdd(List<ItemStack> out, ItemStack stack) {
        int remaining = stack.getCount();
        int max = stack.getMaxCount();
        while (remaining > 0) {
            int part = Math.min(max, remaining);
            ItemStack copy = stack.copy();
            copy.setCount(part);
            out.add(copy);
            remaining -= part;
        }
    }

    private static ItemStack smeltResult(ServerWorld world, ItemStack input) {
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

        Optional<RecipeEntry<?>> match = world.getRecipeManager().getFirstMatch(
                RecipeType.SMELTING,
                new SingleStackRecipeInput(new ItemStack(item)),
                world
        ).map(entry -> (RecipeEntry<?>) entry);

        if (match.isEmpty()) {
            SMELT_CACHE.put(item, ItemStack.EMPTY);
            return null;
        }

        ItemStack result = match.get().value().getResult(world.getRegistryManager());
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
