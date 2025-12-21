package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class SpaceGravityFieldAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.SPACE_GRAVITY_FIELD;
    }

    @Override
    public String name() {
        return "Gravity Field";
    }

    @Override
    public String description() {
        return "Creates a field around you that alters gravity for allies and enemies.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().space().gravityFieldCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int duration = GemsBalance.v().space().gravityFieldDurationTicks();
        if (duration <= 0) {
            player.sendMessage(Text.literal("Gravity Field is disabled."), true);
            return false;
        }
        AbilityRuntime.startSpaceGravityField(player, duration);
        player.sendMessage(Text.literal("Gravity Field activated."), true);
        return true;
    }
}
