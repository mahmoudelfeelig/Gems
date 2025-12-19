package com.blissmc.gems.power;

import com.blissmc.gems.config.GemsBalance;
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
        return GemsBalance.v().life().lifeCircleCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        AbilityRuntime.startLifeCircle(player, GemsBalance.v().life().lifeCircleDurationTicks());
        player.sendMessage(Text.literal("Life Circle active."), true);
        return true;
    }
}
