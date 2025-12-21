package com.feel.gems.client;

public final class ClientExtraState {
    private static int fluxChargePercent = 0;
    private static boolean hasSoul = false;
    private static String soulTypeId = "";

    private ClientExtraState() {
    }

    public static int fluxChargePercent() {
        return fluxChargePercent;
    }

    public static boolean hasSoul() {
        return hasSoul;
    }

    public static String soulTypeId() {
        return soulTypeId;
    }

    public static void update(int fluxChargePercent, boolean hasSoul, String soulTypeId) {
        ClientExtraState.fluxChargePercent = Math.max(0, Math.min(200, fluxChargePercent));
        ClientExtraState.hasSoul = hasSoul;
        ClientExtraState.soulTypeId = soulTypeId == null ? "" : soulTypeId;
    }

    public static void reset() {
        fluxChargePercent = 0;
        hasSoul = false;
        soulTypeId = "";
    }
}

