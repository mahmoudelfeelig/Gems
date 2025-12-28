package com.feel.gems.power.ability.terror;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.terror.TerrorRemoteChargeRuntime;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class TerrorRemoteChargeAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.TERROR_REMOTE_CHARGE;
    }

    @Override
    public String name() {
        return "Remote Charge";
    }

    @Override
    public String description() {
        return "Arm a remote charge on a block, then detonate it from anywhere.";
    }

    @Override
    public int cooldownTicks() {
        return 0;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (TerrorRemoteChargeRuntime.detonate(player)) {
            AbilityFeedback.burst(player, ParticleTypes.SMOKE, 10, 0.2D);
            AbilityFeedback.sound(player, SoundEvents.ENTITY_TNT_PRIMED, 0.8F, 0.9F);
            player.sendMessage(Text.literal("Remote charge detonated."), true);
            return true;
        }

        if (TerrorRemoteChargeRuntime.hasActiveCharge(player)) {
            player.sendMessage(Text.literal("Remote charge is already armed. Use the ability again to detonate."), true);
            return false;
        }

        if (!TerrorRemoteChargeRuntime.startArming(player)) {
            player.sendMessage(Text.literal("Remote charge is unavailable."), true);
            return false;
        }

        int windowSeconds = GemsBalance.v().terror().remoteChargeArmWindowTicks() / 20;
        player.sendMessage(Text.literal("Right-click a block within " + windowSeconds + "s to arm the charge."), true);
        return true;
    }
}
