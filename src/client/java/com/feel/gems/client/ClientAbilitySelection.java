package com.feel.gems.client;

import com.feel.gems.core.GemId;




public final class ClientAbilitySelection {
    private static GemId gem = null;
    private static int slotNumber = 0;

    private ClientAbilitySelection() {
    }

    public static void record(GemId gem, int slotNumber) {
        ClientAbilitySelection.gem = gem;
        ClientAbilitySelection.slotNumber = slotNumber;
    }

    public static int slotNumber(GemId gem) {
        return ClientAbilitySelection.gem == gem ? slotNumber : 0;
    }

    public static void reset() {
        gem = null;
        slotNumber = 0;
    }
}

