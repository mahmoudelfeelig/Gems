package com.blissmc.gems.power;

import com.blissmc.gems.config.GemsBalance;
import com.blissmc.gems.trust.GemTrust;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class NullifyAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.NULLIFY;
    }

    @Override
    public String name() {
        return "Nullify";
    }

    @Override
    public String description() {
        return "Removes status effects from nearby enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().strength().nullifyCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        int radius = GemsBalance.v().strength().nullifyRadiusBlocks();
        int affected = 0;
        for (ServerPlayerEntity other : world.getPlayers(p -> p.squaredDistanceTo(player) <= radius * (double) radius)) {
            if (GemTrust.isTrusted(player, other)) {
                continue;
            }
            other.clearStatusEffects();
            affected++;
        }
        player.sendMessage(Text.literal("Nullified " + affected + " players."), true);
        return true;
    }
}
