package com.feel.gems.item;

import com.feel.gems.net.GemStateSync;
import com.feel.gems.state.GemPlayerState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class HeartItem extends Item {
    public HeartItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            return TypedActionResult.pass(stack);
        }
        if (!(user instanceof ServerPlayerEntity player)) {
            return TypedActionResult.pass(stack);
        }

        GemPlayerState.initIfNeeded(player);
        int before = GemPlayerState.getMaxHearts(player);
        if (before >= GemPlayerState.MAX_MAX_HEARTS) {
            return TypedActionResult.fail(stack);
        }

        GemPlayerState.setMaxHearts(player, before + 1);
        GemPlayerState.applyMaxHearts(player);
        GemStateSync.send(player);
        stack.decrement(1);
        return TypedActionResult.success(stack);
    }
}
