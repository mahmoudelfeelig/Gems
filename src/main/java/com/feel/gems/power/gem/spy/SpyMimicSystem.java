package com.feel.gems.power.gem.spy;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.core.GemId;
import com.feel.gems.net.SpySkinshiftPayload;
import com.feel.gems.power.gem.astra.SoulSummons;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityDisables;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.trust.GemTrust;
import com.feel.gems.util.GemsNbt;
import com.feel.gems.util.GemsTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;


public final class SpyMimicSystem {
    private static final String KEY_DEATHS = "gemsDeathCount";

    private static final String KEY_LAST_KILLED_TYPE = "spyLastKilledType";

    private static final String KEY_LAST_SEEN_ABILITY = "spyLastSeenAbility";
    private static final String KEY_LAST_SEEN_AT = "spyLastSeenAt";
    private static final String KEY_LAST_SEEN_CASTER = "spyLastSeenCaster";
    private static final String KEY_LAST_SEEN_CASTER_ENTITY_ID = "spyLastSeenCasterEntityId";
    private static final String KEY_OBSERVED = "spyObserved";

    private static final String KEY_STOLEN = "spyStolen";
    private static final String KEY_STOLEN_SELECTED = "spyStolenSelected";
    private static final String KEY_STOLEN_BY = "spyStolenBy";

    private static final String KEY_MIMIC_UNTIL = "spyMimicUntil";
    private static final String KEY_SKINSHIFT_UNTIL = "spySkinshiftUntil";
    private static final String KEY_SKINSHIFT_TARGET = "spySkinshiftTarget";
    private static final String KEY_SKINSHIFT_NAME = "spySkinshiftName";

    private static final String KEY_STILL_LAST_X = "spyStillX";
    private static final String KEY_STILL_LAST_Y = "spyStillY";
    private static final String KEY_STILL_LAST_Z = "spyStillZ";
    private static final String KEY_STILL_LAST_MOVED = "spyStillLastMoved";

    private static final Identifier MOD_MIMIC_MAX_HEALTH = Identifier.of("gems", "spy_mimic_max_health");
    private static final Identifier MOD_MIMIC_SPEED = Identifier.of("gems", "spy_mimic_speed");

    // Cache of online players currently eligible for Spy/Mimic logic to avoid scanning everyone on each observed ability use.
    private static final Set<UUID> ACTIVE_SPIES = new HashSet<>();

    private SpyMimicSystem() {
    }

    public static void incrementDeaths(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        int deaths = nbt.getInt(KEY_DEATHS, 0);
        nbt.putInt(KEY_DEATHS, deaths + 1);

        // Dying clears observation progress windows.
        nbt.remove(KEY_LAST_SEEN_ABILITY);
        nbt.remove(KEY_LAST_SEEN_AT);
        nbt.remove(KEY_LAST_SEEN_CASTER);
        nbt.remove(KEY_LAST_SEEN_CASTER_ENTITY_ID);
        nbt.remove(KEY_OBSERVED);
    }

    public static int deaths(ServerPlayerEntity player) {
        return persistent(player).getInt(KEY_DEATHS, 0);
    }

    public static void recordLastKilledMob(ServerPlayerEntity killer, LivingEntity killed) {
        if (killed instanceof ServerPlayerEntity) {
            return;
        }
        if (SoulSummons.isSoul(killed) || SummonerSummons.isSummon(killed)) {
            return;
        }
        Identifier id = Registries.ENTITY_TYPE.getId(killed.getType());
        if (id == null) {
            return;
        }
        persistent(killer).putString(KEY_LAST_KILLED_TYPE, id.toString());
    }

    public static Identifier lastKilledType(ServerPlayerEntity player) {
        String raw = persistent(player).getString(KEY_LAST_KILLED_TYPE, "");
        return Identifier.tryParse(raw);
    }

    public static void onAbilityUsed(MinecraftServer server, ServerPlayerEntity caster, Identifier abilityId) {
        if (server == null) {
            return;
        }
        long now = GemsTime.now(caster);
        int range = GemsBalance.v().spyMimic().observeRangeBlocks();
        double rangeSq = range * (double) range;
        int window = GemsBalance.v().spyMimic().observeWindowTicks();

        // If the cache is empty, do a full scan so cold starts still record observations.
        if (ACTIVE_SPIES.isEmpty()) {
            for (ServerPlayerEntity spy : server.getPlayerManager().getPlayerList()) {
                if (spy == caster) {
                    continue;
                }
                if (observeIfEligible(spy, caster, abilityId, now, rangeSq, window)) {
                    ACTIVE_SPIES.add(spy.getUuid());
                }
            }
            return;
        }

        // Maintain the cache: prune stale entries, add newly eligible spies, and fallback to a full scan if everything expired.
        var iter = ACTIVE_SPIES.iterator();
        while (iter.hasNext()) {
            UUID uuid = iter.next();
            ServerPlayerEntity spy = server.getPlayerManager().getPlayer(uuid);
            if (spy == null || spy == caster) {
                iter.remove();
                continue;
            }
            if (!observeIfEligible(spy, caster, abilityId, now, rangeSq, window)) {
                iter.remove();
            }
        }

        // Add any newly eligible spies that weren't already in the cache.
        for (ServerPlayerEntity spy : server.getPlayerManager().getPlayerList()) {
            if (spy == caster || ACTIVE_SPIES.contains(spy.getUuid())) {
                continue;
            }
            if (observeIfEligible(spy, caster, abilityId, now, rangeSq, window)) {
                ACTIVE_SPIES.add(spy.getUuid());
            }
        }

        // If pruning removed everyone, fall back to a full scan so steals still register immediately.
        if (ACTIVE_SPIES.isEmpty()) {
            for (ServerPlayerEntity spy : server.getPlayerManager().getPlayerList()) {
                if (spy == caster) {
                    continue;
                }
                if (observeIfEligible(spy, caster, abilityId, now, rangeSq, window)) {
                    ACTIVE_SPIES.add(spy.getUuid());
                }
            }
        }
    }

    private static void recordObservation(ServerPlayerEntity spy, ServerPlayerEntity caster, Identifier abilityId, long now, int windowTicks) {
        NbtCompound nbt = persistent(spy);
        nbt.putString(KEY_LAST_SEEN_ABILITY, abilityId.toString());
        nbt.putLong(KEY_LAST_SEEN_AT, now);
        GemsNbt.putUuid(nbt, KEY_LAST_SEEN_CASTER, caster.getUuid());
        nbt.putInt(KEY_LAST_SEEN_CASTER_ENTITY_ID, caster.getId());

        int epoch = deaths(spy);
        NbtCompound observed = nbt.getCompound(KEY_OBSERVED).orElse(new NbtCompound());
        String key = abilityId.toString();
        NbtCompound rec = observed.getCompound(key).orElse(new NbtCompound());

        int recEpoch = rec.getInt("epoch", 0);
        long first = rec.getLong("first", 0L);
        long last = rec.getLong("last", 0L);
        int count = rec.getInt("count", 0);
        if (recEpoch != epoch || first <= 0 || now - first > windowTicks || now - last > windowTicks) {
            count = 0;
            first = now;
        }
        count++;
        rec.putInt("epoch", epoch);
        rec.putLong("first", first);
        rec.putLong("last", now);
        rec.putInt("count", count);
        GemsNbt.putUuid(rec, "caster", caster.getUuid());
        observed.put(key, rec);
        nbt.put(KEY_OBSERVED, observed);
    }

    public static Identifier lastSeenAbility(ServerPlayerEntity player) {
        return Identifier.tryParse(persistent(player).getString(KEY_LAST_SEEN_ABILITY, ""));
    }

    public static long lastSeenAt(ServerPlayerEntity player) {
        return persistent(player).getLong(KEY_LAST_SEEN_AT, 0L);
    }

    public static UUID lastSeenCaster(ServerPlayerEntity player) {
        return GemsNbt.getUuid(persistent(player), KEY_LAST_SEEN_CASTER);
    }

    public static int lastSeenCasterEntityId(ServerPlayerEntity player) {
        return persistent(player).getInt(KEY_LAST_SEEN_CASTER_ENTITY_ID, -1);
    }

    public static int witnessedCount(ServerPlayerEntity player, Identifier abilityId) {
        NbtCompound nbt = persistent(player);
        NbtCompound observed = nbt.getCompound(KEY_OBSERVED).orElse(null);
        if (observed == null) {
            return 0;
        }
        String key = abilityId.toString();
        return observed.getCompound(key).map(rec -> rec.getInt("count", 0)).orElse(0);
    }

    public static boolean canSteal(ServerPlayerEntity player, Identifier abilityId, long now) {
        int required = GemsBalance.v().spyMimic().stealRequiredWitnessCount();
        int window = GemsBalance.v().spyMimic().observeWindowTicks();
        int count = witnessedCount(player, abilityId);
        if (count < required) {
            return false;
        }
        NbtCompound nbt = persistent(player);
        NbtCompound observed = nbt.getCompound(KEY_OBSERVED).orElse(null);
        if (observed == null) {
            return false;
        }
        String key = abilityId.toString();
        NbtCompound rec = observed.getCompound(key).orElse(null);
        if (rec == null) {
            return false;
        }
        if (rec.getInt("epoch", 0) != deaths(player)) {
            return false;
        }
        long last = rec.getLong("last", 0L);
        return last > 0 && now - last <= window;
    }

    public static boolean stealLastSeen(ServerPlayerEntity spy) {
        long now = GemsTime.now(spy);
        Identifier abilityId = lastSeenAbility(spy);
        if (abilityId == null) {
            spy.sendMessage(Text.literal("No observed ability."), true);
            return false;
        }
        if (!canSteal(spy, abilityId, now)) {
            spy.sendMessage(Text.literal("Not enough observation to steal that ability."), true);
            return false;
        }

        NbtCompound nbt = persistent(spy);
        int max = GemsBalance.v().spyMimic().maxStolenAbilities();
        NbtList list = nbt.getList(KEY_STOLEN).orElse(new NbtList());
        if (list.size() >= max) {
            spy.sendMessage(Text.literal("Stolen ability slots are full."), true);
            return false;
        }

        String raw = abilityId.toString();
        for (int i = 0; i < list.size(); i++) {
            if (raw.equals(list.getString(i))) {
                spy.sendMessage(Text.literal("You already stole that ability."), true);
                return false;
            }
        }
        list.add(NbtString.of(raw));
        nbt.put(KEY_STOLEN, list);
        if (nbt.getInt(KEY_STOLEN_SELECTED, -1) < 0) {
            nbt.putInt(KEY_STOLEN_SELECTED, 0);
        }

        ServerPlayerEntity victim = null;
        int casterEntityId = lastSeenCasterEntityId(spy);
        ServerWorld world = spy.getEntityWorld();
        if (casterEntityId >= 0) {
            var entity = world.getEntityById(casterEntityId);
            if (entity instanceof ServerPlayerEntity candidate && candidate != spy) {
                victim = candidate;
            }
        }
        if (victim == null) {
            UUID casterUuid = lastSeenCaster(spy);
            var server = world.getServer();
            if (casterUuid != null && server != null) {
                ServerPlayerEntity candidate = server.getPlayerManager().getPlayer(casterUuid);
                if (candidate != null && candidate != spy) {
                    victim = candidate;
                }
            }
        }
        if (victim != null) {
            AbilityDisables.disable(victim, abilityId);
            recordStolenFrom(victim, spy.getUuid(), abilityId);
            victim.sendMessage(Text.literal("One of your abilities was stolen! Kill the thief or wait for them to switch gems to recover it."), false);
        }

        AbilityFeedback.sound(spy, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, 0.8F, 1.3F);
        spy.sendMessage(Text.literal("Stole ability: " + raw), true);
        return true;
    }

    public static Identifier selectedStolenAbility(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        NbtList list = nbt.getList(KEY_STOLEN).orElse(null);
        if (list == null) {
            return null;
        }
        if (list.isEmpty()) {
            return null;
        }
        int idx = nbt.getInt(KEY_STOLEN_SELECTED, 0);
        idx = Math.floorMod(idx, list.size());
        return Identifier.tryParse(list.getString(idx, ""));
    }

    public static boolean cycleStolen(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        NbtList list = nbt.getList(KEY_STOLEN).orElse(null);
        if (list == null) {
            return false;
        }
        if (list.isEmpty()) {
            return false;
        }
        int next = Math.floorMod(nbt.getInt(KEY_STOLEN_SELECTED, 0) + 1, list.size());
        nbt.putInt(KEY_STOLEN_SELECTED, next);
        Identifier selected = Identifier.tryParse(list.getString(next, ""));
        if (selected != null) {
            player.sendMessage(Text.literal("Selected stolen ability: " + selected), true);
        }
        return true;
    }

    public static void clearOnGemSwitchAway(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        nbt.remove(KEY_STOLEN);
        nbt.remove(KEY_STOLEN_SELECTED);
        clearMimicForm(player);
        clearSkinshift(player);
        ACTIVE_SPIES.remove(player.getUuid());
    }

    public static void restoreStolenFromThief(ServerPlayerEntity thief) {
        var server = thief.getEntityWorld().getServer();
        if (server == null) {
            return;
        }
        UUID thiefId = thief.getUuid();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            restoreStolenBy(player, thiefId);
        }
    }

    public static void restoreStolenOnKill(ServerPlayerEntity killer, ServerPlayerEntity victim) {
        if (killer == null || victim == null) {
            return;
        }
        restoreStolenBy(killer, victim.getUuid());
    }

    public static void startMimicForm(ServerPlayerEntity player, int durationTicks) {
        Identifier lastKilled = lastKilledType(player);
        if (lastKilled == null) {
            return;
        }
        NbtCompound nbt = persistent(player);
        long now = GemsTime.now(player);
        nbt.putLong(KEY_MIMIC_UNTIL, now + durationTicks);

        float bonusHp = GemsBalance.v().spyMimic().mimicFormBonusMaxHealth();
        float speedMult = GemsBalance.v().spyMimic().mimicFormSpeedMultiplier();

        EntityAttributeInstance maxHp = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (maxHp != null) {
            maxHp.removeModifier(MOD_MIMIC_MAX_HEALTH);
            if (bonusHp > 0) {
                maxHp.addPersistentModifier(new EntityAttributeModifier(MOD_MIMIC_MAX_HEALTH, bonusHp, EntityAttributeModifier.Operation.ADD_VALUE));
            }
        }
        EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(MOD_MIMIC_SPEED);
            if (speedMult != 1.0F) {
                speed.addPersistentModifier(new EntityAttributeModifier(MOD_MIMIC_SPEED, speedMult - 1.0F, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
        }

        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, durationTicks, 0, true, false, false));
        AbilityFeedback.sound(player, SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR, 0.8F, 1.1F);
        player.sendMessage(Text.literal("Mimic Form: " + lastKilled), true);
    }

    public static boolean startSkinshift(ServerPlayerEntity player, ServerPlayerEntity target, int durationTicks) {
        if (target == null || target == player || durationTicks <= 0) {
            return false;
        }
        NbtCompound nbt = persistent(player);
        long now = GemsTime.now(player);
        nbt.putLong(KEY_SKINSHIFT_UNTIL, now + durationTicks);
        GemsNbt.putUuid(nbt, KEY_SKINSHIFT_TARGET, target.getUuid());
        nbt.putString(KEY_SKINSHIFT_NAME, target.getGameProfile().name());
        syncSkinshift(player, target.getUuid());
        AbilityFeedback.sound(player, SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR, 0.7F, 0.9F);
        player.sendMessage(Text.literal("Skinshift: " + target.getName().getString()), true);
        return true;
    }

    public static void tickEverySecond(ServerPlayerEntity player) {
        trackActiveSpy(player);
        long now = GemsTime.now(player);
        tickStillnessCloak(player, now);
        tickMimicFormCleanup(player, now);
        tickSkinshift(player, now);
    }

    private static void trackActiveSpy(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);
        boolean active = GemPlayerState.getEnergy(player) > 0 && GemPlayerState.getActiveGem(player) == GemId.SPY_MIMIC;
        UUID id = player.getUuid();
        if (active) {
            ACTIVE_SPIES.add(id);
        } else {
            ACTIVE_SPIES.remove(id);
        }
    }

    // Rebuild the active spy cache when it is empty so first-time observations still work.
    private static void seedActiveSpies(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            trackActiveSpy(player);
        }
    }

    private static boolean observeIfEligible(ServerPlayerEntity spy, ServerPlayerEntity caster, Identifier abilityId, long now, double rangeSq, int windowTicks) {
        GemPlayerState.initIfNeeded(spy);
        if (GemPlayerState.getEnergy(spy) <= 0 || GemPlayerState.getActiveGem(spy) != GemId.SPY_MIMIC) {
            return false;
        }
        if (spy.getEntityWorld() != caster.getEntityWorld()) {
            return false;
        }
        if (spy.squaredDistanceTo(caster) > rangeSq) {
            return false;
        }

        // "In front of you": require the caster to be in the forward half-space.
        Vec3d toCaster = caster.getEntityPos().subtract(spy.getEntityPos());
        Vec3d look = spy.getRotationVec(1.0F);
        if (toCaster.dotProduct(look) <= 0.0D) {
            return false;
        }

        recordObservation(spy, caster, abilityId, now, windowTicks);
        return true;
    }

    private static void tickStillnessCloak(ServerPlayerEntity player, long now) {
        if (!GemPowers.isPassiveActive(player, PowerIds.SPY_STILLNESS_CLOAK)) {
            return;
        }

        NbtCompound nbt = persistent(player);
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        double lx = nbt.getDouble(KEY_STILL_LAST_X, x);
        double ly = nbt.getDouble(KEY_STILL_LAST_Y, y);
        double lz = nbt.getDouble(KEY_STILL_LAST_Z, z);

        float eps = GemsBalance.v().spyMimic().stillnessMoveEpsilonBlocks();
        double epsSq = eps * (double) eps;
        double dx = x - lx;
        double dy = y - ly;
        double dz = z - lz;
        boolean moved = (dx * dx + dy * dy + dz * dz) > epsSq;

        if (!nbt.contains(KEY_STILL_LAST_MOVED)) {
            nbt.putLong(KEY_STILL_LAST_MOVED, now);
        }
        if (moved) {
            nbt.putLong(KEY_STILL_LAST_MOVED, now);
        }

        nbt.putDouble(KEY_STILL_LAST_X, x);
        nbt.putDouble(KEY_STILL_LAST_Y, y);
        nbt.putDouble(KEY_STILL_LAST_Z, z);

        long lastMoved = nbt.getLong(KEY_STILL_LAST_MOVED, now);
        int stillness = GemsBalance.v().spyMimic().stillnessTicks();
        if (stillness <= 0) {
            return;
        }
        if (now - lastMoved < stillness) {
            return;
        }

        int refresh = GemsBalance.v().spyMimic().stillnessInvisRefreshTicks();
        if (refresh <= 0) {
            return;
        }
        // No particles, no icon.
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, refresh, 0, true, false, false));
    }

    private static void tickMimicFormCleanup(ServerPlayerEntity player, long now) {
        NbtCompound nbt = persistent(player);
        long until = nbt.getLong(KEY_MIMIC_UNTIL, 0L);
        if (until <= 0) {
            return;
        }
        if (now < until) {
            return;
        }
        clearMimicForm(player);
        player.removeStatusEffect(StatusEffects.INVISIBILITY);
        player.setInvisible(false);
        persistent(player).putLong(KEY_STILL_LAST_MOVED, now);
    }

    private static void tickSkinshift(ServerPlayerEntity player, long now) {
        NbtCompound nbt = persistent(player);
        long until = nbt.getLong(KEY_SKINSHIFT_UNTIL, 0L);
        if (until <= 0) {
            return;
        }
        if (now >= until || GemPlayerState.getEnergy(player) <= 0 || GemPlayerState.getActiveGem(player) != GemId.SPY_MIMIC) {
            clearSkinshift(player);
        }
    }

    private static void clearSkinshift(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        if (GemsNbt.getUuid(nbt, KEY_SKINSHIFT_TARGET) == null) {
            nbt.remove(KEY_SKINSHIFT_UNTIL);
            return;
        }
        nbt.remove(KEY_SKINSHIFT_UNTIL);
        nbt.remove(KEY_SKINSHIFT_TARGET);
        nbt.remove(KEY_SKINSHIFT_NAME);
        syncSkinshift(player, null);
    }

    private static void syncSkinshift(ServerPlayerEntity player, UUID target) {
        if (player.getEntityWorld().getServer() == null) {
            return;
        }
        SpySkinshiftPayload payload = new SpySkinshiftPayload(player.getUuid(), Optional.ofNullable(target));
        for (ServerPlayerEntity viewer : player.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(viewer, payload);
        }
    }

    private static void clearMimicForm(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        nbt.remove(KEY_MIMIC_UNTIL);
        EntityAttributeInstance maxHp = player.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (maxHp != null) {
            maxHp.removeModifier(MOD_MIMIC_MAX_HEALTH);
        }
        EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(MOD_MIMIC_SPEED);
        }
        if (player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
            player.removeStatusEffect(StatusEffects.INVISIBILITY);
        }
    }

    public static boolean canAffect(ServerPlayerEntity caster, ServerPlayerEntity target) {
        return !GemTrust.isTrusted(caster, target);
    }

    public static void syncSkinshifts(ServerPlayerEntity viewer) {
        if (viewer.getEntityWorld().getServer() == null) {
            return;
        }
        long now = GemsTime.now(viewer);
        for (ServerPlayerEntity player : viewer.getEntityWorld().getServer().getPlayerManager().getPlayerList()) {
            NbtCompound nbt = persistent(player);
            UUID target = GemsNbt.getUuid(nbt, KEY_SKINSHIFT_TARGET);
            if (target == null) {
                continue;
            }
            long until = nbt.getLong(KEY_SKINSHIFT_UNTIL, 0L);
            if (until <= now) {
                continue;
            }
            SpySkinshiftPayload payload = new SpySkinshiftPayload(player.getUuid(), Optional.of(target));
            ServerPlayNetworking.send(viewer, payload);
        }
    }

    public static void syncSkinshiftSelf(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        UUID target = GemsNbt.getUuid(nbt, KEY_SKINSHIFT_TARGET);
        if (target == null) {
            return;
        }
        long until = nbt.getLong(KEY_SKINSHIFT_UNTIL, 0L);
        if (until <= GemsTime.now(player)) {
            clearSkinshift(player);
            return;
        }
        syncSkinshift(player, target);
    }

    public static Text chatDisguiseName(ServerPlayerEntity player) {
        NbtCompound nbt = persistent(player);
        long until = nbt.getLong(KEY_SKINSHIFT_UNTIL, 0L);
        if (until <= GemsTime.now(player)) {
            return null;
        }
        String name = nbt.getString(KEY_SKINSHIFT_NAME, "");
        if (!name.isBlank()) {
            return Text.literal(name);
        }
        UUID target = GemsNbt.getUuid(nbt, KEY_SKINSHIFT_TARGET);
        if (target == null) {
            return null;
        }
        var server = player.getEntityWorld().getServer();
        if (server == null) {
            return null;
        }
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(target);
        if (online != null) {
            return Text.literal(online.getGameProfile().name());
        }
        return null;
    }

    public static boolean isSkinshiftTarget(ServerPlayerEntity target) {
        if (target == null) {
            return false;
        }
        var server = target.getEntityWorld().getServer();
        if (server == null) {
            return false;
        }
        UUID targetId = target.getUuid();
        long now = GemsTime.now(target);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            NbtCompound nbt = persistent(player);
            UUID candidate = GemsNbt.getUuid(nbt, KEY_SKINSHIFT_TARGET);
            if (candidate == null) {
                continue;
            }
            long until = nbt.getLong(KEY_SKINSHIFT_UNTIL, 0L);
            if (until <= now) {
                continue;
            }
            if (targetId.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static void recordStolenFrom(ServerPlayerEntity victim, UUID thief, Identifier abilityId) {
        if (thief == null || abilityId == null) {
            return;
        }
        NbtCompound root = persistent(victim);
        NbtCompound stolenBy = root.getCompound(KEY_STOLEN_BY).orElse(new NbtCompound());
        stolenBy.put(abilityId.toString(), GemsNbt.fromUuid(thief));
        root.put(KEY_STOLEN_BY, stolenBy);
    }

    private static void restoreStolenBy(ServerPlayerEntity victim, UUID thief) {
        if (thief == null) {
            return;
        }
        NbtCompound root = persistent(victim);
        NbtCompound stolenBy = root.getCompound(KEY_STOLEN_BY).orElse(null);
        if (stolenBy == null) {
            return;
        }
        boolean changed = false;
        for (String key : java.util.List.copyOf(stolenBy.getKeys())) {
            UUID recorded = GemsNbt.toUuid(stolenBy.get(key));
            if (recorded == null) {
                continue;
            }
            if (!thief.equals(recorded)) {
                continue;
            }
            Identifier abilityId = Identifier.tryParse(key);
            if (abilityId != null) {
                AbilityDisables.enable(victim, abilityId);
            }
            stolenBy.remove(key);
            changed = true;
        }
        if (stolenBy.isEmpty()) {
            root.remove(KEY_STOLEN_BY);
        } else {
            root.put(KEY_STOLEN_BY, stolenBy);
        }
        if (changed) {
            victim.sendMessage(Text.literal("Recovered stolen abilities."), true);
        }
    }

    private static NbtCompound persistent(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }
}
