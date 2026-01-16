package com.feel.gems.mastery;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.gem.spy.SpySystem;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.state.GemsPersistentDataHolder;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Tracks server-wide leaderboard stats for general titles.
 * General titles are awarded to the current leader in each category:
 * - Least Deaths (Immortal)
 * - Most Kills (King Slayer)
 * - Most Hearts (Titan)
 * - Max Energy (Whale)
 */
public final class LeaderboardTracker {
    private static final String KEY_KILLS = "gemsKillCount";

    /**
     * Categories for general titles based on leaderboard stats.
     */
    public enum LeaderboardCategory {
        LEAST_DEATHS("gems.title.general.immortal", "Immortal"),
        MOST_KILLS("gems.title.general.king_slayer", "King Slayer"),
        MOST_HEARTS("gems.title.general.titan", "Titan"),
        MAX_ENERGY("gems.title.general.whale", "Whale");

        private final String translationKey;
        private final String fallbackName;

        LeaderboardCategory(String translationKey, String fallbackName) {
            this.translationKey = translationKey;
            this.fallbackName = fallbackName;
        }

        public String translationKey() {
            return translationKey;
        }

        public String fallbackName() {
            return fallbackName;
        }
    }

    /**
     * Record of a player's leaderboard entry.
     */
    public record LeaderboardEntry(UUID playerId, String playerName, int value) {}

    /**
     * Current leaderboard leaders by category.
     */
    private static final Map<LeaderboardCategory, LeaderboardEntry> LEADERS = new EnumMap<>(LeaderboardCategory.class);

    private LeaderboardTracker() {
    }

    // ========== Kill Tracking ==========

    /**
     * Increment the player's kill count.
     * Called from GemsPlayerDeath when a player kills another player.
     */
    public static void incrementKills(ServerPlayerEntity killer) {
        NbtCompound nbt = ((GemsPersistentDataHolder) killer).gems$getPersistentData();
        int kills = nbt.getInt(KEY_KILLS, 0);
        nbt.putInt(KEY_KILLS, kills + 1);
    }

    /**
     * Get the player's kill count.
     */
    public static int getKills(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData().getInt(KEY_KILLS, 0);
    }

    // ========== Leaderboard Updates ==========

    /**
     * Update the leaderboard for all categories.
     * Should be called periodically (e.g., every minute) or on significant events.
     */
    public static void updateLeaderboards(MinecraftServer server) {
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        if (players.isEmpty()) {
            return;
        }

        // Least Deaths - player with fewest deaths (requires at least 1 kill to qualify)
        updateLeastDeaths(players);

        // Most Kills
        updateMostKills(players);

        // Most Hearts
        updateMostHearts(players);

        // Max Energy
        updateMaxEnergy(players);
    }

    private static void updateLeastDeaths(List<ServerPlayerEntity> players) {
        ServerPlayerEntity leader = players.stream()
                .filter(p -> getKills(p) >= 1) // Must have at least 1 kill to qualify
                .min(Comparator.comparingInt(p -> SpySystem.deaths(p)))
                .orElse(null);

        if (leader != null) {
            int deaths = SpySystem.deaths(leader);
            LEADERS.put(LeaderboardCategory.LEAST_DEATHS, 
                    new LeaderboardEntry(leader.getUuid(), leader.getName().getString(), deaths));
        }
    }

    private static void updateMostKills(List<ServerPlayerEntity> players) {
        ServerPlayerEntity leader = players.stream()
                .max(Comparator.comparingInt(LeaderboardTracker::getKills))
                .filter(p -> getKills(p) >= 1) // Must have at least 1 kill
                .orElse(null);

        if (leader != null) {
            int kills = getKills(leader);
            LEADERS.put(LeaderboardCategory.MOST_KILLS,
                    new LeaderboardEntry(leader.getUuid(), leader.getName().getString(), kills));
        }
    }

    private static void updateMostHearts(List<ServerPlayerEntity> players) {
        ServerPlayerEntity leader = players.stream()
                .max(Comparator.comparingInt(GemPlayerState::getMaxHearts))
                .orElse(null);

        if (leader != null) {
            int hearts = GemPlayerState.getMaxHearts(leader);
            LEADERS.put(LeaderboardCategory.MOST_HEARTS,
                    new LeaderboardEntry(leader.getUuid(), leader.getName().getString(), hearts));
        }
    }

    private static void updateMaxEnergy(List<ServerPlayerEntity> players) {
        ServerPlayerEntity leader = players.stream()
                .max(Comparator.comparingInt(GemPlayerState::getEnergy))
                .filter(p -> GemPlayerState.getEnergy(p) >= GemPlayerState.MAX_ENERGY) // Must be at max energy
                .orElse(null);

        if (leader != null) {
            int energy = GemPlayerState.getEnergy(leader);
            LEADERS.put(LeaderboardCategory.MAX_ENERGY,
                    new LeaderboardEntry(leader.getUuid(), leader.getName().getString(), energy));
        }
    }

    // ========== Title Queries ==========

    /**
     * Check if a player holds a general title.
     */
    public static boolean holdsTitle(ServerPlayerEntity player, LeaderboardCategory category) {
        LeaderboardEntry leader = LEADERS.get(category);
        return leader != null && leader.playerId().equals(player.getUuid());
    }

    /**
     * Get all general titles held by a player.
     */
    public static List<LeaderboardCategory> getTitles(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();
        return LEADERS.entrySet().stream()
                .filter(e -> e.getValue().playerId().equals(playerId))
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Get the current leader for a category.
     */
    public static LeaderboardEntry getLeader(LeaderboardCategory category) {
        return LEADERS.get(category);
    }

    /**
     * Get all current leaders.
     */
    public static Map<LeaderboardCategory, LeaderboardEntry> getAllLeaders() {
        return Map.copyOf(LEADERS);
    }

    /**
     * Get the translatable title text for a category.
     */
    public static Text getTitleText(LeaderboardCategory category) {
        return Text.translatable(category.translationKey());
    }
}
