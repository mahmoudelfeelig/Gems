package com.feel.gems.item;

import com.feel.gems.core.GemId;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.power.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.item.GemOwnership;
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

        if (GemOwnership.purgeIfInvalid(player.getServer(), stack)) {
            player.setStackInHand(hand, ItemStack.EMPTY);
            player.sendMessage(Text.literal("This gem has been reclaimed by its owner."), true);
            return TypedActionResult.fail(ItemStack.EMPTY);
        }

        var ownerUuid = GemOwnership.ownerUuid(stack);
        if (ownerUuid != null && !ownerUuid.equals(player.getUuid())) {
            ServerPlayerEntity owner = player.getServer().getPlayerManager().getPlayer(ownerUuid);
            if (owner != null && owner.isAlive()) {
                GemOwnership.applyOwnerPenalty(owner);
            } else {
                // Owner offline: queue penalty for next login.
                GemOwnership.queueOfflinePenalty(player.getServer(), ownerUuid);
            }
        }

        GemOwnership.ensureOwner(stack, player);

        GemPlayerState.setActiveGem(player, gemId);
        GemPowers.sync(player);
        GemStateSync.send(player);
        GemItemGlint.sync(player);
        player.sendMessage(Text.literal("Active gem set to " + gemId.name()), true);
        return TypedActionResult.success(stack);
    }
}
