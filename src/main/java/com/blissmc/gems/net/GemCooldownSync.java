package com.blissmc.gems.net;

import com.blissmc.gems.core.GemDefinition;
import com.blissmc.gems.core.GemId;
import com.blissmc.gems.core.GemRegistry;
import com.blissmc.gems.power.GemAbilityCooldowns;
import com.blissmc.gems.state.GemPlayerState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class GemCooldownSync {
    private GemCooldownSync() {
    }

    public static void send(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);

        GemId active = GemPlayerState.getActiveGem(player);
        GemDefinition def = GemRegistry.definition(active);
        List<Identifier> abilities = def.abilities();

        long now = player.getServerWorld().getTime();
        List<Integer> remaining = new ArrayList<>(abilities.size());
        for (int i = 0; i < abilities.size(); i++) {
            Identifier id = abilities.get(i);
            remaining.add(GemAbilityCooldowns.remainingTicks(player, id, now));
        }

        ServerPlayNetworking.send(player, new CooldownSnapshotPayload(active.ordinal(), remaining));
    }
}
