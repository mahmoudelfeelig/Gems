package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
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
        return GemsBalance.v().fire().heatHazeCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        AbilityRuntime.startHeatHazeZone(player, GemsBalance.v().fire().heatHazeDurationTicks());
        AbilityFeedback.sound(player, SoundEvents.ITEM_FIRECHARGE_USE, 0.9F, 1.0F);
        AbilityFeedback.burst(player, ParticleTypes.FLAME, 20, 0.35D);
        player.sendMessage(Text.literal("Heat Haze Zone active."), true);
        return true;
    }
}
