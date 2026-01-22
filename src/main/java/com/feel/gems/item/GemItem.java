package com.feel.gems.item;

import com.feel.gems.core.GemId;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import java.util.UUID;
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

        UUID ownerUuid = GemOwnership.ownerUuid(stack);
        boolean ownedByPlayer = ownerUuid != null && ownerUuid.equals(player.getUuid());
        if (GemOwnership.isInvalidForEpoch(server, stack)) {
            if (ownedByPlayer) {
                // Refresh epoch for your own stale gem so it can still be activated (common when carrying multiple gems across deaths).
                GemOwnership.tagOwned(stack, player);
            } else if (GemOwnership.purgeIfInvalid(server, stack)) {
                player.setStackInHand(hand, ItemStack.EMPTY);
                player.sendMessage(Text.translatable("gems.item.gem.reclaimed"), true);
                return ActionResult.SUCCESS.withNewHandStack(ItemStack.EMPTY);
            }
        }

        if (ownerUuid != null && !ownedByPlayer) {
            ServerPlayerEntity owner = server.getPlayerManager().getPlayer(ownerUuid);
            if (owner != null) {
                GemPlayerState.initIfNeeded(owner);
                if (GemPlayerState.getActiveGem(owner) == gemId) {
                    if (owner.isAlive()) {
                        GemOwnership.applyOwnerPenalty(owner);
                    }
                    player.sendMessage(Text.translatable("gems.item.gem.not_owned"), true);
                    player.setStackInHand(hand, ItemStack.EMPTY);
                    return ActionResult.SUCCESS.withNewHandStack(ItemStack.EMPTY);
                }
            }
        }

        boolean ownsGem = GemPlayerState.getOwnedGems(player).contains(gemId);
        if (!ownsGem) {
            if (player.isCreative()) {
                GemPlayerState.addOwnedGem(player, gemId);
                ownsGem = true;
            } else {
                // Claim the gem from the item so non-active drops behave like vanilla loot.
                GemPlayerState.addOwnedGem(player, gemId);
                GemOwnership.tagOwned(stack, player);
                ownsGem = true;
            }
        }

        GemPlayerState.setActiveGem(player, gemId);
        AugmentRuntime.captureActiveGemAugments(player, gemId, stack);
        GemPowers.sync(player);
        GemStateSync.send(player);
        GemItemGlint.sync(player);
        player.sendMessage(Text.translatable("gems.item.gem.activated", gemId.name()), true);
        return ActionResult.SUCCESS;
    }
}
