package com.feel.gems.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;




public final class GemsClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientNetworking.register();
        GemsKeybinds.register();
        com.feel.gems.client.hud.GemsHud.register();
        com.feel.gems.client.hud.GemsTooltips.register();
        HandledScreens.register(com.feel.gems.screen.ModScreenHandlers.TRADER, com.feel.gems.client.screen.TraderScreen::new);
    }
}
