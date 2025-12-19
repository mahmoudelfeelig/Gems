package com.blissmc.gems.client;

import net.fabricmc.api.ClientModInitializer;

public final class GemsClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientNetworking.register();
        GemsKeybinds.register();
        com.blissmc.gems.client.hud.GemsHud.register();
        com.blissmc.gems.client.hud.GemsTooltips.register();
    }
}
