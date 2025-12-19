package com.blissmc.gems.power;

import com.blissmc.gems.config.GemsBalance;
import com.blissmc.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
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
            return true;
        }
        if (target instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
            player.sendMessage(Text.literal("Target is trusted."), true);
            return true;
        }

        float amount = GemsBalance.v().life().healthDrainAmount();
        target.damage(player.getDamageSources().magic(), amount);
        player.heal(amount);
        player.sendMessage(Text.literal("Drained " + amount + " health."), true);
        return true;
    }
}
