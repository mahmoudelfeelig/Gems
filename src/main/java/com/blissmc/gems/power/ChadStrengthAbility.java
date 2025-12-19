package com.blissmc.gems.power;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ChadStrengthAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.CHAD_STRENGTH;
    }

    @Override
    public String name() {
        return "Chad Strength";
    }

    @Override
    public String description() {
        return "For a short time, every 4th hit deals bonus damage.";
    }

    @Override
    public int cooldownTicks() {
        return 90 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        AbilityRuntime.startChadStrength(player, 45 * 20);
        player.sendMessage(Text.literal("Chad Strength active."), true);
        return true;
    }
}

