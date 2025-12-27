package com.feel.gems.power.ability.astra;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
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
        AbilityFeedback.sound(player, SoundEvents.ITEM_SPYGLASS_USE, 0.9F, 1.2F);
        AbilityFeedback.burst(player, ParticleTypes.END_ROD, 18, 0.25D);
        player.sendMessage(Text.literal("Astral Camera active."), true);
        return true;
    }
}
