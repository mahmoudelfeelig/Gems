package com.feel.gems.power.ability.reaper;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;



public final class ReaperDeathOathAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.REAPER_DEATH_OATH;
    }

    @Override
    public String name() {
        return "Death Oath";
    }

    @Override
    public String description() {
        return "Bind yourself to a target; you slowly lose health until you hit them.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().reaper().deathOathCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int range = GemsBalance.v().reaper().deathOathRangeBlocks();
        LivingEntity target = Targeting.raycastLiving(player, range);
        if (!(target instanceof ServerPlayerEntity other)) {
            player.sendMessage(Text.literal("No player target."), true);
            return false;
        }
        if (GemTrust.isTrusted(player, other)) {
            player.sendMessage(Text.literal("Target is trusted."), true);
            return false;
        }
        int duration = GemsBalance.v().reaper().deathOathDurationTicks();
        if (duration <= 0) {
            player.sendMessage(Text.literal("Death Oath is disabled."), true);
            return false;
        }
        AbilityRuntime.startReaperDeathOath(player, other.getUuid(), duration);
        player.sendMessage(Text.literal("Death Oath: " + other.getName().getString()), true);
        return true;
    }
}
