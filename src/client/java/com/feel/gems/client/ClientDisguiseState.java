package com.feel.gems.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;


public final class ClientDisguiseState {
    private static final Map<UUID, UUID> DISGUISES = new HashMap<>();

    private ClientDisguiseState() {
    }

    private static UUID removeDisguise(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return DISGUISES.remove(playerId);
    }

    private static void restoreDisguise(UUID playerId, UUID target) {
        if (playerId == null || target == null) {
            return;
        }
        DISGUISES.put(playerId, target);
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
        if (player == null) {
            return null;
        }
        return overrideSkin(player.getUuid());
    }

    public static SkinTextures overrideSkin(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        UUID target = DISGUISES.get(playerId);
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

        // Avoid recursion if both players are disguised as each other (e.g., Mirror Match).
        UUID removed = removeDisguise(target);
        try {
            return entry.getSkinTextures();
        } finally {
            restoreDisguise(target, removed);
        }
    }

    public static Text overrideName(AbstractClientPlayerEntity player) {
        if (player == null) {
            return null;
        }
        return overrideName(player.getUuid());
    }

    public static Text overrideName(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        UUID target = DISGUISES.get(playerId);
        if (target == null) {
            return null;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() == null) {
            return null;
        }
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(target);
        if (entry == null || entry.getProfile() == null) {
            return null;
        }
        return Text.literal(entry.getProfile().name());
    }

    public static void reset() {
        DISGUISES.clear();
    }
}
