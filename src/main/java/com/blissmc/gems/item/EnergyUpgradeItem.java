package com.feel.gems.item;

import com.feel.gems.net.GemStateSync;
import com.feel.gems.power.GemPowers;
import com.feel.gems.state.GemPlayerState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class EnergyUpgradeItem extends Item {
    public EnergyUpgradeItem(Settings settings) {
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
        int before = GemPlayerState.getEnergy(player);
        if (before >= GemPlayerState.MAX_ENERGY) {
            return TypedActionResult.fail(stack);
        }

        GemPlayerState.setEnergy(player, before + 1);
        GemPowers.sync(player);
        GemStateSync.send(player);
        GemItemGlint.sync(player);
        stack.decrement(1);
        return TypedActionResult.success(stack);
    }
}
