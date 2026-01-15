package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.entity.ModEntities;
import com.feel.gems.entity.ShadowCloneEntity;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

/**
 * Phantasm - create a decoy that taunts enemies and explodes on death.
 */
public final class BonusPhantasmAbility implements GemAbility {
    private static final Map<UUID, Phantasm> PHANTASMS = new HashMap<>();

    @Override
    public Identifier id() {
        return PowerIds.BONUS_PHANTASM;
    }

    @Override
    public String name() {
        return "Phantasm";
    }

    @Override
    public String description() {
        return "Create a decoy that taunts enemies and explodes on death.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().bonusPool().phantasmCooldownSeconds * 20;
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return false;
        }
        Entity entity = ModEntities.SHADOW_CLONE.create(world, SpawnReason.MOB_SUMMONED);
        if (!(entity instanceof ShadowCloneEntity clone)) {
            return false;
        }
        clone.setOwner(player);
        int duration = GemsBalance.v().bonusPool().phantasmDurationSeconds * 20;
        clone.setMaxLifetime(duration);
        clone.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0.0F);
        world.spawnEntity(clone);

        Box box = clone.getBoundingBox().expand(12);
        for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, box, m -> m.isAlive())) {
            mob.setTarget(clone);
        }
        PHANTASMS.put(clone.getUuid(), new Phantasm(world, clone.getUuid(), player.getUuid(), world.getTime() + duration));
        world.spawnParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 15, 0.5, 0.4, 0.5, 0.02);
        return true;
    }

    public static void tick(ServerWorld world) {
        if (PHANTASMS.isEmpty()) {
            return;
        }
        long now = world.getTime();
        Iterator<Map.Entry<UUID, Phantasm>> iter = PHANTASMS.entrySet().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            Phantasm phantom = entry.getValue();
            if (phantom.world != world) {
                continue;
            }
            if (now <= phantom.until) {
                continue;
            }
            Entity clone = world.getEntity(phantom.cloneUuid);
            if (clone != null) {
                float power = GemsBalance.v().bonusPool().phantasmExplosionDamage;
                world.createExplosion(clone, clone.getX(), clone.getY(), clone.getZ(), power, ServerWorld.ExplosionSourceType.MOB);
                clone.discard();
            }
            iter.remove();
        }
    }

    private record Phantasm(ServerWorld world, UUID cloneUuid, UUID ownerUuid, long until) {
    }
}
