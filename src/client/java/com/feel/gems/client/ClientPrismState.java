package com.feel.gems.client;

import com.feel.gems.net.PrismAbilitiesSyncPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;

/**
 * Client-side state for Prism gem abilities (HUD display and cooldown tracking).
 */
public final class ClientPrismState {
    private static final List<PrismAbilityEntry> ABILITIES = new ArrayList<>();
    private static final Map<Identifier, Long> COOLDOWN_END_TICKS = new HashMap<>();
    private static final Map<Identifier, Integer> LAST_COOLDOWN_TICKS = new HashMap<>();
    private static Identifier lastUsedPrism = null;

    private ClientPrismState() {
    }

    public record PrismAbilityEntry(Identifier id, String name) {}

    public static void reset() {
        ABILITIES.clear();
        COOLDOWN_END_TICKS.clear();
        LAST_COOLDOWN_TICKS.clear();
        lastUsedPrism = null;
    }

    public static void update(PrismAbilitiesSyncPayload payload) {
        ABILITIES.clear();
        COOLDOWN_END_TICKS.clear();
        
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        long now = world != null ? world.getTime() : 0;
        
        for (PrismAbilitiesSyncPayload.PrismAbilityInfo info : payload.abilities()) {
            ABILITIES.add(new PrismAbilityEntry(info.id(), info.name()));
            if (info.remainingCooldownTicks() > 0) {
                COOLDOWN_END_TICKS.put(info.id(), now + info.remainingCooldownTicks());
            }
        }
    }

    public static void setCooldown(int slotIndex, int cooldownTicks) {
        if (slotIndex < 0 || slotIndex >= ABILITIES.size()) {
            return;
        }
        Identifier id = ABILITIES.get(slotIndex).id();
        
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null || cooldownTicks <= 0) {
            return;
        }
        long now = world.getTime();
        COOLDOWN_END_TICKS.put(id, now + cooldownTicks);
        LAST_COOLDOWN_TICKS.put(id, cooldownTicks);
        lastUsedPrism = id;
    }

    public static int lastCooldownTicks(Identifier abilityId) {
        return LAST_COOLDOWN_TICKS.getOrDefault(abilityId, 0);
    }

    public static List<PrismAbilityEntry> getAbilities() {
        return List.copyOf(ABILITIES);
    }

    public static int remainingTicks(Identifier abilityId) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null) {
            return 0;
        }
        Long end = COOLDOWN_END_TICKS.get(abilityId);
        if (end == null) {
            return 0;
        }
        long remaining = end - world.getTime();
        if (remaining <= 0) {
            return 0;
        }
        return (int) remaining;
    }

    public static boolean isLastUsed(Identifier abilityId) {
        return abilityId != null && abilityId.equals(lastUsedPrism);
    }
}
