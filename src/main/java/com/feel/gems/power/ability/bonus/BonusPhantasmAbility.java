package com.feel.gems.power.ability.bonus;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.entity.HunterPackEntity;
import com.feel.gems.entity.ModEntities;
import com.feel.gems.net.payloads.ShadowCloneSyncPayload;
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
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

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
        Entity entity = ModEntities.HUNTER_PACK.create(world, SpawnReason.MOB_SUMMONED);
        if (!(entity instanceof HunterPackEntity clone)) {
            return false;
        }
        UUID packId = UUID.randomUUID();
        clone.setOwner(player, packId);
        int duration = GemsBalance.v().bonusPool().phantasmDurationSeconds * 20;
        clone.setMaxLifetime(duration);
        clone.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0.0F);
        world.spawnEntity(clone);

        Box box = clone.getBoundingBox().expand(12);
        for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, box, m -> m.isAlive())) {
            mob.setTarget(clone);
        }
        PHANTASMS.put(clone.getUuid(), new Phantasm(world, clone.getUuid(), player.getUuid(), world.getTime() + duration, clone.getEntityPos()));
        world.spawnParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 15, 0.5, 0.4, 0.5, 0.02);

        ShadowCloneSyncPayload syncPayload = new ShadowCloneSyncPayload(
                clone.getId(),
                player.getUuid(),
                player.getGameProfile().name()
        );
        for (ServerPlayerEntity tracker : PlayerLookup.tracking(clone)) {
            ServerPlayNetworking.send(tracker, syncPayload);
        }
        ServerPlayNetworking.send(player, syncPayload);
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
            Entity clone = world.getEntity(phantom.cloneUuid);
            if (clone != null) {
                phantom.lastPos = clone.getEntityPos();
            }
            boolean expired = now > phantom.until;
            boolean dead = clone == null || !clone.isAlive();
            if (!expired && !dead) {
                continue;
            }
            explode(phantom, clone);
            iter.remove();
        }
    }

    private static void explode(Phantasm phantom, Entity clone) {
        ServerWorld world = phantom.world;
        Vec3d pos = clone != null ? clone.getEntityPos() : phantom.lastPos;
        if (pos == null) {
            return;
        }
        float power = GemsBalance.v().bonusPool().phantasmExplosionDamage;
        world.createExplosion(clone, pos.x, pos.y, pos.z, power, ServerWorld.ExplosionSourceType.MOB);
        if (clone != null) {
            clone.discard();
        }
        clearTargets(world, phantom.cloneUuid, pos);
    }

    private static void clearTargets(ServerWorld world, UUID cloneUuid, Vec3d center) {
        Box box = new Box(center, center).expand(32.0D);
        for (MobEntity mob : world.getEntitiesByClass(MobEntity.class, box, m -> m.getTarget() != null)) {
            Entity target = mob.getTarget();
            if (target != null && cloneUuid.equals(target.getUuid())) {
                mob.setTarget(null);
            }
        }
    }

    private static final class Phantasm {
        final ServerWorld world;
        final UUID cloneUuid;
        final UUID ownerUuid;
        final long until;
        Vec3d lastPos;

        private Phantasm(ServerWorld world, UUID cloneUuid, UUID ownerUuid, long until, Vec3d lastPos) {
            this.world = world;
            this.cloneUuid = cloneUuid;
            this.ownerUuid = ownerUuid;
            this.until = until;
            this.lastPos = lastPos;
        }
    }
}
