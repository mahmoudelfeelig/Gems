package com.feel.gems.power;

import net.minecraft.entity.Entity;

import java.util.Set;
import java.util.UUID;

public final class SoulSummons {
    public static final String TAG_SOUL = "gems_soul";
    private static final String TAG_OWNER_PREFIX = "gems_soul_owner:";

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
}

