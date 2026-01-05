package com.feel.gems.client;

import com.feel.gems.net.ChaosSlotPayload;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Client-side state for Chaos gem's 4 ability slots.
 */
public final class ClientChaosState {
    public static final int SLOT_COUNT = 4;
    
    public record SlotState(
        String abilityName,
        Identifier abilityId,
        String passiveName,
        int remainingSeconds,
        int cooldownSeconds
    ) {
        public static SlotState inactive() {
            return new SlotState("", null, "", 0, 0);
        }
        
        public boolean isActive() {
            return abilityId != null && remainingSeconds > 0;
        }
    }
    
    private static final List<SlotState> slots = new ArrayList<>();
    
    static {
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots.add(SlotState.inactive());
        }
    }

    private ClientChaosState() {
    }

    public static SlotState getSlot(int index) {
        if (index < 0 || index >= SLOT_COUNT) return SlotState.inactive();
        return slots.get(index);
    }

    public static void update(ChaosSlotPayload payload) {
        List<ChaosSlotPayload.SlotData> data = payload.slots();
        for (int i = 0; i < SLOT_COUNT && i < data.size(); i++) {
            ChaosSlotPayload.SlotData d = data.get(i);
            Identifier abilityId = (d.abilityId() == null || d.abilityId().isEmpty()) 
                ? null : Identifier.tryParse(d.abilityId());
            slots.set(i, new SlotState(
                d.abilityName(),
                abilityId,
                d.passiveName(),
                d.remainingSeconds(),
                d.cooldownSeconds()
            ));
        }
    }

    public static void reset() {
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots.set(i, SlotState.inactive());
        }
    }

    public static boolean hasAnyActiveSlot() {
        for (SlotState slot : slots) {
            if (slot.isActive()) return true;
        }
        return false;
    }
    
    // Legacy methods for compatibility
    public static String abilityName() {
        SlotState slot = getSlot(0);
        return slot.abilityName;
    }

    public static Identifier abilityId() {
        SlotState slot = getSlot(0);
        return slot.abilityId;
    }

    public static String passiveName() {
        SlotState slot = getSlot(0);
        return slot.passiveName;
    }

    public static int rotationSecondsRemaining() {
        SlotState slot = getSlot(0);
        return slot.remainingSeconds;
    }

    public static boolean hasState() {
        return hasAnyActiveSlot();
    }
}
