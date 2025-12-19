package com.blissmc.gems.power;

import com.blissmc.gems.config.GemsBalance;
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
        return GemsBalance.v().strength().chadCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        AbilityRuntime.startChadStrength(player, GemsBalance.v().strength().chadDurationTicks());
        player.sendMessage(Text.literal("Chad Strength active."), true);
        return true;
    }
}
