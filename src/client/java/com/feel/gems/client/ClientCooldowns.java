package com.feel.gems.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.feel.gems.core.GemDefinition;
import com.feel.gems.core.GemId;
import com.feel.gems.core.GemRegistry;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;




public final class ClientCooldowns {
    private static final Map<Identifier, Long> END_TICKS = new HashMap<>();
    private static final Map<Identifier, Integer> LAST_COOLDOWN_TICKS = new HashMap<>();
    private static GemId activeGem = null;
    private static Identifier lastUsedAbility = null;

    private ClientCooldowns() {
    }

    public static void reset() {
        activeGem = null;
        END_TICKS.clear();
        LAST_COOLDOWN_TICKS.clear();
        lastUsedAbility = null;
    }

    public static void clearIfGemChanged(GemId gem) {
        if (activeGem == gem) {
            return;
        }
        activeGem = gem;
        END_TICKS.clear();
        LAST_COOLDOWN_TICKS.clear();
        lastUsedAbility = null;
    }

    public static void applySnapshot(GemId gem, java.util.List<Integer> remainingAbilityCooldownTicks) {
        clearIfGemChanged(gem);
        END_TICKS.clear();

        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null) {
            return;
        }
        long now = world.getTime();

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
        ClientWorld world = client.world;
        if (world == null) {
            return;
        }
        long now = world.getTime();

        GemDefinition def = GemRegistry.definition(gem);
        List<Identifier> abilities = def.abilities();
        if (abilityIndex < 0 || abilityIndex >= abilities.size()) {
            return;
        }
        Identifier abilityId = abilities.get(abilityIndex);
        END_TICKS.put(abilityId, now + cooldownTicks);
        LAST_COOLDOWN_TICKS.put(abilityId, cooldownTicks);
        lastUsedAbility = abilityId;
    }

    public static int lastCooldownTicks(GemId gem, Identifier abilityId) {
        if (activeGem != gem) {
            return 0;
        }
        return LAST_COOLDOWN_TICKS.getOrDefault(abilityId, 0);
    }

    public static int remainingTicks(GemId gem, Identifier abilityId) {
        if (activeGem != gem) {
            return 0;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null) {
            return 0;
        }
        Long end = END_TICKS.get(abilityId);
        if (end == null) {
            return 0;
        }
        long remaining = end - world.getTime();
        if (remaining <= 0) {
            return 0;
        }
        if (remaining > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) remaining;
    }

    public static boolean isLastUsed(GemId gem, Identifier abilityId) {
        return activeGem == gem && abilityId != null && abilityId.equals(lastUsedAbility);
    }
}
