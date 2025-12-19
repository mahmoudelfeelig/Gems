package com.blissmc.gems.power;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

public final class FrailerAbility implements GemAbility {
    private static final int WEAKNESS_DURATION_TICKS = 8 * 20;

    @Override
    public Identifier id() {
        return PowerIds.FRAILER;
    }

    @Override
    public String name() {
        return "Frailer";
    }

    @Override
    public String description() {
        return "Applies Weakness to a targeted enemy.";
    }

    @Override
    public int cooldownTicks() {
        return 20 * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        LivingEntity target = raycastLivingTarget(player, 20.0D);
        if (target == null) {
            player.sendMessage(Text.literal("No target."), true);
            return true;
        }
        if (player.isTeammate(target)) {
            player.sendMessage(Text.literal("Target is an ally."), true);
            return true;
        }

        target.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, WEAKNESS_DURATION_TICKS, 0));
        player.sendMessage(Text.literal("Frailer: weakened " + target.getName().getString()), true);
        return true;
    }

    private static LivingEntity raycastLivingTarget(ServerPlayerEntity player, double maxDistance) {
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
        Predicate<net.minecraft.entity.Entity> predicate = entity ->
                entity instanceof LivingEntity living && living.isAlive() && living != player;
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

