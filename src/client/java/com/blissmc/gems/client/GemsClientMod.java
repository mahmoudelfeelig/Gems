package com.feel.gems.client;

import net.fabricmc.api.ClientModInitializer;

public final class GemsClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientNetworking.register();
        GemsKeybinds.register();
        com.feel.gems.client.hud.GemsHud.register();
        com.feel.gems.client.hud.GemsTooltips.register();
    }
}
