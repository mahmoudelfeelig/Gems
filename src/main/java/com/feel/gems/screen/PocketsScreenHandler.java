package com.feel.gems.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;




public final class PocketsScreenHandler extends GenericContainerScreenHandler {
    private final ServerPlayerEntity owner;
    private final SimpleInventory pockets;

    public PocketsScreenHandler(int syncId, PlayerInventory playerInventory, ServerPlayerEntity owner, SimpleInventory pockets) {
        super(typeForRows(rowsFor(pockets)), syncId, playerInventory, pockets, rowsFor(pockets));
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

    private static int rowsFor(SimpleInventory pockets) {
        int size = pockets == null ? 0 : pockets.size();
        int rows = size / 9;
        return Math.max(1, Math.min(6, rows));
    }

    private static ScreenHandlerType<?> typeForRows(int rows) {
        return switch (rows) {
            case 2 -> ScreenHandlerType.GENERIC_9X2;
            case 3 -> ScreenHandlerType.GENERIC_9X3;
            case 4 -> ScreenHandlerType.GENERIC_9X4;
            case 5 -> ScreenHandlerType.GENERIC_9X5;
            case 6 -> ScreenHandlerType.GENERIC_9X6;
            case 1 -> ScreenHandlerType.GENERIC_9X1;
            default -> ScreenHandlerType.GENERIC_9X1;
        };
    }
}

