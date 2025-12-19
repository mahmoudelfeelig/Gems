package com.blissmc.gems.power;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public interface GemAbility {
    Identifier id();

    String name();

    String description();

    int cooldownTicks();

    boolean activate(ServerPlayerEntity player);
}

