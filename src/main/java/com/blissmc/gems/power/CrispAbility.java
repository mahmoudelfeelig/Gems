package com.blissmc.gems.power;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class CrispAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.CRISP;
    }

    @Override
    public String name() {
        return "Crisp";
    }

    @Override
    public String description() {
        return "Evaporates water in an area for a short duration.";
    }

    @Override
    public int cooldownTicks() {
        return 90 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        AbilityRuntime.startCrisp(player, 10 * 20);
        player.sendMessage(Text.literal("Crisp active."), true);
        return true;
    }
}

