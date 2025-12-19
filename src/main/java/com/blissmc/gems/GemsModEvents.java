package com.blissmc.gems;

import com.blissmc.gems.state.GemPlayerState;
import com.blissmc.gems.item.ModItems;
import com.blissmc.gems.net.GemStateSync;
import com.blissmc.gems.power.GemPowers;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public final class GemsModEvents {
    private static int tickCounter = 0;

    private GemsModEvents() {
    }

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            GemPlayerState.initIfNeeded(player);
            GemPlayerState.applyMaxHearts(player);
            ensureActiveGemItem(player);
            GemPowers.sync(player);
            GemStateSync.send(player);
        });

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            GemPlayerState.copy(oldPlayer, newPlayer);
            GemPlayerState.initIfNeeded(newPlayer);
            GemPlayerState.applyMaxHearts(newPlayer);
            ensureActiveGemItem(newPlayer);
            GemPowers.sync(newPlayer);
            GemStateSync.send(newPlayer);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter % 40 != 0) {
                return;
            }
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                GemPowers.maintain(player);
            }
        });
    }

    private static void ensureActiveGemItem(ServerPlayerEntity player) {
        Item gemItem = ModItems.gemItem(GemPlayerState.getActiveGem(player));
        if (hasItem(player, gemItem)) {
            return;
        }
        player.giveItemStack(new ItemStack(gemItem));
    }

    private static boolean hasItem(ServerPlayerEntity player, Item item) {
        for (ItemStack stack : player.getInventory().main) {
            if (stack.isOf(item)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offHand) {
            if (stack.isOf(item)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (stack.isOf(item)) {
                return true;
            }
        }
        return false;
    }
}
