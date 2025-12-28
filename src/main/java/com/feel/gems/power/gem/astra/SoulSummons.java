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
    private static final double FOLLOW_START_SQ = 36.0D;
    private static final double FOLLOW_STOP_SQ = 9.0D;
    private static final double FOLLOW_SPEED = 1.1D;

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
            if (mob.getWorld() != owner.getWorld()) {
                continue;
            }
            LivingEntity currentTarget = mob.getTarget();
            if (currentTarget != null && currentTarget.isAlive()) {
                continue;
            }
            if (currentTarget != null && !currentTarget.isAlive()) {
                mob.setTarget(null);
            }
            if (mob.getAttacker() != null) {
                continue;
            }
            if (fallbackTarget != null && fallbackTarget.getWorld() == mob.getWorld()) {
                mob.setTarget(fallbackTarget);
                continue;
            }
            double distSq = mob.squaredDistanceTo(owner);
            if (distSq > FOLLOW_START_SQ) {
                mob.getNavigation().startMovingTo(owner, FOLLOW_SPEED);
            } else if (distSq < FOLLOW_STOP_SQ) {
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

