package com.feel.gems.screen;

import com.feel.gems.GemsMod;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;




public final class ModScreenHandlers {
    public static final ScreenHandlerType<TraderScreenHandler> TRADER = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(GemsMod.MOD_ID, "gem_trader"),
            new ScreenHandlerType<>(TraderScreenHandler::new, FeatureSet.empty())
    );

    public static final ExtendedScreenHandlerType<GemSeerScreenHandler, GemSeerScreenHandler.OpeningData> GEM_SEER = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(GemsMod.MOD_ID, "gem_seer"),
            new ExtendedScreenHandlerType<>(GemSeerScreenHandler::new, GemSeerScreenHandler.OpeningData.PACKET_CODEC)
    );

    private ModScreenHandlers() {
    }

    public static void init() {
        // Triggers static init.
    }
}
