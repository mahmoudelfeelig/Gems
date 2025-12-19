package com.blissmc.gems.power;

import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public final class AstralDaggersAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.ASTRAL_DAGGERS;
    }

    @Override
    public String name() {
        return "Astral Daggers";
    }

    @Override
    public String description() {
        return "Fires a volley of fast ranged daggers.";
    }

    @Override
    public int cooldownTicks() {
        return 8 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d spawnPos = player.getEyePos().add(direction.multiply(1.2D));

        for (int i = 0; i < 5; i++) {
            ArrowEntity arrow = new ArrowEntity(player.getWorld(), player, new ItemStack(Items.ARROW), ItemStack.EMPTY);
            arrow.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
            Vec3d spread = direction.add(
                    (player.getRandom().nextDouble() - 0.5D) * 0.05D,
                    (player.getRandom().nextDouble() - 0.5D) * 0.05D,
                    (player.getRandom().nextDouble() - 0.5D) * 0.05D
            );
            arrow.setVelocity(spread.x, spread.y, spread.z, 3.5F, 0.0F);
            arrow.setDamage(4.0D);
            arrow.setCritical(false);
            arrow.pickupType = ArrowEntity.PickupPermission.DISALLOWED;
            player.getWorld().spawnEntity(arrow);
        }

        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0F, 1.4F);
        return true;
    }
}
