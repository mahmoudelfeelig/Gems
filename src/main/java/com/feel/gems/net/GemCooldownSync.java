package com.feel.gems.net;

import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;
import com.feel.gems.legendary.LegendaryCooldowns;
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
        
        // Apply Chrono Charm multiplier to remaining cooldown display.
        // Chrono Charms make cooldowns tick faster, so the effective remaining time is shorter.
        float chronoMultiplier = LegendaryCooldowns.getCooldownMultiplier(player);
        
        List<Integer> remaining = new ArrayList<>(abilities.size());
        for (int i = 0; i < abilities.size(); i++) {
            Identifier id = abilities.get(i);
            int rawRemaining = GemAbilityCooldowns.remainingTicks(player, id, now);
            // Apply chrono multiplier: if multiplier is 0.5 (2 charms), cooldowns tick 2x faster
            // so effective remaining time is raw * multiplier
            int effectiveRemaining = (int) Math.ceil(rawRemaining * chronoMultiplier);
            remaining.add(effectiveRemaining);
        }

        ServerPlayNetworking.send(player, new CooldownSnapshotPayload(active.ordinal(), remaining));
    }
}
