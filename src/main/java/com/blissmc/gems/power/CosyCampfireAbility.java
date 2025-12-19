package com.blissmc.gems.power;

import com.blissmc.gems.config.GemsBalance;
import net.minecraft.server.network.ServerPlayerEntity;
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
        player.sendMessage(Text.literal("Cosy Campfire active."), true);
        return true;
    }
}
