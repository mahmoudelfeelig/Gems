package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class CosyCampfireAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.COSY_CAMPFIRE;
    }

    @Override
    public String name() {
        return "Cosy Campfire";
    }

    @Override
    public String description() {
        return "Creates an aura that grants trusted players Regeneration IV.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().fire().cosyCampfireCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        AbilityRuntime.startCosyCampfire(player, GemsBalance.v().fire().cosyCampfireDurationTicks());
        AbilityFeedback.sound(player, SoundEvents.BLOCK_CAMPFIRE_CRACKLE, 0.9F, 1.0F);
        AbilityFeedback.burst(player, ParticleTypes.CAMPFIRE_COSY_SMOKE, 12, 0.25D);
        player.sendMessage(Text.literal("Cosy Campfire active."), true);
        return true;
    }
}
