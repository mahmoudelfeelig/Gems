package com.feel.gems.power.ability.duelist;

import com.feel.gems.net.SpySkinshiftPayload;
import com.feel.gems.state.PlayerStateManager;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

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

        // Remove the barrier cage before clearing state
        removeCageIfPresent(player);

        DuelistMirrorMatchAbility.clearDuel(player);
        syncDisguise(player, null);

        // Ensure the partner's disguise is also cleared for all viewers, even if the partner is offline.
        if (server != null && partner != null) {
            ServerPlayerEntity partnerPlayer = server.getPlayerManager().getPlayer(partner);
            if (partnerPlayer != null) {
                // Note: We don't call removeCageIfPresent here because both players share the same cage
                // and we already removed it above
                DuelistMirrorMatchAbility.clearDuel(partnerPlayer);
                syncDisguise(partnerPlayer, null);
            } else {
                clearDisguiseForAllViewers(server, partner);
            }
        }
    }

    /**
     * Remove the barrier cage if the player is in a duel with a cage.
     */
    private static void removeCageIfPresent(ServerPlayerEntity player) {
        BlockPos cageCenter = DuelistMirrorMatchAbility.getCageCenter(player);
        if (cageCenter != null && player.getEntityWorld() instanceof ServerWorld world) {
            DuelistMirrorMatchAbility.removeCage(world, cageCenter);
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
