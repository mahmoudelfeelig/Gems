package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * Thorns Nova - Explode thorns outward, damaging all nearby enemies.
 */
public final class BonusThornsNovaAbility implements GemAbility {
    private static final double RANGE = 8.0;
    private static final float DAMAGE = 10.0f;

    @Override
    public Identifier id() {
        return PowerIds.BONUS_THORNS_NOVA;
    }

    @Override
    public String name() {
        return "Thorns Nova";
    }

    @Override
    public String description() {
        return "Explode thorns outward, dealing 10 damage to all enemies within 8 blocks.";
    }

    @Override
    public int cooldownTicks() {
        return 300; // 15 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        Box box = player.getBoundingBox().expand(RANGE);
        List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !(e instanceof ServerPlayerEntity p && p.isCreative()));

        if (entities.isEmpty()) {
            return false;
        }

        for (LivingEntity entity : entities) {
            if (entity instanceof ServerPlayerEntity otherPlayer) {
                if (VoidImmunity.shouldBlockEffect(player, otherPlayer)) {
                    continue;
                }
                if (GemTrust.isTrusted(player, otherPlayer)) {
                    continue;
                }
            }
            entity.damage(world, world.getDamageSources().thorns(player), DAMAGE);
            
            // Knockback
            Vec3d knockback = entity.getEntityPos().subtract(player.getEntityPos()).normalize().multiply(0.8);
            entity.setVelocity(entity.getVelocity().add(knockback.x, 0.2, knockback.z));
            entity.velocityDirty = true;
        }

        // Thorn particle explosion
        for (int i = 0; i < 50; i++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double radius = world.random.nextDouble() * RANGE;
            double x = player.getX() + Math.cos(angle) * radius;
            double z = player.getZ() + Math.sin(angle) * radius;
            world.spawnParticles(ParticleTypes.CRIT, x, player.getY() + 0.5, z, 2, 0.1, 0.3, 0.1, 0.1);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_SWEET_BERRY_BUSH_PICK_BERRIES, SoundCategory.PLAYERS, 1.5f, 0.5f);
        return true;
    }
}
