package com.feel.gems.power.ability.duelist;

import com.feel.gems.net.SpySkinshiftPayload;
import com.feel.gems.state.PlayerStateManager;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public final class DuelistMirrorMatchRuntime {
    private DuelistMirrorMatchRuntime() {
    }

    public static void start(ServerPlayerEntity caster, ServerPlayerEntity target) {
        if (caster == null || target == null) {
            return;
        }
        // Swap appearances for the duration.
        syncDisguise(caster, target.getUuid());
        syncDisguise(target, caster.getUuid());
    }

    public static void tick(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        if (!DuelistMirrorMatchAbility.isInDuel(player)) {
            clear(player);
            return;
        }
    }

    public static void clear(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        MinecraftServer server = player.getEntityWorld().getServer();
        UUID partner = partnerUuid(player);

        DuelistMirrorMatchAbility.clearDuel(player);
        syncDisguise(player, null);

        // Ensure the partner's disguise is also cleared for all viewers, even if the partner is offline.
        if (server != null && partner != null) {
            ServerPlayerEntity partnerPlayer = server.getPlayerManager().getPlayer(partner);
            if (partnerPlayer != null) {
                DuelistMirrorMatchAbility.clearDuel(partnerPlayer);
                syncDisguise(partnerPlayer, null);
            } else {
                clearDisguiseForAllViewers(server, partner);
            }
        }
    }

    private static void syncDisguise(ServerPlayerEntity player, java.util.UUID targetUuid) {
        if (player.getEntityWorld().getServer() == null) {
            return;
        }
        SpySkinshiftPayload payload = new SpySkinshiftPayload(player.getUuid(), Optional.ofNullable(targetUuid));
        for (ServerPlayerEntity viewer : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(viewer, payload);
        }
    }

    private static UUID partnerUuid(ServerPlayerEntity player) {
        String partnerStr = PlayerStateManager.getPersistent(player, DuelistMirrorMatchAbility.DUEL_PARTNER_KEY);
        if (partnerStr == null || partnerStr.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(partnerStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static void clearDisguiseForAllViewers(MinecraftServer server, UUID playerId) {
        SpySkinshiftPayload payload = new SpySkinshiftPayload(playerId, Optional.empty());
        for (ServerPlayerEntity viewer : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(viewer, payload);
        }
    }
}
