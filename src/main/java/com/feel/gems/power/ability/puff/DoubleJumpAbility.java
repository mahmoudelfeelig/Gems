package com.feel.gems.power.ability.puff;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;



public final class DoubleJumpAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.DOUBLE_JUMP;
    }

    @Override
    public String name() {
        return "Double Jump";
    }

    @Override
    public String description() {
        return "Launches you upward; usable midair.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().puff().doubleJumpCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (player.isOnGround()) {
            player.sendMessage(Text.literal("Jump first."), true);
            return false;
        }
        Vec3d v = player.getVelocity();
        player.setVelocity(v.x, GemsBalance.v().puff().doubleJumpVelocityY(), v.z);
        player.velocityDirty = true;
        AbilityFeedback.syncVelocity(player);
        AbilityFeedback.burst(player, ParticleTypes.CLOUD, 12, 0.2D);
        AbilityFeedback.sound(player, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.7F, 1.5F);
        return true;
    }
}
