package com.feel.gems.power.ability.beacon;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.beacon.BeaconAuraRuntime;
import com.feel.gems.power.gem.beacon.BeaconSupportRuntime;
import com.feel.gems.power.runtime.AbilityFeedback;
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
        return "Sets your active beacon aura to " + type.label() + " until switched off.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().beacon().auraCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        BeaconAuraRuntime.AuraType active = BeaconAuraRuntime.activeType(player);
        if (active == type) {
            BeaconAuraRuntime.setActive(player, null);
            AbilityFeedback.sound(player, SoundEvents.BLOCK_BEACON_DEACTIVATE, 0.7F, 0.9F);
            player.sendMessage(Text.literal("Beacon aura disabled."), true);
            return true;
        }

        BeaconSupportRuntime.applyRally(player);
        BeaconAuraRuntime.setActive(player, type);

        AbilityFeedback.ring(player.getServerWorld(), player.getPos().add(0.0D, 0.1D, 0.0D), 2.5D, ParticleTypes.END_ROD, 24);
        AbilityFeedback.sound(player, SoundEvents.BLOCK_BEACON_POWER_SELECT, 0.8F, 1.2F);
        player.sendMessage(Text.literal("Beacon aura set to " + type.label() + "."), true);
        return true;
    }
}

