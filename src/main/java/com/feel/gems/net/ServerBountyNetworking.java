package com.feel.gems.net;

import com.feel.gems.bounty.BountyBoard;
import com.feel.gems.bounty.BountyBoard.BountyListEntry;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class ServerBountyNetworking {
    private ServerBountyNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(BountyBoardOpenRequestPayload.ID, (payload, context) ->
                context.server().execute(() -> open(context.player()))
        );

        ServerPlayNetworking.registerGlobalReceiver(BountyPlacePayload.ID, (payload, context) ->
                context.server().execute(() -> handlePlace(context.player(), payload))
        );
    }

    public static void open(ServerPlayerEntity player) {
        if (player == null) {
            return;
        }
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return;
        }
        List<BountyBoardPayload.Entry> bounties = BountyBoard.listEntries(server).stream()
                .sorted(Comparator.comparing((BountyListEntry e) -> e.targetName().toLowerCase(Locale.ROOT))
                        .thenComparing(e -> e.placerName().toLowerCase(Locale.ROOT)))
                .map(entry -> new BountyBoardPayload.Entry(
                        entry.targetId(),
                        entry.targetName(),
                        entry.placerId(),
                        entry.placerName(),
                        entry.hearts(),
                        entry.energy()
                ))
                .toList();

        List<BountyBoardPayload.PlayerEntry> players = server.getPlayerManager().getPlayerList().stream()
                .filter(p -> !p.getUuid().equals(player.getUuid()))
                .sorted(Comparator.comparing(p -> p.getName().getString().toLowerCase(Locale.ROOT)))
                .map(p -> new BountyBoardPayload.PlayerEntry(p.getUuid(), p.getName().getString()))
                .toList();

        int maxHearts = BountyBoard.maxAdditionalHearts(player);
        int maxEnergy = BountyBoard.maxAdditionalEnergy(player);

        ServerPlayNetworking.send(player, new BountyBoardPayload(player.getUuid(), maxHearts, maxEnergy, bounties, players));
    }

    private static void handlePlace(ServerPlayerEntity placer, BountyPlacePayload payload) {
        if (placer == null) {
            return;
        }
        MinecraftServer server = placer.getEntityWorld().getServer();
        if (server == null) {
            return;
        }
        ServerPlayerEntity target = server.getPlayerManager().getPlayer(payload.targetId());
        if (target == null) {
            placer.sendMessage(Text.translatable("gems.bounty.error.target_offline"), true);
            return;
        }
        BountyBoard.PlaceResult result = BountyBoard.placeBounty(placer, target, payload.hearts(), payload.energy());
        if (result == BountyBoard.PlaceResult.SUCCESS) {
            placer.sendMessage(Text.translatable("gems.bounty.place.success", target.getName().getString(), payload.hearts(), payload.energy()), true);
            target.sendMessage(Text.translatable("gems.bounty.placed_on_you", placer.getName().getString(), payload.hearts(), payload.energy()), false);
            return;
        }
        placer.sendMessage(messageFor(result), true);
    }

    private static Text messageFor(BountyBoard.PlaceResult result) {
        return switch (result) {
            case ASSASSIN_BLOCKED -> Text.translatable("gems.bounty.error.assassin_blocked");
            case INVALID_TARGET -> Text.translatable("gems.bounty.error.invalid_target");
            case INVALID_AMOUNT -> Text.translatable("gems.bounty.error.invalid_amount");
            case NOT_HIGHER -> Text.translatable("gems.bounty.error.not_higher");
            case NO_CHANGE -> Text.translatable("gems.bounty.error.no_change");
            case INSUFFICIENT_HEARTS -> Text.translatable("gems.bounty.error.insufficient_hearts");
            case INSUFFICIENT_ENERGY -> Text.translatable("gems.bounty.error.insufficient_energy");
            default -> Text.translatable("gems.bounty.error.unknown");
        };
    }
}
