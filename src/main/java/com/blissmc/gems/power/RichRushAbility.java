package com.blissmc.gems.power;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class RichRushAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.RICH_RUSH;
    }

    @Override
    public String name() {
        return "Rich Rush";
    }

    @Override
    public String description() {
        return "Temporarily increases ore and mob drops.";
    }

    @Override
    public int cooldownTicks() {
        return 9 * 60 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        AbilityRuntime.startRichRush(player, 3 * 60 * 20);
        player.sendMessage(Text.literal("Rich Rush active."), true);
        return true;
    }
}

