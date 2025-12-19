package com.blissmc.gems.power;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class AstralCameraAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.ASTRAL_CAMERA;
    }

    @Override
    public String name() {
        return "Astral Camera";
    }

    @Override
    public String description() {
        return "Astral camera: scout in Spectator mode, then return to your original position.";
    }

    @Override
    public int cooldownTicks() {
        return 60 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        AbilityRuntime.startAstralCamera(player, 8 * 20);
        player.sendMessage(Text.literal("Astral Camera active."), true);
        return true;
    }
}
