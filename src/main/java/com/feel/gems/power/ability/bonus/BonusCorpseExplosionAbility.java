package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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
    private static final int CORPSE_LIFETIME_TICKS = 200; // 10 seconds
    private static final List<Corpse> CORPSES = new ArrayList<>();

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
        pruneCorpses(world);

        Box searchBox = player.getBoundingBox().expand(cfg.corpseExplosionCorpseRangeBlocks);
        List<Corpse> corpses = new ArrayList<>();
        for (Corpse corpse : CORPSES) {
            if (!corpse.world().equals(world)) {
                continue;
            }
            if (!searchBox.contains(corpse.pos())) {
                continue;
            }
            corpses.add(corpse);
        }

        if (corpses.isEmpty()) {
            return false;
        }

        for (Corpse corpse : corpses) {
            Vec3d pos = corpse.pos();
            Box explosionBox = new Box(pos, pos).expand(cfg.corpseExplosionRadiusBlocks);
            List<LivingEntity> nearby = world.getEntitiesByClass(LivingEntity.class, explosionBox,
                    e -> e != player && e.isAlive());

            for (LivingEntity entity : nearby) {
                if (entity instanceof ServerPlayerEntity other) {
                    if (GemTrust.isTrusted(player, other) || VoidImmunity.shouldBlockEffect(player, other)) {
                        continue;
                    }
                }
                entity.damage(world, world.getDamageSources().explosion(null, player), cfg.corpseExplosionDamage);
            }

            world.spawnParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5, pos.z,
                    5, 0.5, 0.5, 0.5, 0);
            world.spawnParticles(ParticleTypes.SOUL, pos.x, pos.y + 0.5, pos.z,
                    20, 0.5, 0.5, 0.5, 0.05);
            CORPSES.remove(corpse);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 1.2f);
        return true;
    }

    public static void recordCorpse(LivingEntity entity) {
        if (!(entity.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        long now = world.getTime();
        CORPSES.add(new Corpse(world, entity.getEntityPos(), now));
        pruneCorpses(world);
    }

    private static void pruneCorpses(ServerWorld world) {
        long now = world.getTime();
        Iterator<Corpse> iter = CORPSES.iterator();
        while (iter.hasNext()) {
            Corpse corpse = iter.next();
            long corpseNow = corpse.world().equals(world) ? now : corpse.world().getTime();
            if (corpseNow - corpse.time() > CORPSE_LIFETIME_TICKS) {
                iter.remove();
            }
        }
    }

    private record Corpse(ServerWorld world, Vec3d pos, long time) {
    }
}
