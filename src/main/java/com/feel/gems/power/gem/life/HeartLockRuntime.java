package com.feel.gems.power.gem.life;

import com.feel.gems.GemsMod;
import com.feel.gems.util.GemsTime;
import java.util.List;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;




public final class HeartLockRuntime {
    private static final String TAG_HEART_LOCK = "gems_heart_lock";
    private static final String TAG_UNTIL_PREFIX = "gems_heart_lock_until:";
    private static final String TAG_MAX_PREFIX = "gems_heart_lock_max:";
    private static final Identifier MODIFIER_ID = Identifier.of(GemsMod.MOD_ID, "heart_lock");

    private HeartLockRuntime() {
    }

    public static void apply(ServerPlayerEntity caster, MobEntity mob, int durationTicks) {
        long until = durationTicks > 0 ? GemsTime.now(caster) + durationTicks : 0L;
        float lockedMax = Math.max(2.0F, mob.getHealth());
        clearTags(mob);
        mob.addCommandTag(TAG_HEART_LOCK);
        if (until > 0) {
            mob.addCommandTag(TAG_UNTIL_PREFIX + until);
        }
        mob.addCommandTag(TAG_MAX_PREFIX + lockedMax);
        applyModifier(mob, lockedMax);
    }

    public static void tick(MobEntity mob) {
        if (!mob.getCommandTags().contains(TAG_HEART_LOCK)) {
            return;
        }
        if (!(mob.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        long until = readUntil(mob);
        long now = GemsTime.now(world);
        if (until > 0 && now >= until) {
            clear(mob);
            return;
        }
        float lockedMax = readLockedMax(mob);
        if (lockedMax <= 0.0F) {
            lockedMax = Math.max(2.0F, mob.getHealth());
        }
        applyModifier(mob, lockedMax);
    }

    private static void applyModifier(MobEntity mob, float lockedMax) {
        EntityAttributeInstance maxHealth = mob.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        maxHealth.removeModifier(MODIFIER_ID);
        double baseMax = maxHealth.getValue();
        double delta = lockedMax - baseMax;
        if (delta != 0.0D) {
            maxHealth.addTemporaryModifier(new EntityAttributeModifier(MODIFIER_ID, delta, EntityAttributeModifier.Operation.ADD_VALUE));
        }
        if (mob.getHealth() > lockedMax) {
            mob.setHealth(lockedMax);
        }
    }

    private static void clear(MobEntity mob) {
        clearTags(mob);
        EntityAttributeInstance maxHealth = mob.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(MODIFIER_ID);
        }
    }

    private static long readUntil(MobEntity mob) {
        for (String tag : mob.getCommandTags()) {
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

    private static float readLockedMax(MobEntity mob) {
        for (String tag : mob.getCommandTags()) {
            if (!tag.startsWith(TAG_MAX_PREFIX)) {
                continue;
            }
            String raw = tag.substring(TAG_MAX_PREFIX.length());
            try {
                return Float.parseFloat(raw);
            } catch (NumberFormatException ignored) {
                return 0.0F;
            }
        }
        return 0.0F;
    }

    private static void clearTags(MobEntity mob) {
        List<String> tags = List.copyOf(mob.getCommandTags());
        for (String tag : tags) {
            if (tag.equals(TAG_HEART_LOCK) || tag.startsWith(TAG_UNTIL_PREFIX) || tag.startsWith(TAG_MAX_PREFIX)) {
                mob.removeCommandTag(tag);
            }
        }
    }
}
