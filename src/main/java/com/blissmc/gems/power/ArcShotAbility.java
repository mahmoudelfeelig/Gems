package com.feel.gems.power;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.trust.GemTrust;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.particle.ParticleTypes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ArcShotAbility implements GemAbility {
    @Override
    public Identifier id() {
        return PowerIds.ARC_SHOT;
    }

    @Override
    public String name() {
        return "Arc Shot";
    }

    @Override
    public String description() {
        return "Arc Shot: strikes several enemies along a line with lightning and knockback.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().speed().arcShotCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        ServerWorld world = player.getServerWorld();
        double maxDistance = GemsBalance.v().speed().arcShotRangeBlocks();
        double radius = GemsBalance.v().speed().arcShotRadiusBlocks();
        int maxTargets = GemsBalance.v().speed().arcShotMaxTargets();

        Vec3d start = player.getCameraPosVec(1.0F);
        Vec3d dir = player.getRotationVec(1.0F).normalize();
        Vec3d end = start.add(dir.multiply(maxDistance));

        HitResult blockHit = player.raycast(maxDistance, 1.0F, false);
        double length = maxDistance;
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getPos();
            length = start.distanceTo(end);
        }

        Box box = new Box(start, end).expand(radius);
        List<Hit> hits = new ArrayList<>();
        for (var e : world.getOtherEntities(player, box, ent -> ent instanceof LivingEntity living && living.isAlive())) {
            LivingEntity living = (LivingEntity) e;
            if (living instanceof ServerPlayerEntity other && GemTrust.isTrusted(player, other)) {
                continue;
            }

            Vec3d pos = living.getPos();
            double t = pos.subtract(start).dotProduct(dir);
            if (t < 0.0D || t > length) {
                continue;
            }
            Vec3d closest = start.add(dir.multiply(t));
            double distSq = pos.squaredDistanceTo(closest);
            if (distSq > radius * radius) {
                continue;
            }
            hits.add(new Hit(living, t));
        }

        hits.sort(Comparator.comparingDouble(h -> h.t));
        if (hits.isEmpty()) {
            player.sendMessage(Text.literal("No targets."), true);
            return true;
        }

        int count = 0;
        AbilityFeedback.burstAt(world, start, ParticleTypes.ELECTRIC_SPARK, 18, 0.2D);
        for (Hit hit : hits) {
            LivingEntity target = hit.target;
            spawnLightning(world, target.getPos());
            target.damage(player.getDamageSources().lightningBolt(), GemsBalance.v().speed().arcShotDamage());
            AbilityFeedback.beam(world, start, target.getPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.ELECTRIC_SPARK, 12);

            Vec3d away = target.getPos().subtract(player.getPos()).normalize();
            target.addVelocity(away.x * 0.8D, 0.4D, away.z * 0.8D);
            target.velocityModified = true;

            count++;
            if (count >= maxTargets) {
                break;
            }
        }

        AbilityFeedback.sound(player, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6F, 1.2F);
        return true;
    }

    private static void spawnLightning(ServerWorld world, Vec3d pos) {
        LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
        if (bolt == null) {
            return;
        }
        bolt.refreshPositionAfterTeleport(pos.x, pos.y, pos.z);
        bolt.setCosmetic(true);
        world.spawnEntity(bolt);
    }

    private record Hit(LivingEntity target, double t) {
    }
}
