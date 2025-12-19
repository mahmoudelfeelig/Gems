package com.blissmc.gems.power;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class AstralProjectionAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.ASTRAL_PROJECTION;
    }

    @Override
    public String name() {
        return "Astral Projection";
    }

    @Override
    public String description() {
        return "Temporarily grants spectator mode.";
    }

    @Override
    public int cooldownTicks() {
        return 60 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        AbilityRuntime.startProjection(player, 6 * 20);
        player.sendMessage(Text.literal("Astral Projection active."), true);
        return true;
    }
}

