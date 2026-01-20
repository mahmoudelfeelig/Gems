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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Void Rift - rift that damages enemies who touch it.
 */
public final class BonusVoidRiftAbility implements GemAbility {
    private static final List<Rift> RIFTS = new ArrayList<>();

    @Override
    public Identifier id() {
        return PowerIds.BONUS_VOID_RIFT;
    }

    @Override
    public String name() {
        return "Void Rift";
    }

    @Override
    public String description() {
        return "Tear open a rift that damages enemies who touch it.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().voidRiftCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }
        var cfg = GemsBalance.v().bonusPool();
        Vec3d pos = player.getEntityPos();
        long until = world.getTime() + cfg.voidRiftDurationSeconds * 20L;
        RIFTS.add(new Rift(world, pos, cfg.voidRiftRadiusBlocks, cfg.voidRiftDamagePerSecond, until, player));
        world.spawnParticles(ParticleTypes.PORTAL, pos.x, pos.y + 0.5, pos.z, 40, 0.6, 0.4, 0.6, 0.05);
        return true;
    }

    public static void tick(ServerWorld world) {
        if (RIFTS.isEmpty()) {
            return;
        }
        long now = world.getTime();
        Iterator<Rift> iter = RIFTS.iterator();
        while (iter.hasNext()) {
            Rift rift = iter.next();
            if (rift.world != world) {
                continue;
            }
            if (now > rift.until) {
                iter.remove();
                continue;
            }
            if (now % 20 != 0) {
                continue;
            }
            Box box = new Box(rift.pos, rift.pos).expand(rift.radius);
            List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, box, e -> e.isAlive());
            for (LivingEntity entity : entities) {
                if (entity instanceof ServerPlayerEntity other) {
                    if (GemTrust.isTrusted(rift.owner, other) || VoidImmunity.shouldBlockEffect(rift.owner, other)) {
                        continue;
                    }
                }
                entity.damage(world, rift.owner.getDamageSources().indirectMagic(rift.owner, rift.owner), rift.damagePerSecond);
            }
            world.spawnParticles(ParticleTypes.REVERSE_PORTAL, rift.pos.x, rift.pos.y + 0.2, rift.pos.z, 10, 0.4, 0.2, 0.4, 0.02);
        }
    }

    private record Rift(ServerWorld world, Vec3d pos, int radius, float damagePerSecond, long until, ServerPlayerEntity owner) {
    }
}
