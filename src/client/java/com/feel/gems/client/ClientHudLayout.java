package com.feel.gems.client;

import com.feel.gems.loadout.GemLoadout;

public final class ClientHudLayout {
    private static GemLoadout.HudLayout layout = GemLoadout.HudLayout.defaults();

    private ClientHudLayout() {
    }

    public static void update(GemLoadout.HudLayout next) {
        if (next == null) {
            layout = GemLoadout.HudLayout.defaults();
            return;
        }
        layout = next;
    }

    public static GemLoadout.HudLayout get() {
        return layout;
    }

    public static void reset() {
        layout = GemLoadout.HudLayout.defaults();
    }
}
