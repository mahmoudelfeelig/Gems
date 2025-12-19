package com.blissmc.gems.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;

public final class PocketsScreenHandler extends GenericContainerScreenHandler {
    private final ServerPlayerEntity owner;
    private final SimpleInventory pockets;

    public PocketsScreenHandler(int syncId, PlayerInventory playerInventory, ServerPlayerEntity owner, SimpleInventory pockets) {
        super(ScreenHandlerType.GENERIC_9X1, syncId, playerInventory, pockets, 1);
        this.owner = owner;
        this.pockets = pockets;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (player instanceof ServerPlayerEntity serverPlayer && serverPlayer == owner) {
            PocketsStorage.save(owner, pockets);
        }
    }
}

