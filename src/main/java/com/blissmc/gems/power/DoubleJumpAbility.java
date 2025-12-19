package com.blissmc.gems.power;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
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
        return 2 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (player.isOnGround()) {
            player.sendMessage(Text.literal("Jump first."), true);
            return true;
        }
        Vec3d v = player.getVelocity();
        player.setVelocity(v.x, 0.85D, v.z);
        player.velocityModified = true;
        player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 0.7F, 1.5F);
        return true;
    }
}

