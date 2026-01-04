package com.feel.gems.power.ability.bonus;

import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class BonusGravityWellAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.BONUS_GRAVITY_WELL;
    }

    @Override
    public String name() {
        return "Gravity Well";
    }

    @Override
    public String description() {
        return "Create a gravity well that pulls enemies toward you.";
    }

    @Override
    public int cooldownTicks() {
        return 500; // 25 seconds
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getEntityWorld();
        Vec3d center = player.getEntityPos();
        Box area = player.getBoundingBox().expand(12);
        
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y + 1, center.z, 100, 6, 2, 6, 0.1);
        
        world.getOtherEntities(player, area, e -> e instanceof LivingEntity)
                .forEach(e -> {
                    Vec3d toPlayer = center.subtract(e.getEntityPos()).normalize().multiply(1.5);
                    e.setVelocity(toPlayer);
                    e.velocityDirty = true;
                });
        return true;
    }
}
