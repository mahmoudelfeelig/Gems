package com.feel.gems.client;

import com.feel.gems.client.entity.HunterPackEntityRenderer;
import com.feel.gems.client.entity.ShadowCloneEntityRenderer;
import com.feel.gems.entity.ModEntities;
import com.feel.gems.net.ClientPassiveTogglePayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;




public final class GemsClientMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientNetworking.register();
        GemsClientConfigManager.loadOrCreate();
        GemsKeybinds.register();
        ClientTickEvents.END_CLIENT_TICK.register(GemsKeybinds::tick);
        com.feel.gems.client.hud.GemsHud.register();
        com.feel.gems.client.hud.GemsTooltips.register();
        HandledScreens.register(com.feel.gems.screen.ModScreenHandlers.TRADER, com.feel.gems.client.screen.TraderScreen::new);
        HandledScreens.register(com.feel.gems.screen.ModScreenHandlers.GEM_SEER, com.feel.gems.client.screen.GemSeerScreen::new);
        
        // Register entity renderers
        EntityRendererRegistry.register(ModEntities.SHADOW_CLONE, ShadowCloneEntityRenderer::new);
        EntityRendererRegistry.register(ModEntities.HUNTER_PACK, HunterPackEntityRenderer::new);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            GemsClientConfig cfg = GemsClientConfigManager.config();
            ClientPlayNetworking.send(new ClientPassiveTogglePayload(cfg.passivesEnabled));
        });
    }
}
