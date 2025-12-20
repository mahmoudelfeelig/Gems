package com.feel.gems.screen;

import com.feel.gems.GemsMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureSet;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public final class ModScreenHandlers {
    public static final ScreenHandlerType<TraderScreenHandler> TRADER = Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of(GemsMod.MOD_ID, "trader"),
            new ScreenHandlerType<>(TraderScreenHandler::new, FeatureSet.empty())
    );

    private ModScreenHandlers() {
    }

    public static void init() {
        // Triggers static init.
    }
}

