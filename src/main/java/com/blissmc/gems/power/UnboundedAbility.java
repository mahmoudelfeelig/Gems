package com.blissmc.gems.power;

import com.blissmc.gems.config.GemsBalance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class UnboundedAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.UNBOUNDED;
    }

    @Override
    public String name() {
        return "Unbounded";
    }

    @Override
    public String description() {
        return "Briefly grants Spectator mode, then returns you to normal gameplay.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().astra().unboundedCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        AbilityRuntime.startUnbounded(player, GemsBalance.v().astra().unboundedDurationTicks());
        player.sendMessage(Text.literal("Unbounded active."), true);
        return true;
    }
}
