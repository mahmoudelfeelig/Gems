package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
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
            AbilityFeedback.burstAt(world, other.getPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.SMOKE, 12, 0.25D);
            affected++;
        }
        AbilityFeedback.sound(player, SoundEvents.ENTITY_ENDERMAN_STARE, 0.8F, 0.8F);
        player.sendMessage(Text.literal("Spooked " + affected + " players."), true);
        return true;
    }
}
