package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.util.GemsTime;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class SpyEchoAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPY_ECHO;
    }

    @Override
    public String name() {
        return "Echo";
    }

    @Override
    public String description() {
        return "Replays the last observed ability used in front of you.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().spyMimic().echoCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        Identifier last = SpyMimicSystem.lastSeenAbility(player);
        if (last == null) {
            player.sendMessage(Text.literal("No observed ability."), true);
            return false;
        }
        long now = GemsTime.now(player);
        long seenAt = SpyMimicSystem.lastSeenAt(player);
        int window = GemsBalance.v().spyMimic().echoWindowTicks();
        if (seenAt <= 0 || now - seenAt > window) {
            player.sendMessage(Text.literal("Echo expired."), true);
            return false;
        }

        GemAbility ability = ModAbilities.get(last);
        if (ability == null) {
            player.sendMessage(Text.literal("Unknown ability: " + last), true);
            return false;
        }
        if (last.equals(PowerIds.SPY_ECHO) || last.equals(PowerIds.SPY_STEAL) || last.equals(PowerIds.SPY_STOLEN_CAST)) {
            player.sendMessage(Text.literal("Can't echo that ability."), true);
            return false;
        }
        boolean ok = ability.activate(player);
        if (!ok) {
            player.sendMessage(Text.literal("Echo failed."), true);
        }
        return ok;
    }
}

