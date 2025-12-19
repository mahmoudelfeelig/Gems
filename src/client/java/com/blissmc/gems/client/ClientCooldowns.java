package com.blissmc.gems.client;

import com.blissmc.gems.core.GemDefinition;
import com.blissmc.gems.core.GemId;
import com.blissmc.gems.core.GemRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientCooldowns {
    private static final Map<Identifier, Long> END_TICKS = new HashMap<>();
    private static GemId activeGem = null;

    private ClientCooldowns() {
    }

    public static void reset() {
        activeGem = null;
        END_TICKS.clear();
    }

    public static void clearIfGemChanged(GemId gem) {
        if (activeGem == gem) {
            return;
        }
        activeGem = gem;
        END_TICKS.clear();
    }

    public static void applySnapshot(GemId gem, java.util.List<Integer> remainingAbilityCooldownTicks) {
        clearIfGemChanged(gem);
        END_TICKS.clear();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        long now = client.world.getTime();

        GemDefinition def = GemRegistry.definition(gem);
        List<Identifier> abilities = def.abilities();
        int n = Math.min(abilities.size(), remainingAbilityCooldownTicks.size());
        for (int i = 0; i < n; i++) {
            int remaining = remainingAbilityCooldownTicks.get(i);
            if (remaining <= 0) {
                continue;
            }
            END_TICKS.put(abilities.get(i), now + remaining);
        }
    }

    public static void setCooldown(GemId gem, int abilityIndex, int cooldownTicks) {
        clearIfGemChanged(gem);
        if (cooldownTicks <= 0) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        long now = client.world.getTime();

        GemDefinition def = GemRegistry.definition(gem);
        List<Identifier> abilities = def.abilities();
        if (abilityIndex < 0 || abilityIndex >= abilities.size()) {
            return;
        }
        END_TICKS.put(abilities.get(abilityIndex), now + cooldownTicks);
    }

    public static int remainingTicks(GemId gem, Identifier abilityId) {
        if (activeGem != gem) {
            return 0;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return 0;
        }
        Long end = END_TICKS.get(abilityId);
        if (end == null) {
            return 0;
        }
        long remaining = end - client.world.getTime();
        if (remaining <= 0) {
            return 0;
        }
        if (remaining > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) remaining;
    }
}
