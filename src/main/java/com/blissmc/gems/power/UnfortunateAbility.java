package com.blissmc.gems.power;

import com.blissmc.gems.trust.GemTrust;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class UnfortunateAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.UNFORTUNATE;
    }

    @Override
    public String name() {
        return "Unfortunate";
    }

    @Override
    public String description() {
        return "Chance to stun nearby enemies, disabling attacks and interactions.";
    }

    @Override
    public int cooldownTicks() {
        return 20 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        int stunned = 0;
        for (ServerPlayerEntity other : world.getPlayers(p -> p.squaredDistanceTo(player) <= 10.0D * 10.0D)) {
            if (GemTrust.isTrusted(player, other)) {
                continue;
            }
            if (player.getRandom().nextFloat() <= 0.35F) {
                AbilityRestrictions.stun(other, 3 * 20);
                stunned++;
            }
        }
        player.sendMessage(Text.literal("Unfortunate: stunned " + stunned + " players."), true);
        return true;
    }
}

