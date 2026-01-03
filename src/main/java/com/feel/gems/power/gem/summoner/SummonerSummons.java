package com.feel.gems.power.gem.summoner;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import com.feel.gems.net.GemCooldownSync;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemAbilityCooldowns;
import com.feel.gems.legendary.HypnoControl;
import com.feel.gems.legendary.LegendaryTargeting;
import com.feel.gems.util.GemsNbt;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PiglinBruteEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.mob.HoglinEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;


public final class SummonerSummons {
    public static final String TAG_SUMMON = "gems_summon";
    private static final String TAG_OWNER_PREFIX = "gems_summon_owner:";
    private static final String TAG_UNTIL_PREFIX = "gems_summon_until:";

    private static final String KEY_SUMMONS = "summonerSummons";

    private static final Identifier BONUS_HEALTH_MODIFIER_ID = Identifier.of(GemsMod.MOD_ID, "summoner_bonus_health");

    private SummonerSummons() {
    }

    public static void mark(Entity entity, UUID owner, long untilTick) {
        entity.addCommandTag(TAG_SUMMON);
        entity.addCommandTag(TAG_OWNER_PREFIX + owner);
        entity.addCommandTag(TAG_UNTIL_PREFIX + untilTick);
    }

    public static boolean isSummon(Entity entity) {
        return entity.getCommandTags().contains(TAG_SUMMON);
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

    public static long untilTick(Entity entity) {
        Set<String> tags = entity.getCommandTags();
        for (String tag : tags) {
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

    public static void trackSpawn(ServerPlayerEntity owner, Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        NbtCompound root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        NbtList list = root.getListOrEmpty(KEY_SUMMONS);
        list.add(GemsNbt.fromUuid(entity.getUuid()));
        root.put(KEY_SUMMONS, list);
    }

    public static List<UUID> ownedSummonUuids(ServerPlayerEntity owner) {
        NbtCompound root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        NbtList list = root.getList(KEY_SUMMONS).orElse(null);
        if (list == null) {
            return List.of();
        }
        List<UUID> out = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            UUID uuid = GemsNbt.toUuid(list.get(i));
            if (uuid != null) {
                out.add(uuid);
            }
        }
        return out;
    }

    public record SummonStats(int count, int points) {
    }

    public static int pruneAndCount(ServerPlayerEntity owner) {
        MinecraftServer server = owner.getEntityWorld().getServer();
        if (server == null) {
            return 0;
        }
        NbtCompound root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        NbtList list = root.getListOrEmpty(KEY_SUMMONS);
        if (list.isEmpty()) {
            return 0;
        }
        NbtList next = new NbtList();
        int alive = 0;
        for (int i = 0; i < list.size(); i++) {
            UUID uuid = GemsNbt.toUuid(list.get(i));
            if (uuid == null) {
                continue;
            }
            Entity e = findEntity(server, uuid);
            if (e != null && e.isAlive() && isSummon(e) && owner.getUuid().equals(ownerUuid(e))) {
                next.add(GemsNbt.fromUuid(uuid));
                alive++;
            }
        }
        root.put(KEY_SUMMONS, next);
        return alive;
    }

    public static SummonStats pruneAndPoints(ServerPlayerEntity owner, GemsBalance.Summoner cfg) {
        MinecraftServer server = owner.getEntityWorld().getServer();
        if (server == null) {
            return new SummonStats(0, 0);
        }
        NbtCompound root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        NbtList list = root.getListOrEmpty(KEY_SUMMONS);
        if (list.isEmpty()) {
            return new SummonStats(0, 0);
        }
        NbtList next = new NbtList();
        int alive = 0;
        int points = 0;
        for (int i = 0; i < list.size(); i++) {
            UUID uuid = GemsNbt.toUuid(list.get(i));
            if (uuid == null) {
                continue;
            }
            Entity e = findEntity(server, uuid);
            if (!(e instanceof MobEntity mob) || !mob.isAlive() || !isSummon(mob)) {
                continue;
            }
            if (!owner.getUuid().equals(ownerUuid(mob))) {
                continue;
            }
            next.add(GemsNbt.fromUuid(uuid));
            alive++;
            points += costForEntity(cfg.costs(), mob.getType());
        }
        root.put(KEY_SUMMONS, next);
        return new SummonStats(alive, points);
    }

    public static void discardAll(ServerPlayerEntity owner) {
        MinecraftServer server = owner.getEntityWorld().getServer();
        if (server == null) {
            return;
        }
        for (UUID uuid : ownedSummonUuids(owner)) {
            Entity e = findEntity(server, uuid);
            if (e != null) {
                e.discard();
            }
        }
        ((GemsPersistentDataHolder) owner).gems$getPersistentData().remove(KEY_SUMMONS);
    }

    public static void commandSummons(ServerPlayerEntity owner, LivingEntity target, int rangeBlocks, int strengthAmplifier, int durationTicks) {
        MinecraftServer server = owner.getEntityWorld().getServer();
        if (server == null) {
            return;
        }
        if (target instanceof MobEntity mobTarget) {
            UUID targetOwner = ownerUuid(mobTarget);
            if (targetOwner != null && targetOwner.equals(owner.getUuid()) && isSummon(mobTarget)) {
                return;
            }
            UUID hypnoOwner = HypnoControl.ownerUuid(mobTarget);
            if (hypnoOwner != null && hypnoOwner.equals(owner.getUuid()) && HypnoControl.isHypno(mobTarget)) {
                return;
            }
        }
        double rangeSq = rangeBlocks * (double) rangeBlocks;
        for (UUID uuid : ownedSummonUuids(owner)) {
            Entity e = findEntity(server, uuid);
            if (!(e instanceof MobEntity mob) || !mob.isAlive() || !isSummon(mob)) {
                continue;
            }
            if (mob.getEntityWorld() != target.getEntityWorld()) {
                continue;
            }
            if (mob.squaredDistanceTo(owner) > rangeSq) {
                continue;
            }
            mob.setTarget(target);
            if (strengthAmplifier >= 0 && durationTicks > 0) {
                mob.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(net.minecraft.entity.effect.StatusEffects.STRENGTH, durationTicks, strengthAmplifier, true, false, false));
            }
        }
    }

    public static void followOwner(ServerPlayerEntity owner) {
        MinecraftServer server = owner.getEntityWorld().getServer();
        if (server == null) {
            return;
        }
        double followStart = GemsBalance.v().systems().controlledFollowStartBlocks();
        double followStop = GemsBalance.v().systems().controlledFollowStopBlocks();
        double followSpeed = GemsBalance.v().systems().controlledFollowSpeed();
        double followStartSq = followStart * followStart;
        double followStopSq = followStop * followStop;
        int rangeBlocks = GemsBalance.v().summoner().commandRangeBlocks();
        UUID markTarget = SummonerCommanderMark.activeTargetUuid(owner);
        Entity markEntity = markTarget == null ? null : findEntity(server, markTarget);
        LivingEntity markLiving = markEntity instanceof LivingEntity living ? living : null;
        LivingEntity fallbackTarget = markLiving == null ? LegendaryTargeting.findTarget(owner, rangeBlocks, 0) : null;
        if (fallbackTarget == null && markLiving == null) {
            fallbackTarget = findOwnerThreat(owner, rangeBlocks);
        }
        for (UUID uuid : ownedSummonUuids(owner)) {
            Entity e = findEntity(server, uuid);
            if (!(e instanceof MobEntity mob) || !mob.isAlive() || !isSummon(mob)) {
                continue;
            }
            refreshNetherMob(mob);
            if (mob.getEntityWorld() != owner.getEntityWorld()) {
                continue;
            }
            LivingEntity desiredTarget = markLiving != null ? markLiving : fallbackTarget;
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
            if (desiredTarget != null && desiredTarget.getEntityWorld() == mob.getEntityWorld()) {
                double rangeSq = rangeBlocks * (double) rangeBlocks;
                if (mob.squaredDistanceTo(owner) <= rangeSq) {
                    if (desiredTarget instanceof MobEntity mobTarget) {
                        UUID targetOwner = ownerUuid(mobTarget);
                        if (targetOwner != null && targetOwner.equals(owner.getUuid()) && isSummon(mobTarget)) {
                            continue;
                        }
                        UUID hypnoOwner = HypnoControl.ownerUuid(mobTarget);
                        if (hypnoOwner != null && hypnoOwner.equals(owner.getUuid()) && HypnoControl.isHypno(mobTarget)) {
                            continue;
                        }
                    }
                    mob.setTarget(desiredTarget);
                    continue;
                }
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
        List<MobEntity> mobs = owner.getEntityWorld().getEntitiesByClass(MobEntity.class, box, mob -> mob.getTarget() == owner);
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
        if (target == null || !target.isAlive() || target.getEntityWorld() != owner.getEntityWorld()) {
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
            if (targetOwner != null && targetOwner.equals(owner.getUuid()) && isSummon(mobTarget)) {
                return null;
            }
            UUID hypnoOwner = HypnoControl.ownerUuid(mobTarget);
            if (hypnoOwner != null && hypnoOwner.equals(owner.getUuid()) && HypnoControl.isHypno(mobTarget)) {
                return null;
            }
        }
        return target;
    }

    public static void applyBonusHealth(MobEntity mob, float bonusHealth) {
        if (bonusHealth <= 0.0F) {
            return;
        }
        var inst = mob.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (inst == null) {
            return;
        }
        inst.removeModifier(BONUS_HEALTH_MODIFIER_ID);
        inst.addPersistentModifier(new EntityAttributeModifier(BONUS_HEALTH_MODIFIER_ID, bonusHealth, EntityAttributeModifier.Operation.ADD_VALUE));
        mob.setHealth(Math.min(mob.getMaxHealth(), mob.getHealth() + bonusHealth));
    }

    public static void tuneControlledMob(MobEntity mob) {
        if (mob == null) {
            return;
        }
        mob.setTarget(null);
        mob.setAttacker(null);
        mob.setPersistent();
        refreshNetherMob(mob);
        clearAnger(mob);
        clearBrainAggro(mob);
        if (mob instanceof PiglinBruteEntity brute) {
            brute.setTarget(null);
            brute.setAttacker(null);
        }
    }

    public static void refreshNetherMob(MobEntity mob) {
        if (mob instanceof PiglinEntity piglin) {
            piglin.setImmuneToZombification(true);
        }
        if (mob instanceof HoglinEntity hoglin) {
            hoglin.setImmuneToZombification(true);
        }
        if (mob instanceof PiglinBruteEntity brute) {
            brute.setImmuneToZombification(true);
        }
    }

    public static void clearAnger(MobEntity mob) {
        if (mob instanceof Angerable angerable) {
            angerable.stopAnger();
        }
    }

    private static void clearBrainAggro(MobEntity mob) {
        var brain = mob.getBrain();
        brain.forget(MemoryModuleType.ATTACK_TARGET);
        brain.forget(MemoryModuleType.ANGRY_AT);
        brain.forget(MemoryModuleType.UNIVERSAL_ANGER);
        brain.forget(MemoryModuleType.NEAREST_ATTACKABLE);
        brain.forget(MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER);
        brain.forget(MemoryModuleType.HURT_BY);
        brain.forget(MemoryModuleType.HURT_BY_ENTITY);
    }

    public static Entity findEntity(MinecraftServer server, UUID uuid) {
        for (ServerWorld world : server.getWorlds()) {
            Entity e = world.getEntity(uuid);
            if (e != null) {
                return e;
            }
        }
        return null;
    }

    public static long computeUntilTick(ServerPlayerEntity owner, int lifetimeTicks) {
        if (lifetimeTicks <= 0) {
            return 0L;
        }
        return GemsTime.now(owner) + lifetimeTicks;
    }

    public static void applyCooldown(ServerPlayerEntity owner) {
        int cooldownTicks = GemsBalance.v().summoner().summonSlotCooldownTicks();
        if (cooldownTicks <= 0) {
            return;
        }
        long now = GemsTime.now(owner);
        long until = now + cooldownTicks;
        if (GemAbilityCooldowns.nextAllowedTick(owner, PowerIds.SUMMON_SLOT_1) > now) {
            return;
        }
        GemAbilityCooldowns.setNextAllowedTick(owner, PowerIds.SUMMON_SLOT_1, until);
        GemAbilityCooldowns.setNextAllowedTick(owner, PowerIds.SUMMON_SLOT_2, until);
        GemAbilityCooldowns.setNextAllowedTick(owner, PowerIds.SUMMON_SLOT_3, until);
        GemAbilityCooldowns.setNextAllowedTick(owner, PowerIds.SUMMON_SLOT_4, until);
        GemAbilityCooldowns.setNextAllowedTick(owner, PowerIds.SUMMON_SLOT_5, until);
        GemCooldownSync.send(owner);
    }

    public static int costForEntity(java.util.Map<String, Integer> costs, EntityType<?> type) {
        if (costs == null || costs.isEmpty() || type == null) {
            return 0;
        }
        Identifier id = Registries.ENTITY_TYPE.getId(type);
        if (id == null) {
            return 0;
        }
        Integer cost = costs.get(id.toString());
        return cost == null ? 0 : Math.max(0, cost);
    }
}

