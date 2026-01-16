package com.feel.gems.client;

/**
 * Client-side state for the Rivalry system (HUD display).
 */
public final class ClientRivalryState {
    private static String rivalTarget = "";

    private ClientRivalryState() {
    }

    public static void setRivalTarget(String name) {
        rivalTarget = name != null ? name : "";
    }

    public static String getRivalTarget() {
        return rivalTarget;
    }

    public static boolean hasRivalTarget() {
        return !rivalTarget.isEmpty();
    }

    public static void clear() {
        rivalTarget = "";
    }
}
