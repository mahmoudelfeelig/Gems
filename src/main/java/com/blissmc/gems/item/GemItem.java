package com.blissmc.gems.item;

import com.blissmc.gems.core.GemId;
import com.blissmc.gems.net.GemStateSync;
import com.blissmc.gems.power.GemPowers;
import com.blissmc.gems.state.GemPlayerState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public final class GemItem extends Item {
    private final GemId gemId;

    public GemItem(GemId gemId, Settings settings) {
        super(settings);
        this.gemId = gemId;
    }

    public GemId gemId() {
        return gemId;
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
        GemPlayerState.setActiveGem(player, gemId);
        GemPowers.sync(player);
        GemStateSync.send(player);
        player.sendMessage(Text.literal("Active gem set to " + gemId.name()), true);
        return TypedActionResult.success(stack);
    }
}
