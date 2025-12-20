package com.feel.gems.screen;

import com.feel.gems.core.GemId;
import com.feel.gems.trade.GemTrading;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

public final class TraderScreenHandler extends ScreenHandler {
    public TraderScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ModScreenHandlers.TRADER, syncId);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (!(player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)) {
            return false;
        }

        GemId[] gems = GemId.values();
        if (id < 0 || id >= gems.length) {
            return false;
        }

        GemId gemId = gems[id];
        GemTrading.Result result = GemTrading.trade(serverPlayer, gemId);
        if (!result.success()) {
            return false;
        }

        serverPlayer.sendMessage(Text.literal("Traded for " + gemId.name()), true);
        serverPlayer.closeHandledScreen();
        return true;
    }
}
