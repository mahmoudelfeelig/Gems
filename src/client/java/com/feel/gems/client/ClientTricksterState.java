package com.feel.gems.client;

public final class ClientTricksterState {
    private static boolean puppeted;
    private static boolean mindGames;

    private ClientTricksterState() {
    }

    public static void update(boolean newPuppeted, boolean newMindGames) {
        puppeted = newPuppeted;
        mindGames = newMindGames;
    }

    public static boolean isPuppeted() {
        return puppeted;
    }

    public static boolean isMindGames() {
        return mindGames;
    }

    public static void reset() {
        puppeted = false;
        mindGames = false;
    }
}
