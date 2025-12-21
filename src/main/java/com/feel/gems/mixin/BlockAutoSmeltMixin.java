package com.feel.gems.mixin;

import com.feel.gems.power.AutoSmeltCache;
import com.feel.gems.power.AbilityRuntime;
import com.feel.gems.power.GemPowers;
import com.feel.gems.power.PowerIds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(Block.class)
public abstract class BlockAutoSmeltMixin {
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
                ItemStack smelted = AutoSmeltCache.smeltResult(world, stack);
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
}
