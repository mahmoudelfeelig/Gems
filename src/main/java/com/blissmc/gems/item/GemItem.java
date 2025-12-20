package com.feel.gems.item;

import com.feel.gems.core.GemId;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.power.FluxCharge;
import com.feel.gems.power.GemPowers;
import com.feel.gems.state.GemPlayerState;
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

        if (player.isSneaking() && gemId == GemId.FLUX) {
            boolean changedGem = false;
            if (GemPlayerState.getActiveGem(player) != GemId.FLUX) {
                GemPlayerState.setActiveGem(player, GemId.FLUX);
                GemPowers.sync(player);
                changedGem = true;
            }

            boolean charged = FluxCharge.tryConsumeChargeItem(player);
            if (charged || changedGem) {
                GemStateSync.send(player);
                GemItemGlint.sync(player);
            }
            return TypedActionResult.success(stack);
        }

        GemPlayerState.setActiveGem(player, gemId);
        GemPowers.sync(player);
        GemStateSync.send(player);
        GemItemGlint.sync(player);
        player.sendMessage(Text.literal("Active gem set to " + gemId.name()), true);
        return TypedActionResult.success(stack);
    }
}
