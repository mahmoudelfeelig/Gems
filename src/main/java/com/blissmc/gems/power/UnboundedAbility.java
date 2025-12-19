package com.blissmc.gems.power;

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
        return 60 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        AbilityRuntime.startUnbounded(player, 3 * 20);
        player.sendMessage(Text.literal("Unbounded active."), true);
        return true;
    }
}
