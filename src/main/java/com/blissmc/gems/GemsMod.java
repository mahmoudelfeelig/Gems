package com.feel.gems;

import com.feel.gems.config.GemsBalance;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feel.gems.command.GemsCommands;
import com.feel.gems.item.ModItems;
import com.feel.gems.net.GemsPayloads;
import com.feel.gems.net.ServerAbilityNetworking;
import com.feel.gems.screen.ModScreenHandlers;

public final class GemsMod implements ModInitializer {
    public static final String MOD_ID = "gems";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Gems mod");
        GemsBalance.init();
        GemsPayloads.register();
        ModScreenHandlers.init();
        ModItems.init();
        ServerAbilityNetworking.register();
        GemsModEvents.register();
        GemsCommands.register();
    }
}
