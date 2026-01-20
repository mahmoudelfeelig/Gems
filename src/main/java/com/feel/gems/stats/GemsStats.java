package com.feel.gems.stats;

import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.state.GemPlayerState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class GemsStats {
    private static final String KEY_STATS = "gemsStats";
    private static final String KEY_LAST_ABILITY = "lastAbilityId";

    private static final String KEY_KILLS = "kills";
    private static final String KEY_DEATHS = "deaths";
    private static final String KEY_PLAYER_KILLS = "playerKills";
    private static final String KEY_PLAYER_DEATHS = "playerDeaths";
    private static final String KEY_MOB_KILLS = "mobKills";
    private static final String KEY_ASSASSIN_KILLS = "assassinKills";
    private static final String KEY_ASSASSIN_DEATHS = "assassinDeaths";
    private static final String KEY_FINAL_KILLS = "finalKills";
    private static final String KEY_FINAL_DEATHS = "finalDeaths";

    private static final String KEY_KILLS_BY_GEM = "killsByGem";
    private static final String KEY_DEATHS_BY_GEM = "deathsByGem";
    private static final String KEY_KILLS_BY_ABILITY = "killsByAbility";
    private static final String KEY_DEATHS_BY_ABILITY = "deathsByAbility";
    private static final String KEY_KILLS_BY_VICTIM_GEM = "killsByVictimGem";
    private static final String KEY_DEATHS_BY_KILLER_GEM = "deathsByKillerGem";
    private static final String KEY_ABILITY_USES = "abilityUses";

    private GemsStats() {
    }

    public static void recordAbilityUse(ServerPlayerEntity player, Identifier abilityId) {
        NbtCompound stats = statsRoot(player);
        if (abilityId != null) {
            stats.putString(KEY_LAST_ABILITY, abilityId.toString());
            incrementMap(stats, KEY_ABILITY_USES, abilityId.toString());
        }
    }

    public static Identifier lastAbility(ServerPlayerEntity player) {
        NbtCompound stats = statsRoot(player);
        String raw = stats.getString(KEY_LAST_ABILITY, "");
        if (raw.isEmpty()) {
            return null;
        }
        return Identifier.tryParse(raw);
    }

    public static void recordMobKill(ServerPlayerEntity killer) {
        NbtCompound stats = statsRoot(killer);
        increment(stats, KEY_KILLS);
        increment(stats, KEY_MOB_KILLS);
        incrementMap(stats, KEY_KILLS_BY_GEM, GemPlayerState.getActiveGem(killer).name());
        Identifier last = lastAbility(killer);
        if (last != null) {
            incrementMap(stats, KEY_KILLS_BY_ABILITY, last.toString());
        }
    }

    public static void recordPlayerKill(ServerPlayerEntity killer, ServerPlayerEntity victim, boolean killerAssassin, boolean victimAssassin, boolean finalKill) {
        NbtCompound stats = statsRoot(killer);
        increment(stats, KEY_KILLS);
        increment(stats, KEY_PLAYER_KILLS);
        if (killerAssassin) {
            increment(stats, KEY_ASSASSIN_KILLS);
        }
        if (finalKill) {
            increment(stats, KEY_FINAL_KILLS);
        }

        incrementMap(stats, KEY_KILLS_BY_GEM, GemPlayerState.getActiveGem(killer).name());
        incrementMap(stats, KEY_KILLS_BY_VICTIM_GEM, GemPlayerState.getActiveGem(victim).name());

        Identifier last = lastAbility(killer);
        if (last != null) {
            incrementMap(stats, KEY_KILLS_BY_ABILITY, last.toString());
        }
    }

    public static void recordPlayerDeath(ServerPlayerEntity victim, ServerPlayerEntity killer, boolean victimAssassin, boolean finalDeath) {
        NbtCompound stats = statsRoot(victim);
        increment(stats, KEY_DEATHS);
        increment(stats, KEY_PLAYER_DEATHS);
        if (victimAssassin) {
            increment(stats, KEY_ASSASSIN_DEATHS);
        }
        if (finalDeath) {
            increment(stats, KEY_FINAL_DEATHS);
        }

        incrementMap(stats, KEY_DEATHS_BY_GEM, GemPlayerState.getActiveGem(victim).name());
        if (killer != null) {
            incrementMap(stats, KEY_DEATHS_BY_KILLER_GEM, GemPlayerState.getActiveGem(killer).name());
            Identifier last = lastAbility(killer);
            if (last != null) {
                incrementMap(stats, KEY_DEATHS_BY_ABILITY, last.toString());
            }
        }
    }

    public static int deaths(ServerPlayerEntity player) {
        NbtCompound stats = statsRoot(player);
        return stats.getInt(KEY_DEATHS, 0);
    }

    public static void reset(ServerPlayerEntity player) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        root.remove(KEY_STATS);
    }

    public static boolean setStat(ServerPlayerEntity player, String statId, int value) {
        if (player == null || statId == null) {
            return false;
        }
        String key = statId.trim().toLowerCase(java.util.Locale.ROOT);
        String target = switch (key) {
            case "kills" -> KEY_KILLS;
            case "deaths" -> KEY_DEATHS;
            case "playerkills", "player_kills", "player-kills" -> KEY_PLAYER_KILLS;
            case "playerdeaths", "player_deaths", "player-deaths" -> KEY_PLAYER_DEATHS;
            case "mobkills", "mob_kills", "mob-kills" -> KEY_MOB_KILLS;
            case "assassinkills", "assassin_kills", "assassin-kills" -> KEY_ASSASSIN_KILLS;
            case "assassindeaths", "assassin_deaths", "assassin-deaths" -> KEY_ASSASSIN_DEATHS;
            case "finalkills", "final_kills", "final-kills" -> KEY_FINAL_KILLS;
            case "finaldeaths", "final_deaths", "final-deaths" -> KEY_FINAL_DEATHS;
            default -> null;
        };
        if (target == null) {
            return false;
        }
        NbtCompound stats = statsRoot(player);
        stats.putInt(target, Math.max(0, value));
        return true;
    }

    public static List<Text> buildSummary(ServerPlayerEntity player) {
        NbtCompound stats = statsRoot(player);
        int kills = stats.getInt(KEY_KILLS, 0);
        int deaths = stats.getInt(KEY_DEATHS, 0);
        int playerKills = stats.getInt(KEY_PLAYER_KILLS, 0);
        int playerDeaths = stats.getInt(KEY_PLAYER_DEATHS, 0);
        int mobKills = stats.getInt(KEY_MOB_KILLS, 0);
        int assassinKills = stats.getInt(KEY_ASSASSIN_KILLS, 0);
        int assassinDeaths = stats.getInt(KEY_ASSASSIN_DEATHS, 0);
        int finalKills = stats.getInt(KEY_FINAL_KILLS, 0);
        int finalDeaths = stats.getInt(KEY_FINAL_DEATHS, 0);
        int totalAbilityUses = sumMap(stats, KEY_ABILITY_USES);

        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal("Stats for " + player.getName().getString() + ":"));
        lines.add(Text.literal("Kills: " + kills + " (Players: " + playerKills + ", Mobs: " + mobKills + ")"));
        lines.add(Text.literal("Deaths: " + deaths + " (Player deaths: " + playerDeaths + ")"));
        lines.add(Text.literal("Assassin K/D: " + assassinKills + "/" + assassinDeaths + " | Final K/D: " + finalKills + "/" + finalDeaths));
        lines.add(Text.literal("Ability uses: " + totalAbilityUses));

        String topAbility = topKey(stats, KEY_ABILITY_USES);
        if (topAbility != null) {
            lines.add(Text.literal("Top ability: " + topAbility));
        }
        String topGemKills = topKey(stats, KEY_KILLS_BY_GEM);
        if (topGemKills != null) {
            lines.add(Text.literal("Top kill gem: " + topGemKills));
        }
        String topVictimGem = topKey(stats, KEY_KILLS_BY_VICTIM_GEM);
        if (topVictimGem != null) {
            lines.add(Text.literal("Top victim gem: " + topVictimGem));
        }
        String topDeathAbility = topKey(stats, KEY_DEATHS_BY_ABILITY);
        if (topDeathAbility != null) {
            lines.add(Text.literal("Top death ability: " + topDeathAbility));
        }
        String topDeathGem = topKey(stats, KEY_DEATHS_BY_GEM);
        if (topDeathGem != null) {
            lines.add(Text.literal("Top death gem: " + topDeathGem));
        }
        String topKillerGem = topKey(stats, KEY_DEATHS_BY_KILLER_GEM);
        if (topKillerGem != null) {
            lines.add(Text.literal("Top killer gem: " + topKillerGem));
        }
        return lines;
    }

    private static NbtCompound statsRoot(ServerPlayerEntity player) {
        NbtCompound root = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        NbtCompound stats = root.getCompound(KEY_STATS).orElse(null);
        if (stats == null) {
            stats = new NbtCompound();
            root.put(KEY_STATS, stats);
        }
        return stats;
    }

    private static void increment(NbtCompound stats, String key) {
        int current = stats.getInt(key, 0);
        stats.putInt(key, current + 1);
    }

    private static void incrementMap(NbtCompound stats, String mapKey, String key) {
        NbtCompound map = stats.getCompound(mapKey).orElse(null);
        if (map == null) {
            map = new NbtCompound();
            stats.put(mapKey, map);
        }
        int current = map.getInt(key, 0);
        map.putInt(key, current + 1);
    }

    private static String topKey(NbtCompound stats, String mapKey) {
        NbtCompound map = stats.getCompound(mapKey).orElse(null);
        if (map == null) {
            return null;
        }
        Map<String, Integer> values = new TreeMap<>();
        for (String key : map.getKeys()) {
            values.put(key, map.getInt(key, 0));
        }
        return values.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private static int sumMap(NbtCompound stats, String mapKey) {
        NbtCompound map = stats.getCompound(mapKey).orElse(null);
        if (map == null) {
            return 0;
        }
        int total = 0;
        for (String key : map.getKeys()) {
            total += map.getInt(key, 0);
        }
        return total;
    }
}
