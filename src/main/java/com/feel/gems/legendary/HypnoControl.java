package com.feel.gems.legendary;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import com.feel.gems.util.MobBlacklist;
import java.util.ArrayList;
import java.util.List;
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
import com.feel.gems.trust.GemTrust;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.legendary.LegendaryTargeting;




public final class HypnoControl {
    public static final String TAG_HYPNO = "gems_hypno";
    private static final String TAG_OWNER_PREFIX = "gems_hypno_owner:";
    private static final String TAG_UNTIL_PREFIX = "gems_hypno_until:";
    private static final String KEY_HYPNO_MOBS = "legendaryHypnoMobs";
    private static final double FOLLOW_START_SQ = 36.0D;
    private static final double FOLLOW_STOP_SQ = 9.0D;
    private static final double FOLLOW_SPEED = 1.1D;

    private HypnoControl() {
    }

    public static boolean tryControl(ServerPlayerEntity owner, MobEntity mob) {
        if (!isAllowed(mob)) {
            return false;
        }
        UUID existing = ownerUuid(mob);
        if (existing != null && !existing.equals(owner.getUuid())) {
            return false;
        }
        int max = GemsBalance.v().legendary().hypnoMaxControlled();
        if (max > 0 && pruneAndCount(owner) >= max) {
            return false;
        }
        long now = GemsTime.now(owner);
        int duration = GemsBalance.v().legendary().hypnoDurationTicks();
        mark(mob, owner.getUuid(), duration > 0 ? now + duration : 0L);
        track(owner, mob);
        mob.setTarget(null);
        return true;
    }

    public static boolean isHypno(Entity entity) {
        return entity.getCommandTags().contains(TAG_HYPNO);
    }

    public static UUID ownerUuid(Entity entity) {
        for (String tag : entity.getCommandTags()) {
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

    public static long untilTick(Entity entity) {
        for (String tag : entity.getCommandTags()) {
            if (!tag.startsWith(TAG_UNTIL_PREFIX)) {
                continue;
            }
            String raw = tag.substring(TAG_UNTIL_PREFIX.length());
            try {
                return Long.parseLong(raw);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    public static int pruneAndCount(ServerPlayerEntity owner) {
        MinecraftServer server = owner.getServer();
        if (server == null) {
            return 0;
        }
        NbtCompound root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        NbtList list = root.getList(KEY_HYPNO_MOBS, NbtElement.INT_ARRAY_TYPE);
        if (list.isEmpty()) {
            return 0;
        }
        NbtList next = new NbtList();
        int alive = 0;
        long now = GemsTime.now(owner);
        for (int i = 0; i < list.size(); i++) {
            UUID uuid = NbtHelper.toUuid(list.get(i));
            Entity e = findEntity(server, uuid);
            if (!(e instanceof MobEntity mob) || !mob.isAlive() || !isHypno(mob)) {
                continue;
            }
            if (!owner.getUuid().equals(ownerUuid(mob))) {
                continue;
            }
            long until = untilTick(mob);
            if (until > 0 && now >= until) {
                release(mob);
                continue;
            }
            next.add(NbtHelper.fromUuid(uuid));
            alive++;
        }
        root.put(KEY_HYPNO_MOBS, next);
        return alive;
    }

    public static void releaseAll(ServerPlayerEntity owner) {
        MinecraftServer server = owner.getServer();
        if (server == null) {
            return;
        }
        for (UUID uuid : ownedMobUuids(owner)) {
            Entity e = findEntity(server, uuid);
            if (e instanceof MobEntity mob) {
                release(mob);
            }
        }
        ((GemsPersistentDataHolder) owner).gems$getPersistentData().remove(KEY_HYPNO_MOBS);
    }

    public static void followOwner(ServerPlayerEntity owner) {
        MinecraftServer server = owner.getServer();
        if (server == null) {
            return;
        }
        int rangeBlocks = GemsBalance.v().legendary().hypnoRangeBlocks();
        LivingEntity fallbackTarget = normalizeTarget(owner, LegendaryTargeting.findTarget(owner, rangeBlocks, 0), rangeBlocks);
        if (fallbackTarget == null) {
            fallbackTarget = findOwnerThreat(owner, rangeBlocks);
        }
        for (UUID uuid : ownedMobUuids(owner)) {
            Entity e = findEntity(server, uuid);
            if (!(e instanceof MobEntity mob) || !mob.isAlive() || !isHypno(mob)) {
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
        if (target instanceof ServerPlayerEntity playerTarget && GemTrust.isTrusted(owner, playerTarget)) {
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
            if (targetOwner != null && targetOwner.equals(owner.getUuid()) && isHypno(mobTarget)) {
                return null;
            }
            UUID summonOwner = SummonerSummons.ownerUuid(mobTarget);
            if (summonOwner != null && summonOwner.equals(owner.getUuid()) && SummonerSummons.isSummon(mobTarget)) {
                return null;
            }
        }
        return target;
    }

    public static void commandMobs(ServerPlayerEntity owner, LivingEntity target, int rangeBlocks) {
        MinecraftServer server = owner.getServer();
        if (server == null) {
            return;
        }
        if (rangeBlocks <= 0) {
            return;
        }
        if (target instanceof ServerPlayerEntity playerTarget && GemTrust.isTrusted(owner, playerTarget)) {
            return;
        }
        if (target instanceof MobEntity mobTarget) {
            UUID targetOwner = ownerUuid(mobTarget);
            if (targetOwner != null && targetOwner.equals(owner.getUuid()) && isHypno(mobTarget)) {
                return;
            }
            UUID summonOwner = SummonerSummons.ownerUuid(mobTarget);
            if (summonOwner != null && summonOwner.equals(owner.getUuid()) && SummonerSummons.isSummon(mobTarget)) {
                return;
            }
        }
        double rangeSq = rangeBlocks * (double) rangeBlocks;
        for (UUID uuid : ownedMobUuids(owner)) {
            Entity e = findEntity(server, uuid);
            if (!(e instanceof MobEntity mob) || !mob.isAlive() || !isHypno(mob)) {
                continue;
            }
            if (mob.getWorld() != target.getWorld()) {
                continue;
            }
            if (mob.squaredDistanceTo(owner) > rangeSq) {
                continue;
            }
            mob.setTarget(target);
        }
    }

    public static boolean isAllowed(MobEntity mob) {
        return !MobBlacklist.isBlacklisted(mob);
    }

    private static void mark(MobEntity mob, UUID owner, long untilTick) {
        List<String> tags = List.copyOf(mob.getCommandTags());
        for (String tag : tags) {
            if (tag.startsWith(TAG_OWNER_PREFIX) || tag.startsWith(TAG_UNTIL_PREFIX)) {
                mob.removeCommandTag(tag);
            }
        }
        mob.addCommandTag(TAG_HYPNO);
        mob.addCommandTag(TAG_OWNER_PREFIX + owner);
        if (untilTick > 0) {
            mob.addCommandTag(TAG_UNTIL_PREFIX + untilTick);
        }
    }

    private static void release(MobEntity mob) {
        List<String> tags = List.copyOf(mob.getCommandTags());
        for (String tag : tags) {
            if (tag.equals(TAG_HYPNO) || tag.startsWith(TAG_OWNER_PREFIX) || tag.startsWith(TAG_UNTIL_PREFIX)) {
                mob.removeCommandTag(tag);
            }
        }
    }

    private static void track(ServerPlayerEntity owner, MobEntity mob) {
        NbtCompound root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        NbtList list = root.getList(KEY_HYPNO_MOBS, NbtElement.INT_ARRAY_TYPE);
        list.add(NbtHelper.fromUuid(mob.getUuid()));
        root.put(KEY_HYPNO_MOBS, list);
    }

    private static List<UUID> ownedMobUuids(ServerPlayerEntity owner) {
        NbtCompound root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        if (!root.contains(KEY_HYPNO_MOBS, NbtElement.LIST_TYPE)) {
            return List.of();
        }
        NbtList list = root.getList(KEY_HYPNO_MOBS, NbtElement.INT_ARRAY_TYPE);
        List<UUID> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            out.add(NbtHelper.toUuid(list.get(i)));
        }
        return out;
    }

    private static Entity findEntity(MinecraftServer server, UUID uuid) {
        for (ServerWorld world : server.getWorlds()) {
            Entity e = world.getEntity(uuid);
            if (e != null) {
                return e;
            }
        }
        return null;
    }
}
