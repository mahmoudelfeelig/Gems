package com.feel.gems.net;

import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.power.runtime.GemAbilityCooldowns;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.util.GemsTime;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;




public final class GemCooldownSync {
    private GemCooldownSync() {
    }

    public static void send(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);

        GemId active = GemPlayerState.getActiveGem(player);
        GemDefinition def = GemRegistry.definition(active);
        List<Identifier> abilities = def.abilities();

        long now = GemsTime.now(player);
        List<Integer> remaining = new ArrayList<>(abilities.size());
        for (int i = 0; i < abilities.size(); i++) {
            Identifier id = abilities.get(i);
            remaining.add(GemAbilityCooldowns.remainingTicks(player, id, now));
        }

        ServerPlayNetworking.send(player, new CooldownSnapshotPayload(active.ordinal(), remaining));
    }
}
