package com.feel.gems.power.ability.pillager;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.pillager.PillagerVolleyRuntime;
import com.feel.gems.power.registry.PowerIds;
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
            player.sendMessage(Text.translatable("gems.ability.pillager.volley.disabled"), true);
            return false;
        }
        PillagerVolleyRuntime.start(player, duration);
        return true;
    }
}

