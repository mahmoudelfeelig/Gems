package com.blissmc.gems.power;

import com.blissmc.gems.trust.GemTrust;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class HeartLockAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.HEART_LOCK;
    }

    @Override
    public String name() {
        return "Heart Lock";
    }

    @Override
    public String description() {
        return "Temporarily locks an enemy player's max health to their current health.";
    }

    @Override
    public int cooldownTicks() {
        return 45 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        var target = Targeting.raycastLiving(player, 20.0D);
        if (!(target instanceof ServerPlayerEntity other)) {
            player.sendMessage(Text.literal("No player target."), true);
            return true;
        }
        if (GemTrust.isTrusted(player, other)) {
            player.sendMessage(Text.literal("Target is trusted."), true);
            return true;
        }

        AbilityRuntime.startHeartLock(player, other, 6 * 20);
        player.sendMessage(Text.literal("Heart Lock applied."), true);
        return true;
    }
}

