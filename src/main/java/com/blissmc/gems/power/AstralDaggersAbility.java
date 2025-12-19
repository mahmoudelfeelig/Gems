package com.blissmc.gems.power;

import com.blissmc.gems.config.GemsBalance;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
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
        return GemsBalance.v().astra().astralDaggersCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int count = GemsBalance.v().astra().astralDaggersCount();
        float spreadAmount = GemsBalance.v().astra().astralDaggersSpread();
        float velocity = GemsBalance.v().astra().astralDaggersVelocity();
        float damage = GemsBalance.v().astra().astralDaggersDamage();

        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d spawnPos = player.getEyePos().add(direction.multiply(1.2D));

        AbilityFeedback.burstAt(player.getServerWorld(), spawnPos, ParticleTypes.END_ROD, Math.min(24, count * 3), 0.12D);

        for (int i = 0; i < count; i++) {
            ArrowEntity arrow = new ArrowEntity(player.getWorld(), player, new ItemStack(Items.ARROW), ItemStack.EMPTY);
            arrow.setPosition(spawnPos.x, spawnPos.y, spawnPos.z);
            Vec3d spread = direction.add(
                    (player.getRandom().nextDouble() - 0.5D) * spreadAmount,
                    (player.getRandom().nextDouble() - 0.5D) * spreadAmount,
                    (player.getRandom().nextDouble() - 0.5D) * spreadAmount
            );
            arrow.setVelocity(spread.x, spread.y, spread.z, velocity, 0.0F);
            arrow.setDamage(damage);
            arrow.setCritical(false);
            arrow.pickupType = ArrowEntity.PickupPermission.DISALLOWED;
            player.getWorld().spawnEntity(arrow);
        }

        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0F, 1.4F);
        return true;
    }
}
