package com.feel.gems.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.SkinTextures;


public final class ClientDisguiseState {
    private static final Map<UUID, UUID> DISGUISES = new HashMap<>();

    private ClientDisguiseState() {
    }

    public static void update(UUID player, Optional<UUID> target) {
        if (player == null) {
            return;
        }
        if (target == null || target.isEmpty()) {
            DISGUISES.remove(player);
            return;
        }
        DISGUISES.put(player, target.get());
    }

    public static SkinTextures overrideSkin(AbstractClientPlayerEntity player) {
        UUID target = DISGUISES.get(player.getUuid());
        if (target == null) {
            return null;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) {
            return null;
        }
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(target);
        if (entry == null) {
            return null;
        }
        return entry.getSkinTextures();
    }

    public static void reset() {
        DISGUISES.clear();
    }
}
