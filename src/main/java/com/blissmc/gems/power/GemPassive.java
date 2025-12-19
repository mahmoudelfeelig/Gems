package com.blissmc.gems.power;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public interface GemPassive {
    Identifier id();

    String name();

    String description();

    void apply(ServerPlayerEntity player);

    void remove(ServerPlayerEntity player);
}

