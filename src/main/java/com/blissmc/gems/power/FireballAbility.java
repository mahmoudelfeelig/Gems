package com.blissmc.gems.power;

import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class FireballAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.FIREBALL;
    }

    @Override
    public String name() {
        return "Fireball";
    }

    @Override
    public String description() {
        return "Charge-and-release explosive fireball.";
    }

    @Override
    public int cooldownTicks() {
        return 4 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d spawnPos = player.getEyePos().add(direction.multiply(1.5D));

        FireballEntity fireball = new FireballEntity(player.getWorld(), player, direction, 1);
        fireball.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, player.getYaw(), player.getPitch());
        player.getWorld().spawnEntity(fireball);

        player.getWorld().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENTITY_GHAST_SHOOT,
                SoundCategory.PLAYERS,
                1.0F,
                1.0F
        );
        return true;
    }
}

