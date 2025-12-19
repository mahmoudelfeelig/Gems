package com.blissmc.gems;

import com.blissmc.gems.config.GemsBalance;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blissmc.gems.command.GemsCommands;
import com.blissmc.gems.item.ModItems;
import com.blissmc.gems.net.GemsPayloads;
import com.blissmc.gems.net.ServerAbilityNetworking;

public final class GemsMod implements ModInitializer {
    public static final String MOD_ID = "gems";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Gems mod");
        GemsBalance.init();
        GemsPayloads.register();
        ModItems.init();
        ServerAbilityNetworking.register();
        GemsModEvents.register();
        GemsCommands.register();
    }
}
