package com.feel.gems.power.ability.astra;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.augment.AugmentRuntime;
import com.feel.gems.core.GemId;
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
        int duration = AugmentRuntime.applyDurationMultiplier(player, GemId.ASTRA, GemsBalance.v().astra().astralCameraDurationTicks());
        AbilityRuntime.startAstralCamera(player, duration);
        AbilityFeedback.sound(player, SoundEvents.ITEM_SPYGLASS_USE, 0.9F, 1.2F);
        AbilityFeedback.burst(player, ParticleTypes.END_ROD, 18, 0.25D);
        player.sendMessage(Text.translatable("gems.ability.astra.camera.active"), true);
        return true;
    }
}
