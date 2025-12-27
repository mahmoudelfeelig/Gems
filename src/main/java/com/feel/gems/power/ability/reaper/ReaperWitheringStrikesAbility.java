package com.feel.gems.power.ability.reaper;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;



public final class ReaperWitheringStrikesAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.REAPER_WITHERING_STRIKES;
    }

    @Override
    public String name() {
        return "Withering Strikes";
    }

    @Override
    public String description() {
        return "Temporarily causes your hits to apply Wither.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().reaper().witheringStrikesCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int duration = GemsBalance.v().reaper().witheringStrikesDurationTicks();
        if (duration <= 0) {
            player.sendMessage(Text.literal("Withering Strikes is disabled."), true);
            return false;
        }
        AbilityRuntime.startReaperWitheringStrikes(player, duration);
        player.sendMessage(Text.literal("Withering Strikes active."), true);
        return true;
    }
}
