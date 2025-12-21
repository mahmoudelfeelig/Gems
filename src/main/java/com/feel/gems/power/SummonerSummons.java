package com.feel.gems.power;

import com.feel.gems.GemsMod;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsTime;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
        NbtList list = root.getList(KEY_SUMMONS, NbtElement.INT_ARRAY_TYPE);
        list.add(NbtHelper.fromUuid(entity.getUuid()));
        root.put(KEY_SUMMONS, list);
    }

    public static List<UUID> ownedSummonUuids(ServerPlayerEntity owner) {
        NbtCompound root = ((GemsPersistentDataHolder) owner).gems$getPersistentData();
        if (!root.contains(KEY_SUMMONS, NbtElement.LIST_TYPE)) {
            return List.of();
        }
        NbtList list = root.getList(KEY_SUMMONS, NbtElement.INT_ARRAY_TYPE);
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
        NbtList list = root.getList(KEY_SUMMONS, NbtElement.INT_ARRAY_TYPE);
        if (list.isEmpty()) {
            return 0;
        }
        NbtList next = new NbtList();
        int alive = 0;
        for (int i = 0; i < list.size(); i++) {
            UUID uuid = NbtHelper.toUuid(list.get(i));
            Entity e = findEntity(server, uuid);
            if (e != null && e.isAlive() && isSummon(e) && owner.getUuid().equals(ownerUuid(e))) {
                next.add(NbtHelper.fromUuid(uuid));
                alive++;
            }
        }
        root.put(KEY_SUMMONS, next);
        return alive;
    }

    public static void discardAll(ServerPlayerEntity owner) {
        MinecraftServer server = owner.getServer();
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
        MinecraftServer server = owner.getServer();
        if (server == null) {
            return;
        }
        double rangeSq = rangeBlocks * (double) rangeBlocks;
        for (UUID uuid : ownedSummonUuids(owner)) {
            Entity e = findEntity(server, uuid);
            if (!(e instanceof MobEntity mob) || !mob.isAlive() || !isSummon(mob)) {
                continue;
            }
            if (mob.getWorld() != target.getWorld()) {
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

    public static void applyBonusHealth(MobEntity mob, float bonusHealth) {
        if (bonusHealth <= 0.0F) {
            return;
        }
        var inst = mob.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (inst == null) {
            return;
        }
        inst.removeModifier(BONUS_HEALTH_MODIFIER_ID);
        inst.addPersistentModifier(new EntityAttributeModifier(BONUS_HEALTH_MODIFIER_ID, bonusHealth, EntityAttributeModifier.Operation.ADD_VALUE));
        mob.setHealth(Math.min(mob.getMaxHealth(), mob.getHealth() + bonusHealth));
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
}

