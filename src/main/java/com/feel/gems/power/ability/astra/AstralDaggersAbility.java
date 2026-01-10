package com.feel.gems.power.ability.astra;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
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

        ServerWorld world = player.getEntityWorld();
        AbilityFeedback.burstAt(world, spawnPos, ParticleTypes.END_ROD, Math.min(24, count * 3), 0.12D);

        for (int i = 0; i < count; i++) {
            // 1.21+ validates that the "weapon" stack is a valid projectile weapon for arrows.
            ArrowEntity arrow = new ArrowEntity(world, player, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
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
            world.spawnEntity(arrow);
        }

        AbilityFeedback.sound(player, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.4F);
        return true;
    }
}
