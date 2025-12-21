package com.feel.gems.power;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

public final class Targeting {
    private Targeting() {
    }

    public static LivingEntity raycastLiving(ServerPlayerEntity player, double maxDistance) {
        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d direction = player.getRotationVec(1.0F);
        Vec3d end = start.add(direction.multiply(maxDistance));

        HitResult blockHit = player.raycast(maxDistance, 1.0F, false);
        double maxDistanceSq = maxDistance * maxDistance;
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getPos();
            maxDistanceSq = start.squaredDistanceTo(end);
        }

        Box box = player.getBoundingBox().stretch(direction.multiply(maxDistance)).expand(1.0D);
        Predicate<Entity> predicate = entity -> entity instanceof LivingEntity living && living.isAlive() && living != player;
        EntityHitResult entityHit = ProjectileUtil.raycast(player, start, end, box, predicate, maxDistanceSq);
        if (entityHit == null) {
            return null;
        }
        if (!(entityHit.getEntity() instanceof LivingEntity living)) {
            return null;
        }
        return living;
    }
}

