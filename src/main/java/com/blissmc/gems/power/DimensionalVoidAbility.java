package com.blissmc.gems.power;

import com.blissmc.gems.trust.GemTrust;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class DimensionalVoidAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.DIMENSIONAL_VOID;
    }

    @Override
    public String name() {
        return "Dimensional Void";
    }

    @Override
    public String description() {
        return "Suppresses enemy gem abilities in a radius for a short duration.";
    }

    @Override
    public int cooldownTicks() {
        return 60 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        int duration = 8 * 20;
        int affected = 0;
        for (ServerPlayerEntity other : world.getPlayers(p -> p.squaredDistanceTo(player) <= 10.0D * 10.0D)) {
            if (GemTrust.isTrusted(player, other)) {
                continue;
            }
            AbilityRestrictions.suppress(other, duration);
            affected++;
        }
        player.sendMessage(Text.literal("Dimensional Void: suppressed " + affected + " players."), true);
        return true;
    }
}

