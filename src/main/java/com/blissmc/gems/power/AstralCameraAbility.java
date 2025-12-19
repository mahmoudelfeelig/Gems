package com.blissmc.gems.power;

import com.blissmc.gems.config.GemsBalance;
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
        return GemsBalance.v().astra().astralCameraCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        AbilityRuntime.startAstralCamera(player, GemsBalance.v().astra().astralCameraDurationTicks());
        player.sendMessage(Text.literal("Astral Camera active."), true);
        return true;
    }
}
