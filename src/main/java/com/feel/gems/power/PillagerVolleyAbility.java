package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class PillagerVolleyAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.PILLAGER_VOLLEY;
    }

    @Override
    public String name() {
        return "Volley";
    }

    @Override
    public String description() {
        return "Automatically fires a short burst of arrows.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().pillager().volleyCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int duration = GemsBalance.v().pillager().volleyDurationTicks();
        if (duration <= 0) {
            player.sendMessage(Text.literal("Volley is disabled."), true);
            return false;
        }
        PillagerVolleyRuntime.start(player, duration);
        return true;
    }
}

