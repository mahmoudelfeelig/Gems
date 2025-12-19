package com.blissmc.gems.power;

import com.blissmc.gems.config.GemsBalance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class AmplificationAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.AMPLIFICATION;
    }

    @Override
    public String name() {
        return "Amplification";
    }

    @Override
    public String description() {
        return "Temporarily boosts enchantments on your tools and armor.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().wealth().amplificationCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        EnchantmentAmplification.apply(player, GemsBalance.v().wealth().amplificationDurationTicks());
        player.sendMessage(Text.literal("Amplification active."), true);
        return true;
    }
}
