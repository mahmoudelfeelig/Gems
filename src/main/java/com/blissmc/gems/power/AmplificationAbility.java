package com.blissmc.gems.power;

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
        return 180 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        EnchantmentAmplification.apply(player, 45 * 20);
        player.sendMessage(Text.literal("Amplification active."), true);
        return true;
    }
}

