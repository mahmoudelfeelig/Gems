package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.trust.GemTrust;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
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
            AbilityFeedback.burstAt(world, other.getPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.ENCHANT, 10, 0.25D);
            affected++;
        }
        AbilityFeedback.sound(player, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 0.8F, 0.8F);
        AbilityFeedback.burst(player, ParticleTypes.ENCHANT, 14, 0.35D);
        player.sendMessage(Text.literal("Nullified " + affected + " players."), true);
        return true;
    }
}
