package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Corpse Explosion - Detonate nearby corpses (recently killed entities) for AoE damage.
 */
public final class BonusCorpseExplosionAbility implements GemAbility {
    private static final Map<UUID, MarkedTarget> MARKED = new HashMap<>();
    private static final Map<UUID, Long> SUPPRESS_CHAIN_UNTIL = new HashMap<>();

    @Override
    public Identifier id() {
        return PowerIds.BONUS_CORPSE_EXPLOSION;
    }

    @Override
    public String name() {
        return "Corpse Explosion";
    }

    @Override
    public String description() {
        return "Detonate nearby corpses for AoE damage.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().corpseExplosionCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) return false;
        var cfg = GemsBalance.v().bonusPool();
        pruneMarks(world);

        int range = cfg.corpseExplosionCorpseRangeBlocks;
        if (range <= 0) {
            return false;
        }
        int durationTicks = Math.max(1, cfg.corpseExplosionMarkDurationSeconds * 20);
        long expiresAt = world.getTime() + durationTicks;

        Box searchBox = player.getBoundingBox().expand(range);
        List<LivingEntity> targets = world.getEntitiesByClass(LivingEntity.class, searchBox,
                e -> e.isAlive() && e != player);
        if (targets.isEmpty()) {
            return false;
        }
        boolean markedAny = false;
        for (LivingEntity target : targets) {
            if (target instanceof ServerPlayerEntity other) {
                if (GemTrust.isTrusted(player, other) || VoidImmunity.shouldBlockEffect(player, other)) {
                    continue;
                }
            }
            MARKED.put(target.getUuid(), new MarkedTarget(world, player.getUuid(), expiresAt));
            world.spawnParticles(ParticleTypes.SOUL, target.getX(), target.getY() + 0.5, target.getZ(),
                    8, 0.3, 0.4, 0.3, 0.02);
            markedAny = true;
        }
        if (!markedAny) {
            return false;
        }
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PARTICLE_SOUL_ESCAPE, SoundCategory.PLAYERS, 0.6f, 1.1f);
        return true;
    }

    public static void recordCorpse(LivingEntity entity) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        Long suppressed = SUPPRESS_CHAIN_UNTIL.remove(entity.getUuid());
        if (suppressed != null && world.getTime() <= suppressed) {
            return;
        }
        MarkedTarget marked = MARKED.remove(entity.getUuid());
        if (marked == null || marked.world != world) {
            return;
        }
        long now = world.getTime();
        if (now > marked.expiresAt) {
            return;
        }
        explodeAt(marked, entity.getEntityPos());
    }

    private static void explodeAt(MarkedTarget marked, Vec3d pos) {
        ServerWorld world = marked.world;
        ServerPlayerEntity owner = world.getServer().getPlayerManager().getPlayer(marked.ownerUuid);
        var cfg = GemsBalance.v().bonusPool();
        long now = world.getTime();

        Box explosionBox = new Box(pos, pos).expand(cfg.corpseExplosionRadiusBlocks);
        List<LivingEntity> nearby = world.getEntitiesByClass(LivingEntity.class, explosionBox,
                e -> e.isAlive());

        for (LivingEntity entity : nearby) {
            if (owner != null && entity instanceof ServerPlayerEntity other) {
                if (GemTrust.isTrusted(owner, other) || VoidImmunity.shouldBlockEffect(owner, other)) {
                    continue;
                }
            }
            if (owner != null && entity == owner) {
                continue;
            }
            SUPPRESS_CHAIN_UNTIL.put(entity.getUuid(), now + 4);
            entity.damage(world, world.getDamageSources().explosion(null, owner), cfg.corpseExplosionDamage);
        }

        world.spawnParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5, pos.z,
                6, 0.5, 0.5, 0.5, 0);
        world.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y + 0.5, pos.z,
                20, 0.5, 0.5, 0.5, 0.05);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS, 0.9f, 1.1f);
    }

    private static void pruneMarks(ServerWorld world) {
        long now = world.getTime();
        Iterator<Map.Entry<UUID, MarkedTarget>> iter = MARKED.entrySet().iterator();
        while (iter.hasNext()) {
            MarkedTarget mark = iter.next().getValue();
            long markNow = mark.world.equals(world) ? now : mark.world.getTime();
            if (markNow > mark.expiresAt) {
                iter.remove();
            }
        }
        Iterator<Map.Entry<UUID, Long>> suppressIter = SUPPRESS_CHAIN_UNTIL.entrySet().iterator();
        while (suppressIter.hasNext()) {
            Map.Entry<UUID, Long> entry = suppressIter.next();
            if (entry.getValue() < now) {
                suppressIter.remove();
            }
        }
    }

    private static final class MarkedTarget {
        final ServerWorld world;
        final UUID ownerUuid;
        final long expiresAt;

        private MarkedTarget(ServerWorld world, UUID ownerUuid, long expiresAt) {
            this.world = world;
            this.ownerUuid = ownerUuid;
            this.expiresAt = expiresAt;
        }
    }
}
