package com.blissmc.gems.power;

import com.blissmc.gems.config.GemsBalance;
import com.blissmc.gems.trust.GemTrust;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class SpookAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPOOK;
    }

    @Override
    public String name() {
        return "Spook";
    }

    @Override
    public String description() {
        return "Briefly disorients nearby enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().astra().spookCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        int duration = GemsBalance.v().astra().spookDurationTicks();
        int radius = GemsBalance.v().astra().spookRadiusBlocks();
        int affected = 0;
        for (ServerPlayerEntity other : world.getPlayers(p -> p.squaredDistanceTo(player) <= radius * (double) radius)) {
            if (GemTrust.isTrusted(player, other)) {
                continue;
            }
            other.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, duration, 0, true, false, false));
            other.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, duration, 0, true, false, false));
            affected++;
        }
        player.sendMessage(Text.literal("Spooked " + affected + " players."), true);
        return true;
    }
}
