package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class HealthDrainAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.HEALTH_DRAIN;
    }

    @Override
    public String name() {
        return "Health Drain";
    }

    @Override
    public String description() {
        return "Siphons health from a target to heal you.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().life().healthDrainCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        LivingEntity target = Targeting.raycastLiving(player, GemsBalance.v().life().healthDrainRangeBlocks());
        if (target == null) {
            player.sendMessage(Text.literal("No target."), true);
            return false;
        }
        if (target instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
            player.sendMessage(Text.literal("Target is trusted."), true);
            return false;
        }

        float amount = GemsBalance.v().life().healthDrainAmount();
        target.damage(player.getDamageSources().magic(), amount);
        player.heal(amount);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 0.7F, 1.4F);
        AbilityFeedback.burst(player, ParticleTypes.HEART, 10, 0.25D);
        if (player.getServerWorld() != null) {
            AbilityFeedback.burstAt(player.getServerWorld(), target.getPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.DAMAGE_INDICATOR, 12, 0.25D);
        }
        player.sendMessage(Text.literal("Drained " + amount + " health."), true);
        return true;
    }
}
