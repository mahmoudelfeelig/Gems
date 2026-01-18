package com.feel.gems.net;

import com.feel.gems.item.legendary.HuntersTrophyNecklaceItem;
import com.feel.gems.power.util.Targeting;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Server-side networking for the Trophy Necklace passive steal UI.
 */
public final class ServerTrophyNecklaceNetworking {
    private static final ConcurrentHashMap<UUID, Set<Identifier>> OPEN_SESSIONS = new ConcurrentHashMap<>();

    private ServerTrophyNecklaceNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(TrophyNecklaceOpenRequestPayload.ID, (payload, context) ->
            context.player().getEntityWorld().getServer().execute(() -> handleOpen(context.player())));

        ServerPlayNetworking.registerGlobalReceiver(TrophyNecklaceClaimPayload.ID, (payload, context) ->
                context.player().getEntityWorld().getServer().execute(() -> handleClaim(context.player(), payload)));
    }

    public static void setSession(ServerPlayerEntity player, Set<Identifier> offeredPassives) {
        if (player == null) {
            return;
        }
        if (offeredPassives == null || offeredPassives.isEmpty()) {
            OPEN_SESSIONS.remove(player.getUuid());
            return;
        }
        OPEN_SESSIONS.put(player.getUuid(), Set.copyOf(offeredPassives));
    }

    private static boolean isOffered(ServerPlayerEntity player, Identifier passiveId) {
        Set<Identifier> offered = OPEN_SESSIONS.get(player.getUuid());
        return offered != null && offered.contains(passiveId);
    }

    private static void handleClaim(ServerPlayerEntity player, TrophyNecklaceClaimPayload payload) {
        if (player == null || payload == null) {
            return;
        }
        if (!HuntersTrophyNecklaceItem.hasNecklace(player)) {
            player.sendMessage(Text.translatable("gems.item.trophy_necklace.need_necklace"), true);
            return;
        }
        Identifier passiveId = payload.passiveId();
        if (passiveId == null) {
            return;
        }
        if (!isOffered(player, passiveId) && !HuntersTrophyNecklaceItem.wasLastOffered(player, passiveId)) {
            return;
        }

        boolean ok;
        if (payload.steal()) {
            UUID source = HuntersTrophyNecklaceItem.getLastTargetUuid(player);
            ok = HuntersTrophyNecklaceItem.stealPassiveFrom(player, passiveId, source);
        } else {
            ok = HuntersTrophyNecklaceItem.unstealPassive(player, passiveId);
        }
        if (ok) {
            com.feel.gems.power.runtime.GemPowers.sync(player);
        }

        // Re-open the UI with the same last target, if possible.
        HuntersTrophyNecklaceItem.openLastTargetScreen(player);
    }

    private static void handleOpen(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        if (!HuntersTrophyNecklaceItem.hasNecklace(player)) {
            player.sendMessage(Text.translatable("gems.item.trophy_necklace.need_necklace"), true);
            return;
        }
        ServerPlayerEntity target = Targeting.raycastPlayer(player, 15.0D);
        if (target != null && target != player) {
            HuntersTrophyNecklaceItem.openScreenForTarget(player, target);
            return;
        }
        boolean opened = HuntersTrophyNecklaceItem.openLastTargetScreen(player);
        if (!opened) {
            player.sendMessage(Text.translatable("gems.item.trophy_necklace.no_session"), true);
        }
    }
}
