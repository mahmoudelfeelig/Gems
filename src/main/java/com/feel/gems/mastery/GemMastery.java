package com.feel.gems.mastery;

import com.feel.gems.core.GemId;
import com.feel.gems.state.GemsPersistentDataHolder;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Manages gem mastery data for players.
 * Tracks ability usage per gem and manages cosmetic selections.
 */
public final class GemMastery {
    private static final String KEY_MASTERY = "gemMastery";
    private static final String KEY_USAGE = "usage";
    private static final String KEY_SELECTED_TITLE = "selectedTitle";
    private static final String KEY_SELECTED_AURA = "selectedAura";
    private static final String KEY_SELECTED_TITLE_OVERRIDE = "selectedTitleOverride";

    private GemMastery() {
    }

    // ========== Ability Usage Tracking ==========

    /**
     * Increment ability usage count for a gem.
     * Called whenever a player successfully uses a gem ability.
     */
    public static void incrementUsage(ServerPlayerEntity player, GemId gem) {
        NbtCompound gemData = getGemMasteryData(player, gem);
        int current = gemData.getInt(KEY_USAGE).orElse(0);
        gemData.putInt(KEY_USAGE, current + 1);
    }

    /**
     * Get the ability usage count for a gem.
     */
    public static int getUsage(ServerPlayerEntity player, GemId gem) {
        NbtCompound gemData = getGemMasteryData(player, gem);
        return gemData.getInt(KEY_USAGE).orElse(0);
    }

    /**
     * Get all gem usage counts for a player.
     */
    public static Map<GemId, Integer> getAllUsage(ServerPlayerEntity player) {
        Map<GemId, Integer> result = new EnumMap<>(GemId.class);
        for (GemId gem : GemId.values()) {
            result.put(gem, getUsage(player, gem));
        }
        return result;
    }

    // ========== Title Selection ==========

    /**
     * Set the player's selected title (from any gem they've unlocked).
     * Pass null or empty string to clear.
     */
    public static void setSelectedTitle(ServerPlayerEntity player, String titleId) {
        setSelectedTitle(player, titleId, false);
    }

    /**
     * Set the player's selected title, optionally bypassing unlock checks.
     */
    public static void setSelectedTitle(ServerPlayerEntity player, String titleId, boolean force) {
        NbtCompound mastery = getMasteryRoot(player);
        if (titleId == null || titleId.isEmpty()) {
            mastery.remove(KEY_SELECTED_TITLE);
            mastery.remove(KEY_SELECTED_TITLE_OVERRIDE);
        } else {
            mastery.putString(KEY_SELECTED_TITLE, titleId);
            mastery.putBoolean(KEY_SELECTED_TITLE_OVERRIDE, force);
        }
    }

    /**
     * Get the player's currently selected title id.
     */
    public static String getSelectedTitle(ServerPlayerEntity player) {
        NbtCompound mastery = getMasteryRoot(player);
        return mastery.getString(KEY_SELECTED_TITLE).orElse("");
    }

    /**
     * Check if the selected title is forced (admin override).
     */
    public static boolean isSelectedTitleForced(ServerPlayerEntity player) {
        NbtCompound mastery = getMasteryRoot(player);
        return mastery.getBoolean(KEY_SELECTED_TITLE_OVERRIDE, false);
    }

    /**
     * Get the player's selected title reward, or null if none/invalid.
     */
    public static MasteryReward getSelectedTitleReward(ServerPlayerEntity player) {
        String titleId = getSelectedTitle(player);
        if (titleId.isEmpty()) {
            return null;
        }
        MasteryReward reward = MasteryRewards.findById(titleId);
        if (reward == null || reward.type() != MasteryReward.MasteryRewardType.TITLE) {
            return null;
        }
        NbtCompound mastery = getMasteryRoot(player);
        boolean override = mastery.getBoolean(KEY_SELECTED_TITLE_OVERRIDE, false);
        if (override) {
            return reward;
        }
        // Verify it's actually unlocked
        GemId gem = gemFromRewardId(titleId);
        if (gem == null) {
            return null;
        }
        int usage = getUsage(player, gem);
        if (usage < reward.threshold()) {
            return null;
        }
        return reward;
    }

    // ========== Aura Selection ==========

    /**
     * Set the player's selected aura (from any gem they've unlocked).
     * Pass null or empty string to clear.
     */
    public static void setSelectedAura(ServerPlayerEntity player, String auraId) {
        NbtCompound mastery = getMasteryRoot(player);
        if (auraId == null || auraId.isEmpty()) {
            mastery.remove(KEY_SELECTED_AURA);
        } else {
            mastery.putString(KEY_SELECTED_AURA, auraId);
        }
    }

    /**
     * Get the player's currently selected aura id.
     */
    public static String getSelectedAura(ServerPlayerEntity player) {
        NbtCompound mastery = getMasteryRoot(player);
        return mastery.getString(KEY_SELECTED_AURA).orElse("");
    }

    /**
     * Get the player's selected aura reward, or null if none/invalid.
     */
    public static MasteryReward getSelectedAuraReward(ServerPlayerEntity player) {
        String auraId = getSelectedAura(player);
        if (auraId.isEmpty()) {
            return null;
        }
        MasteryReward reward = MasteryRewards.findById(auraId);
        if (reward == null || reward.type() != MasteryReward.MasteryRewardType.AURA) {
            return null;
        }
        // Verify it's actually unlocked
        GemId gem = gemFromRewardId(auraId);
        if (gem == null) {
            return null;
        }
        int usage = getUsage(player, gem);
        if (usage < reward.threshold()) {
            return null;
        }
        return reward;
    }

    // ========== Helpers ==========

    private static NbtCompound getMasteryRoot(ServerPlayerEntity player) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        NbtCompound mastery = root.getCompound(KEY_MASTERY).orElse(null);
        if (mastery == null) {
            mastery = new NbtCompound();
            root.put(KEY_MASTERY, mastery);
        }
        return mastery;
    }

    private static NbtCompound getGemMasteryData(ServerPlayerEntity player, GemId gem) {
        NbtCompound mastery = getMasteryRoot(player);
        String gemKey = gem.name().toLowerCase();
        NbtCompound gemData = mastery.getCompound(gemKey).orElse(null);
        if (gemData == null) {
            gemData = new NbtCompound();
            mastery.put(gemKey, gemData);
        }
        return gemData;
    }

    /**
     * Extract the gem from a reward id (e.g., "astra_master" -> ASTRA).
     */
    public static GemId gemFromRewardId(String rewardId) {
        if (rewardId == null || rewardId.isEmpty()) {
            return null;
        }
        for (GemId gem : GemId.values()) {
            String prefix = gem.name().toLowerCase() + "_";
            if (rewardId.startsWith(prefix)) {
                return gem;
            }
        }
        return null;
    }
}
