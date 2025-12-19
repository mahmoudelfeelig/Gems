package com.blissmc.gems.client;

import com.blissmc.gems.core.GemId;

public final class ClientGemState {
    private static boolean initialized = false;
    private static GemId activeGem = GemId.ASTRA;
    private static int energy = 0;
    private static int maxHearts = 10;

    private ClientGemState() {
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static GemId activeGem() {
        return activeGem;
    }

    public static int energy() {
        return energy;
    }

    public static int maxHearts() {
        return maxHearts;
    }

    public static void update(GemId gem, int energy, int maxHearts) {
        ClientGemState.activeGem = gem;
        ClientGemState.energy = energy;
        ClientGemState.maxHearts = maxHearts;
        ClientGemState.initialized = true;
    }
}

