package com.blissmc.gems.power;

import com.blissmc.gems.config.GemsBalance;
import com.blissmc.gems.trust.GemTrust;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class FumbleAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.FUMBLE;
    }

    @Override
    public String name() {
        return "Fumble";
    }

    @Override
    public String description() {
        return "Fumble: enemies cannot use their offhand and cannot eat for a short time.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().wealth().fumbleCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        int radius = GemsBalance.v().wealth().fumbleRadiusBlocks();
        int affected = 0;
        for (ServerPlayerEntity other : world.getPlayers(p -> p.squaredDistanceTo(player) <= radius * (double) radius)) {
            if (GemTrust.isTrusted(player, other)) {
                continue;
            }
            WealthFumble.apply(other, GemsBalance.v().wealth().fumbleDurationTicks());
            affected++;
        }
        player.sendMessage(Text.literal("Fumble affected " + affected + " players."), true);
        return true;
    }
}
