package com.feel.gems.client;

import com.feel.gems.core.GemId;
import net.minecraft.client.MinecraftClient;




public final class ClientAbilitySelection {
    private static GemId gem = null;
    private static int slotNumber = 0;
    private static long selectedAtTick = 0;

    private ClientAbilitySelection() {
    }

    public static void record(GemId gem, int slotNumber) {
        ClientAbilitySelection.gem = gem;
        ClientAbilitySelection.slotNumber = slotNumber;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            ClientAbilitySelection.selectedAtTick = client.world.getTime();
        }
    }

    public static int slotNumber(GemId gem) {
        return ClientAbilitySelection.gem == gem ? slotNumber : 0;
    }

    public static void reset() {
        gem = null;
        slotNumber = 0;
        selectedAtTick = 0;
    }
}

