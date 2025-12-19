package com.blissmc.gems.power;

import com.blissmc.gems.config.GemsBalance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class SpeedStormAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPEED_STORM;
    }

    @Override
    public String name() {
        return "Speed Storm";
    }

    @Override
    public String description() {
        return "Creates a field that buffs trusted players and heavily slows enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().speed().speedStormCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        AbilityRuntime.startSpeedStorm(player, GemsBalance.v().speed().speedStormDurationTicks());
        player.sendMessage(Text.literal("Speed Storm active."), true);
        return true;
    }
}
