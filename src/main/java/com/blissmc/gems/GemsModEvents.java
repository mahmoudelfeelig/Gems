package com.feel.gems;

import com.feel.gems.state.GemPlayerState;
import com.feel.gems.item.ModItems;
import com.feel.gems.item.GemItemGlint;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.power.FluxCharge;
import com.feel.gems.power.AbilityRuntime;
import com.feel.gems.power.BreezyBashTracker;
import com.feel.gems.power.GemPowers;
import com.feel.gems.power.SoulSystem;
import com.feel.gems.power.AutoSmeltCache;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.debug.GemsStressTest;
import com.feel.gems.debug.GemsPerfMonitor;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import com.feel.gems.core.GemId;

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

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            AbilityRuntime.cleanupOnDisconnect(server, player);
            GemTrust.clearRuntimeCache(player.getUuid());
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

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> AutoSmeltCache.clear());

        ServerTickEvents.START_SERVER_TICK.register(server -> GemsPerfMonitor.onTickStart());

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

        ServerTickEvents.END_SERVER_TICK.register(BreezyBashTracker::tick);
        ServerTickEvents.END_SERVER_TICK.register(GemsStressTest::tick);
        ServerTickEvents.END_SERVER_TICK.register(server -> GemsPerfMonitor.onTickEnd());
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
