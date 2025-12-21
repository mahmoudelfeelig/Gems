package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class SummonRecallAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SUMMON_RECALL;
    }

    @Override
    public String name() {
        return "Recall";
    }

    @Override
    public String description() {
        return "Despawns all of your active summons.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().summoner().recallCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int before = SummonerSummons.pruneAndCount(player);
        if (before <= 0) {
            player.sendMessage(Text.literal("No summons to recall."), true);
            return false;
        }
        SummonerSummons.discardAll(player);
        AbilityFeedback.burst(player, net.minecraft.particle.ParticleTypes.POOF, 18, 0.35D);
        AbilityFeedback.sound(player, net.minecraft.sound.SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.7F, 1.2F);
        player.sendMessage(Text.literal("Recalled summons."), true);
        return true;
    }
}

