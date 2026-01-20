package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.gem.voidgem.VoidImmunity;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.trust.GemTrust;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Storm Call - summon a lightning storm in an area with random strikes.
 */
public final class BonusStormCallAbility implements GemAbility {
    private static final List<Storm> STORMS = new ArrayList<>();

    @Override
    public Identifier id() {
        return PowerIds.BONUS_STORM_CALL;
    }

    @Override
    public String name() {
        return "Storm Call";
    }

    @Override
    public String description() {
        return "Summon a lightning storm in an area with random strikes.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().stormCallCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }
        var cfg = GemsBalance.v().bonusPool();
        Vec3d center = player.getEntityPos();
        long until = world.getTime() + cfg.stormCallDurationSeconds * 20L;
        STORMS.add(new Storm(world, center, cfg.stormCallRadiusBlocks, cfg.stormCallDamagePerStrike,
                cfg.stormCallStrikesPerSecond, until, player));
        world.spawnParticles(ParticleTypes.CLOUD, center.x, center.y + 0.5, center.z, 20, 1.0, 0.3, 1.0, 0.02);
        return true;
    }

    public static void tick(ServerWorld world) {
        if (STORMS.isEmpty()) {
            return;
        }
        long now = world.getTime();
        Iterator<Storm> iter = STORMS.iterator();
        while (iter.hasNext()) {
            Storm storm = iter.next();
            if (storm.world != world) {
                continue;
            }
            if (now > storm.until) {
                iter.remove();
                continue;
            }
            if (now % 20 != 0) {
                continue;
            }
            int strikes = Math.max(1, storm.strikesPerSecond);
            for (int i = 0; i < strikes; i++) {
                double rx = (world.getRandom().nextDouble() * 2.0 - 1.0) * storm.radius;
                double rz = (world.getRandom().nextDouble() * 2.0 - 1.0) * storm.radius;
                Vec3d strike = storm.center.add(rx, 0.0, rz);
                LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(world, SpawnReason.TRIGGERED);
                if (lightning != null) {
                    lightning.refreshPositionAfterTeleport(strike.x, strike.y, strike.z);
                    world.spawnEntity(lightning);
                }
                Box box = new Box(strike, strike).expand(2.0);
                List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive());
                for (LivingEntity entity : entities) {
                    if (entity instanceof ServerPlayerEntity other) {
                        if (GemTrust.isTrusted(storm.owner, other) || VoidImmunity.shouldBlockEffect(storm.owner, other)) {
                            continue;
                        }
                    }
                    entity.damage(world, storm.owner.getDamageSources().indirectMagic(storm.owner, storm.owner), storm.damagePerStrike);
                }
            }
        }
    }

    private record Storm(ServerWorld world, Vec3d center, int radius, float damagePerStrike, int strikesPerSecond,
                         long until, ServerPlayerEntity owner) {
    }
}
