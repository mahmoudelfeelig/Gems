package com.blissmc.gems.power;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class HeatHazeZoneAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.HEAT_HAZE_ZONE;
    }

    @Override
    public String name() {
        return "Heat Haze Zone";
    }

    @Override
    public String description() {
        return "Heat haze zone: allies gain Fire Resistance; enemies gain Mining Fatigue and Weakness.";
    }

    @Override
    public int cooldownTicks() {
        return 90 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        AbilityRuntime.startHeatHazeZone(player, 10 * 20);
        player.sendMessage(Text.literal("Heat Haze Zone active."), true);
        return true;
    }
}
