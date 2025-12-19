package com.blissmc.gems.power;

import com.blissmc.gems.state.GemPlayerState;
import com.blissmc.gems.state.GemsPersistentDataHolder;
import com.blissmc.gems.trust.GemTrust;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameMode;

import java.util.UUID;

public final class AbilityRuntime {
    private static final String KEY_CAMPFIRE_UNTIL = "cosyCampfireUntil";
    private static final String KEY_CRISP_UNTIL = "crispUntil";
    private static final String KEY_SPEED_STORM_UNTIL = "speedStormUntil";

    private static final String KEY_UNBOUNDED_UNTIL = "unboundedUntil";
    private static final String KEY_UNBOUNDED_ALLOW_FLY = "unboundedPrevAllowFly";
    private static final String KEY_UNBOUNDED_FLYING = "unboundedPrevFlying";

    private static final String KEY_PROJECTION_UNTIL = "projectionUntil";
    private static final String KEY_PROJECTION_PREV_GAMEMODE = "projectionPrevGamemode";

    private static final String KEY_LIFE_CIRCLE_UNTIL = "lifeCircleUntil";
    private static final String KEY_LIFE_CIRCLE_CASTER = "lifeCircleCaster";

    private static final String KEY_HEART_LOCK_UNTIL = "heartLockUntil";
    private static final String KEY_HEART_LOCK_CASTER = "heartLockCaster";

    private static final String KEY_BOUNTY_UNTIL = "bountyUntil";
    private static final String KEY_BOUNTY_TARGET = "bountyTarget";

    private static final String KEY_CHAD_UNTIL = "chadUntil";
    private static final String KEY_CHAD_HITS = "chadHits";

    private static final String KEY_RICH_RUSH_UNTIL = "richRushUntil";

    private static final String CUSTOM_DATA_KEY_AMPLIFY = "gemsAmplify";
    private static final String CUSTOM_DATA_KEY_OWNER = "gemsOwner";

    private AbilityRuntime() {
    }

    public static void tickEverySecond(ServerPlayerEntity player) {
        long now = player.getServerWorld().getTime();

        tickCosyCampfire(player, now);
        tickCrisp(player, now);
        tickSpeedStorm(player, now);
        tickLifeCircle(player, now);
        tickHeartLock(player, now);
        tickUnbounded(player, now);
        tickProjection(player, now);
        tickBounty(player, now);
        tickAmplificationCleanup(player, now);
    }

    public static void startCosyCampfire(ServerPlayerEntity player, int durationTicks) {
        persistent(player).putLong(KEY_CAMPFIRE_UNTIL, player.getServerWorld().getTime() + durationTicks);
    }

    public static void startCrisp(ServerPlayerEntity player, int durationTicks) {
        persistent(player).putLong(KEY_CRISP_UNTIL, player.getServerWorld().getTime() + durationTicks);
    }

    public static void startSpeedStorm(ServerPlayerEntity player, int durationTicks) {
        persistent(player).putLong(KEY_SPEED_STORM_UNTIL, player.getServerWorld().getTime() + durationTicks);
    }

    public static void startUnbounded(ServerPlayerEntity player, int durationTicks) {
        NbtCompound nbt = persistent(player);
        nbt.putLong(KEY_UNBOUNDED_UNTIL, player.getServerWorld().getTime() + durationTicks);
        nbt.putBoolean(KEY_UNBOUNDED_ALLOW_FLY, player.getAbilities().allowFlying);
        nbt.putBoolean(KEY_UNBOUNDED_FLYING, player.getAbilities().flying);

        player.getAbilities().allowFlying = true;
        player.getAbilities().flying = true;
        player.sendAbilitiesUpdate();
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, durationTicks, 0, true, false, false));
    }

    public static void startProjection(ServerPlayerEntity player, int durationTicks) {
        NbtCompound nbt = persistent(player);
        nbt.putLong(KEY_PROJECTION_UNTIL, player.getServerWorld().getTime() + durationTicks);
        nbt.putString(KEY_PROJECTION_PREV_GAMEMODE, player.interactionManager.getGameMode().getName());
        player.changeGameMode(GameMode.SPECTATOR);
    }

    public static void startLifeCircle(ServerPlayerEntity caster, int durationTicks) {
        NbtCompound nbt = persistent(caster);
        nbt.putLong(KEY_LIFE_CIRCLE_UNTIL, caster.getServerWorld().getTime() + durationTicks);
        nbt.putUuid(KEY_LIFE_CIRCLE_CASTER, caster.getUuid());
    }

    public static void startHeartLock(ServerPlayerEntity caster, ServerPlayerEntity target, int durationTicks) {
        NbtCompound nbt = persistent(target);
        nbt.putLong(KEY_HEART_LOCK_UNTIL, target.getServerWorld().getTime() + durationTicks);
        nbt.putUuid(KEY_HEART_LOCK_CASTER, caster.getUuid());

        double currentMax = target.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
        double desiredMax = Math.max(2.0D, target.getHealth());
        double delta = desiredMax - currentMax;
        Identifier modifierId = heartLockModifierId(caster.getUuid());
        applyMaxHealthModifier(target, modifierId, delta);
    }

    public static void startBounty(ServerPlayerEntity hunter, UUID target, int durationTicks) {
        NbtCompound nbt = persistent(hunter);
        nbt.putLong(KEY_BOUNTY_UNTIL, hunter.getServerWorld().getTime() + durationTicks);
        nbt.putUuid(KEY_BOUNTY_TARGET, target);
    }

    public static void startChadStrength(ServerPlayerEntity player, int durationTicks) {
        NbtCompound nbt = persistent(player);
        nbt.putLong(KEY_CHAD_UNTIL, player.getServerWorld().getTime() + durationTicks);
        nbt.putInt(KEY_CHAD_HITS, 0);
    }

    public static boolean isChadStrengthActive(ServerPlayerEntity player) {
        long now = player.getServerWorld().getTime();
        return persistent(player).getLong(KEY_CHAD_UNTIL) > now;
    }

    public static int incrementChadHit(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        int next = nbt.getInt(KEY_CHAD_HITS) + 1;
        nbt.putInt(KEY_CHAD_HITS, next);
        return next;
    }

    public static void startRichRush(ServerPlayerEntity player, int durationTicks) {
        persistent(player).putLong(KEY_RICH_RUSH_UNTIL, player.getServerWorld().getTime() + durationTicks);
    }

    public static boolean isRichRushActive(ServerPlayerEntity player) {
        long now = player.getServerWorld().getTime();
        return persistent(player).getLong(KEY_RICH_RUSH_UNTIL) > now;
    }

    public static void setOwnerIfMissing(ItemStack stack, UUID owner) {
        if (stack.isEmpty()) {
            return;
        }
        NbtComponent existing = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (existing != null && existing.getNbt().contains(CUSTOM_DATA_KEY_OWNER, NbtElement.INT_ARRAY_TYPE)) {
            return;
        }
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.put(CUSTOM_DATA_KEY_OWNER, NbtHelper.fromUuid(owner)));
    }

    public static UUID getOwner(ItemStack stack) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) {
            return null;
        }
        NbtCompound nbt = custom.getNbt();
        if (!nbt.contains(CUSTOM_DATA_KEY_OWNER, NbtElement.INT_ARRAY_TYPE)) {
            return null;
        }
        return NbtHelper.toUuid(nbt.get(CUSTOM_DATA_KEY_OWNER));
    }

    private static void tickCosyCampfire(ServerPlayerEntity player, long now) {
        if (persistent(player).getLong(KEY_CAMPFIRE_UNTIL) <= now) {
            return;
        }
        ServerWorld world = player.getServerWorld();
        Box box = new Box(player.getBlockPos()).expand(8.0D);
        for (ServerPlayerEntity other : world.getPlayers(p -> p.squaredDistanceTo(player) <= 8.0D * 8.0D)) {
            if (GemTrust.isTrusted(player, other)) {
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, 3, true, false, false));
            }
        }
    }

    private static void tickCrisp(ServerPlayerEntity player, long now) {
        if (persistent(player).getLong(KEY_CRISP_UNTIL) <= now) {
            return;
        }
        FireCrisp.apply(player.getServerWorld(), player.getBlockPos(), 8);
    }

    private static void tickSpeedStorm(ServerPlayerEntity player, long now) {
        if (persistent(player).getLong(KEY_SPEED_STORM_UNTIL) <= now) {
            return;
        }
        ServerWorld world = player.getServerWorld();
        for (ServerPlayerEntity other : world.getPlayers(p -> p.squaredDistanceTo(player) <= 10.0D * 10.0D)) {
            if (GemTrust.isTrusted(player, other)) {
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 1, true, false, false));
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 40, 1, true, false, false));
            } else {
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 6, true, false, false));
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 40, 2, true, false, false));
            }
        }
    }

    private static void tickLifeCircle(ServerPlayerEntity caster, long now) {
        NbtCompound nbt = persistent(caster);
        long until = nbt.getLong(KEY_LIFE_CIRCLE_UNTIL);
        if (until <= 0) {
            return;
        }
        UUID casterId = nbt.contains(KEY_LIFE_CIRCLE_CASTER, NbtElement.INT_ARRAY_TYPE) ? nbt.getUuid(KEY_LIFE_CIRCLE_CASTER) : caster.getUuid();
        Identifier bonusId = lifeCircleBonusId(casterId);
        Identifier penaltyId = lifeCirclePenaltyId(casterId);

        ServerWorld world = caster.getServerWorld();

        if (until <= now) {
            for (ServerPlayerEntity p : world.getPlayers()) {
                removeMaxHealthModifier(p, bonusId);
                removeMaxHealthModifier(p, penaltyId);
            }
            nbt.remove(KEY_LIFE_CIRCLE_UNTIL);
            nbt.remove(KEY_LIFE_CIRCLE_CASTER);
            return;
        }

        for (ServerPlayerEntity other : world.getPlayers()) {
            double distSq = other.squaredDistanceTo(caster);
            boolean inRange = distSq <= 8.0D * 8.0D;
            if (!inRange) {
                removeMaxHealthModifier(other, bonusId);
                removeMaxHealthModifier(other, penaltyId);
                continue;
            }

            if (GemTrust.isTrusted(caster, other)) {
                applyMaxHealthModifier(other, bonusId, 8.0D);
                removeMaxHealthModifier(other, penaltyId);
            } else {
                applyMaxHealthModifier(other, penaltyId, -8.0D);
                removeMaxHealthModifier(other, bonusId);
            }
        }
    }

    private static void tickHeartLock(ServerPlayerEntity target, long now) {
        NbtCompound nbt = persistent(target);
        long until = nbt.getLong(KEY_HEART_LOCK_UNTIL);
        if (until <= 0) {
            return;
        }
        UUID caster = nbt.getUuid(KEY_HEART_LOCK_CASTER);
        Identifier modifierId = heartLockModifierId(caster);
        if (until <= now) {
            removeMaxHealthModifier(target, modifierId);
            nbt.remove(KEY_HEART_LOCK_UNTIL);
            nbt.remove(KEY_HEART_LOCK_CASTER);
            return;
        }

        double currentMax = target.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
        double desiredMax = Math.max(2.0D, target.getHealth());
        double delta = desiredMax - currentMax;
        applyMaxHealthModifier(target, modifierId, delta);
    }

    private static void tickUnbounded(ServerPlayerEntity player, long now) {
        NbtCompound nbt = persistent(player);
        long until = nbt.getLong(KEY_UNBOUNDED_UNTIL);
        if (until <= 0 || until > now) {
            return;
        }
        boolean prevAllow = nbt.getBoolean(KEY_UNBOUNDED_ALLOW_FLY);
        boolean prevFlying = nbt.getBoolean(KEY_UNBOUNDED_FLYING);
        player.getAbilities().allowFlying = prevAllow;
        player.getAbilities().flying = prevFlying && prevAllow;
        player.sendAbilitiesUpdate();
        nbt.remove(KEY_UNBOUNDED_UNTIL);
        nbt.remove(KEY_UNBOUNDED_ALLOW_FLY);
        nbt.remove(KEY_UNBOUNDED_FLYING);
    }

    private static void tickProjection(ServerPlayerEntity player, long now) {
        NbtCompound nbt = persistent(player);
        long until = nbt.getLong(KEY_PROJECTION_UNTIL);
        if (until <= 0 || until > now) {
            return;
        }
        String name = nbt.getString(KEY_PROJECTION_PREV_GAMEMODE);
        GameMode prev = parseGameMode(name);
        if (prev != null) {
            player.changeGameMode(prev);
        }
        nbt.remove(KEY_PROJECTION_UNTIL);
        nbt.remove(KEY_PROJECTION_PREV_GAMEMODE);
    }

    private static void tickBounty(ServerPlayerEntity hunter, long now) {
        NbtCompound nbt = persistent(hunter);
        long until = nbt.getLong(KEY_BOUNTY_UNTIL);
        if (until <= 0) {
            return;
        }
        if (until <= now) {
            nbt.remove(KEY_BOUNTY_UNTIL);
            nbt.remove(KEY_BOUNTY_TARGET);
            return;
        }
        if (!nbt.contains(KEY_BOUNTY_TARGET, NbtElement.INT_ARRAY_TYPE)) {
            return;
        }
        UUID targetUuid = nbt.getUuid(KEY_BOUNTY_TARGET);
        ServerPlayerEntity target = hunter.getServer().getPlayerManager().getPlayer(targetUuid);
        if (target == null) {
            hunter.sendMessage(Text.literal("Bounty: target offline"), true);
            return;
        }
        double dist = Math.sqrt(hunter.squaredDistanceTo(target));
        hunter.sendMessage(Text.literal("Bounty: " + target.getName().getString() + " (" + (int) dist + "m)"), true);
    }

    private static void tickAmplificationCleanup(ServerPlayerEntity player, long now) {
        for (ItemStack stack : player.getInventory().main) {
            restoreAmplifiedIfExpired(player.getServerWorld(), stack, now);
        }
        for (ItemStack stack : player.getInventory().offHand) {
            restoreAmplifiedIfExpired(player.getServerWorld(), stack, now);
        }
        for (ItemStack stack : player.getInventory().armor) {
            restoreAmplifiedIfExpired(player.getServerWorld(), stack, now);
        }
    }

    private static void restoreAmplifiedIfExpired(ServerWorld world, ItemStack stack, long now) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) {
            return;
        }
        NbtCompound nbt = custom.getNbt();
        if (!nbt.contains(CUSTOM_DATA_KEY_AMPLIFY, NbtElement.COMPOUND_TYPE)) {
            return;
        }
        NbtCompound marker = nbt.getCompound(CUSTOM_DATA_KEY_AMPLIFY);
        long until = marker.getLong("until");
        if (until > now) {
            return;
        }

        if (marker.contains("enchants", NbtElement.LIST_TYPE)) {
            NbtList list = marker.getList("enchants", NbtElement.COMPOUND_TYPE);
            EnchantmentAmplification.restoreFromList(world, stack, list);
        }

        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, data -> data.remove(CUSTOM_DATA_KEY_AMPLIFY));
    }

    private static void applyMaxHealthModifier(ServerPlayerEntity player, Identifier id, double deltaHealth) {
        EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        maxHealth.removeModifier(id);
        if (deltaHealth != 0.0D) {
            maxHealth.addPersistentModifier(new EntityAttributeModifier(id, deltaHealth, EntityAttributeModifier.Operation.ADD_VALUE));
        }
        float newMax = (float) maxHealth.getValue();
        if (player.getHealth() > newMax) {
            player.setHealth(newMax);
        }
    }

    private static void removeMaxHealthModifier(ServerPlayerEntity player, Identifier id) {
        EntityAttributeInstance maxHealth = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        maxHealth.removeModifier(id);
        float newMax = (float) maxHealth.getValue();
        if (player.getHealth() > newMax) {
            player.setHealth(newMax);
        }
    }

    private static Identifier lifeCircleBonusId(UUID caster) {
        return Identifier.of("gems", "life_circle_bonus_" + caster);
    }

    private static Identifier lifeCirclePenaltyId(UUID caster) {
        return Identifier.of("gems", "life_circle_penalty_" + caster);
    }

    private static Identifier heartLockModifierId(UUID caster) {
        return Identifier.of("gems", "heart_lock_" + caster);
    }

    private static GameMode parseGameMode(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (GameMode mode : GameMode.values()) {
            if (mode.getName().equals(name)) {
                return mode;
            }
        }
        return null;
    }

    private static NbtCompound persistent(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}

