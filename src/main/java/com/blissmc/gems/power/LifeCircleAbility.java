package com.blissmc.gems.power;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class LifeCircleAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.LIFE_CIRCLE;
    }

    @Override
    public String name() {
        return "Life Circle";
    }

    @Override
    public String description() {
        return "Aura that boosts trusted players' max health and reduces enemies' max health.";
    }

    @Override
    public int cooldownTicks() {
        return 60 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        AbilityRuntime.startLifeCircle(player, 12 * 20);
        player.sendMessage(Text.literal("Life Circle active."), true);
        return true;
    }
}

