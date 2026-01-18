package com.feel.gems.net;

import com.feel.gems.config.GemsDisables;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.spy.SpySystem;
import com.feel.gems.power.registry.ModAbilities;
import com.feel.gems.state.GemPlayerState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Server-side networking for Spy observed ability selection.
 */
public final class ServerSpyObservedNetworking {
    private ServerSpyObservedNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(SpyObservedOpenRequestPayload.ID, (payload, context) ->
                context.server().execute(() -> openScreen(context.player())));

        ServerPlayNetworking.registerGlobalReceiver(SpyObservedSelectPayload.ID, (payload, context) ->
                context.server().execute(() -> handleSelect(context.player(), payload.abilityId())));
    }

    private static void openScreen(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);
        if (!SpySystem.isSpyActive(player)) {
            player.sendMessage(Text.translatable("gems.spy.observed.not_spy"), true);
            return;
        }
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return;
        }

        List<SpyObservedScreenPayload.ObservedEntry> entries = new ArrayList<>();
        for (SpySystem.ObservedAbility abilityInfo : SpySystem.observedAbilities(player)) {
            Identifier id = abilityInfo.id();
            if (id == null || GemsDisables.isAbilityDisabled(id)) {
                continue;
            }
            GemAbility ability = ModAbilities.get(id);
            if (ability == null) {
                continue;
            }
            boolean canSteal = SpySystem.canSteal(player, id);
            boolean canEcho = SpySystem.canEcho(player, id);
            entries.add(new SpyObservedScreenPayload.ObservedEntry(
                    id,
                    ability.name(),
                    abilityInfo.count(),
                    canEcho,
                    canSteal
            ));
        }

        entries.sort(Comparator.comparing(SpyObservedScreenPayload.ObservedEntry::name));
        Identifier selected = SpySystem.selectedObservedAbility(player);
        ServerPlayNetworking.send(player, new SpyObservedScreenPayload(entries, selected));
    }

    private static void handleSelect(ServerPlayerEntity player, Identifier abilityId) {
        if (abilityId == null) {
            return;
        }
        if (!SpySystem.selectObservedAbility(player, abilityId)) {
            player.sendMessage(Text.translatable("gems.spy.observed.select_failed"), true);
            return;
        }
        openScreen(player);
    }
}
