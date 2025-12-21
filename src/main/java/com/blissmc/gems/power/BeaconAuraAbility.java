package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class BeaconAuraAbility implements GemAbility {
    private final BeaconAuraRuntime.AuraType type;

    public BeaconAuraAbility(BeaconAuraRuntime.AuraType type) {
        this.type = type;
    }

    @Override
    public Identifier id() {
        return type.id();
    }

    @Override
    public String name() {
        return "Aura: " + type.label();
    }

    @Override
    public String description() {
        return "Becomes a moving beacon that grants " + type.label() + " to trusted allies nearby.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().beacon().auraCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int duration = GemsBalance.v().beacon().auraDurationTicks();
        if (duration <= 0) {
            player.sendMessage(Text.literal("Beacon aura is disabled."), true);
            return false;
        }
        BeaconSupportRuntime.applyRally(player);
        BeaconAuraRuntime.start(player, type, duration);

        AbilityFeedback.ring(player.getServerWorld(), player.getPos().add(0.0D, 0.1D, 0.0D), 2.5D, ParticleTypes.END_ROD, 24);
        AbilityFeedback.sound(player, SoundEvents.BLOCK_BEACON_POWER_SELECT, 0.8F, 1.2F);
        return true;
    }
}

