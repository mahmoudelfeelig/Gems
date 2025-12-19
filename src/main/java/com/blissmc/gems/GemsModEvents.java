package com.blissmc.gems;

import com.blissmc.gems.state.GemPlayerState;
import com.blissmc.gems.item.ModItems;
import com.blissmc.gems.item.GemItemGlint;
import com.blissmc.gems.net.GemStateSync;
import com.blissmc.gems.power.FluxCharge;
import com.blissmc.gems.power.AbilityRuntime;
import com.blissmc.gems.power.GemPowers;
import com.blissmc.gems.power.SoulSystem;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import com.blissmc.gems.core.GemId;

public final class GemsModEvents {
    private static int tickCounter = 0;

    private GemsModEvents() {
    }

    public static void register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            if (entity instanceof ServerPlayerEntity player && killedEntity instanceof net.minecraft.entity.LivingEntity living) {
                SoulSystem.onKilledMob(player, living);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            GemPlayerState.initIfNeeded(player);
            GemPlayerState.applyMaxHearts(player);
            ensureActiveGemItem(player);
            GemPowers.sync(player);
            GemItemGlint.sync(player);
            GemStateSync.send(player);
        });

        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            GemPlayerState.copy(oldPlayer, newPlayer);
            GemPlayerState.initIfNeeded(newPlayer);
            GemPlayerState.applyMaxHearts(newPlayer);
            ensureActiveGemItem(newPlayer);
            GemPowers.sync(newPlayer);
            GemItemGlint.sync(newPlayer);
            GemStateSync.send(newPlayer);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            boolean doMaintain = tickCounter % 40 == 0;
            boolean doFluxOvercharge = tickCounter % 20 == 0;
            if (!doMaintain && !doFluxOvercharge) {
                return;
            }

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                if (doMaintain) {
                    GemPowers.maintain(player);
                }
                if (doFluxOvercharge) {
                    GemPlayerState.initIfNeeded(player);
                    if (GemPlayerState.getActiveGem(player) == GemId.FLUX && GemPlayerState.getEnergy(player) > 0) {
                        FluxCharge.tickOvercharge(player);
                    }
                    AbilityRuntime.tickEverySecond(player);
                }
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
