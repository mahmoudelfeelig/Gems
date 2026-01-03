package com.feel.gems.item;

import com.feel.gems.core.GemId;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
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
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (!(user instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }

        var server = player.getEntityWorld().getServer();
        GemPlayerState.initIfNeeded(player);
        GemOwnership.ensureOwner(stack, player); // tag immediately so stale/no-owner items belong to the clicker

        boolean ownedByPlayer = GemOwnership.isOwnedBy(stack, player.getUuid());
        if (GemOwnership.isInvalidForEpoch(server, stack)) {
            if (ownedByPlayer) {
                // Refresh epoch for your own stale gem so it can still be activated (common when carrying multiple gems across deaths).
                GemOwnership.tagOwned(stack, player.getUuid(), GemPlayerState.getGemEpoch(player));
            } else if (GemOwnership.purgeIfInvalid(server, stack)) {
                player.setStackInHand(hand, ItemStack.EMPTY);
                player.sendMessage(Text.literal("This gem has been reclaimed by its owner."), true);
                return ActionResult.SUCCESS.withNewHandStack(ItemStack.EMPTY);
            }
        }

        // Gem items are only valid for gems you currently own (prevents re-activating traded-away gems).
        if (!GemPlayerState.getOwnedGems(player).contains(gemId)) {
            if (ownedByPlayer) {
                player.setStackInHand(hand, ItemStack.EMPTY);
                return ActionResult.SUCCESS.withNewHandStack(ItemStack.EMPTY);
            }
            player.sendMessage(Text.literal("You no longer own this gem."), true);
            return ActionResult.FAIL;
        }

        var ownerUuid = GemOwnership.ownerUuid(stack);
        if (ownerUuid != null && !ownerUuid.equals(player.getUuid())) {
            ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerUuid);
            if (owner != null && owner.isAlive()) {
                GemPlayerState.initIfNeeded(owner);
                if (GemPlayerState.getOwnedGems(owner).contains(gemId)) {
                    GemOwnership.applyOwnerPenalty(owner);
                }
            } else {
                // Owner offline: skip penalties (avoids punitive side-effects for stale/traded-away items).
            }
        }

        GemPlayerState.setActiveGem(player, gemId);
        GemPowers.sync(player);
        GemStateSync.send(player);
        GemItemGlint.sync(player);
        player.sendMessage(Text.literal("Active gem set to " + gemId.name()), true);
        return ActionResult.SUCCESS;
    }
}
