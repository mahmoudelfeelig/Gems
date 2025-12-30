package com.feel.gems.power.gem.astra;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.HypnoControl;
import com.feel.gems.legendary.LegendaryTargeting;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.state.GemsPersistentDataHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;


public final class SoulSummons {
    public static final String TAG_SOUL = "gems_soul";
    private static final String TAG_OWNER_PREFIX = "gems_soul_owner:";
    private static final String KEY_SOUL_SUMMONS = "astraSoulSummons";

    private SoulSummons() {
    }

    public static void mark(Entity entity, UUID owner) {
        entity.addCommandTag(TAG_SOUL);
        entity.addCommandTag(TAG_OWNER_PREFIX + owner);
    }

    public static boolean isSoul(Entity entity) {
        return entity.getCommandTags().contains(TAG_SOUL);
    }

    public static UUID ownerUuid(Entity entity) {
        Set<String> tags = entity.getCommandTags();
        for (String tag : tags) {
            if (!tag.startsWith(TAG_OWNER_PREFIX)) {
                continue;
            }
            String raw = tag.substring(TAG_OWNER_PREFIX.length());
            try {
                return UUID.fromString(raw);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    public static void trackSpawn(ServerPlayerEntity owner, Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        NbtCompound root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        NbtList list = root.getList(KEY_SOUL_SUMMONS, NbtElement.INT_ARRAY_TYPE);
        list.add(NbtHelper.fromUuid(entity.getUuid()));
        root.put(KEY_SOUL_SUMMONS, list);
    }

    public static List<UUID> ownedSoulUuids(ServerPlayerEntity owner) {
        NbtCompound root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        if (!root.contains(KEY_SOUL_SUMMONS, NbtElement.LIST_TYPE)) {
            return List.of();
        }
        NbtList list = root.getList(KEY_SOUL_SUMMONS, NbtElement.INT_ARRAY_TYPE);
        List<UUID> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            out.add(NbtHelper.toUuid(list.get(i)));
        }
        return out;
    }

    public static int pruneAndCount(ServerPlayerEntity owner) {
        MinecraftServer server = owner.getServer();
        if (server == null) {
            return 0;
        }
        NbtCompound root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        NbtList list = root.getList(KEY_SOUL_SUMMONS, NbtElement.INT_ARRAY_TYPE);
        if (list.isEmpty()) {
            return 0;
        }
        NbtList next = new NbtList();
        int alive = 0;
        for (int i = 0; i < list.size(); i++) {
            UUID uuid = NbtHelper.toUuid(list.get(i));
            Entity e = SummonerSummons.findEntity(server, uuid);
            if (e != null && e.isAlive() && isSoul(e) && owner.getUuid().equals(ownerUuid(e))) {
                next.add(NbtHelper.fromUuid(uuid));
                alive++;
            }
        }
        root.put(KEY_SOUL_SUMMONS, next);
        return alive;
    }

    public static void followOwner(ServerPlayerEntity owner) {
        MinecraftServer server = owner.getServer();
        if (server == null) {
            return;
        }
        double followStart = GemsBalance.v().systems().controlledFollowStartBlocks();
        double followStop = GemsBalance.v().systems().controlledFollowStopBlocks();
        double followSpeed = GemsBalance.v().systems().controlledFollowSpeed();
        double followStartSq = followStart * followStart;
        double followStopSq = followStop * followStop;
        int rangeBlocks = GemsBalance.v().summoner().commandRangeBlocks();
        LivingEntity fallbackTarget = LegendaryTargeting.findTarget(owner, rangeBlocks, 0);
        if (fallbackTarget == null) {
            fallbackTarget = findOwnerThreat(owner, rangeBlocks);
        }
        for (UUID uuid : ownedSoulUuids(owner)) {
            Entity e = SummonerSummons.findEntity(server, uuid);
            if (!(e instanceof MobEntity mob) || !mob.isAlive() || !isSoul(mob)) {
                continue;
            }
            SummonerSummons.refreshNetherMob(mob);
            if (mob.getWorld() != owner.getWorld()) {
                continue;
            }
            LivingEntity desiredTarget = fallbackTarget;
            LivingEntity currentTarget = mob.getTarget();
            LivingEntity normalizedTarget = normalizeTarget(owner, currentTarget, rangeBlocks);
            if (currentTarget != null && (normalizedTarget == null || !currentTarget.isAlive())) {
                mob.setTarget(null);
                currentTarget = null;
                normalizedTarget = null;
            }
            if (normalizedTarget != null) {
                if (desiredTarget != null && normalizedTarget.getUuid().equals(desiredTarget.getUuid())) {
                    continue;
                }
                if (desiredTarget == null) {
                    mob.setTarget(null);
                } else {
                    mob.setTarget(desiredTarget);
                    continue;
                }
            }
            if (desiredTarget != null && desiredTarget.getWorld() == mob.getWorld()) {
                mob.setTarget(desiredTarget);
                continue;
            }
            double distSq = mob.squaredDistanceTo(owner);
            if (distSq > followStartSq) {
                mob.getNavigation().startMovingTo(owner, followSpeed);
            } else if (distSq < followStopSq) {
                mob.getNavigation().stop();
            }
        }
    }

    private static LivingEntity findOwnerThreat(ServerPlayerEntity owner, int rangeBlocks) {
        LivingEntity attacker = owner.getAttacker();
        LivingEntity candidate = normalizeTarget(owner, attacker, rangeBlocks);
        if (candidate != null) {
            return candidate;
        }
        if (rangeBlocks <= 0) {
            return null;
        }
        double maxSq = rangeBlocks * (double) rangeBlocks;
        var box = owner.getBoundingBox().expand(rangeBlocks);
        List<MobEntity> mobs = owner.getWorld().getEntitiesByClass(MobEntity.class, box, mob -> mob.getTarget() == owner);
        LivingEntity nearest = null;
        double nearestSq = Double.MAX_VALUE;
        for (MobEntity mob : mobs) {
            LivingEntity normalized = normalizeTarget(owner, mob, rangeBlocks);
            if (normalized == null) {
                continue;
            }
            double distSq = owner.squaredDistanceTo(mob);
            if (distSq > maxSq || distSq >= nearestSq) {
                continue;
            }
            nearestSq = distSq;
            nearest = mob;
        }
        return nearest;
    }

    private static LivingEntity normalizeTarget(ServerPlayerEntity owner, LivingEntity target, int rangeBlocks) {
        if (target == null || !target.isAlive() || target.getWorld() != owner.getWorld()) {
            return null;
        }
        if (rangeBlocks > 0) {
            double maxSq = rangeBlocks * (double) rangeBlocks;
            if (owner.squaredDistanceTo(target) > maxSq) {
                return null;
            }
        }
        if (target instanceof MobEntity mobTarget) {
            UUID targetOwner = ownerUuid(mobTarget);
            if (targetOwner != null && targetOwner.equals(owner.getUuid()) && isSoul(mobTarget)) {
                return null;
            }
            UUID summonOwner = SummonerSummons.ownerUuid(mobTarget);
            if (summonOwner != null && summonOwner.equals(owner.getUuid()) && SummonerSummons.isSummon(mobTarget)) {
                return null;
            }
            UUID hypnoOwner = HypnoControl.ownerUuid(mobTarget);
            if (hypnoOwner != null && hypnoOwner.equals(owner.getUuid()) && HypnoControl.isHypno(mobTarget)) {
                return null;
            }
        }
        return target;
    }
}

