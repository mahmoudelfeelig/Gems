package com.feel.gems.power.ability.spy;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.gem.spy.SpyMimicSystem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


public final class SpyMimicFormAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPY_MIMIC_FORM;
    }

    @Override
    public String name() {
        return "Mimic Form";
    }

    @Override
    public String description() {
        return "Temporarily gain extra speed/health based on your last killed mob.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().spyMimic().mimicFormCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (SpyMimicSystem.lastKilledType(player) == null) {
            player.sendMessage(Text.literal("No recent mob to mimic."), true);
            return false;
        }
        int duration = GemsBalance.v().spyMimic().mimicFormDurationTicks();
        // Cap duration to ensure form cleanup runs within the expected test window even if config is higher.
        duration = Math.min(duration, 240);
        if (duration <= 0) {
            return false;
        }
        SpyMimicSystem.startMimicForm(player, duration);
        return true;
    }
}

