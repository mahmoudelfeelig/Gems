package com.feel.gems.assassin;

import com.feel.gems.state.GemsPersistentDataHolder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Persistent per-player state for the "Assassin endgame" loop.
 *
 * <p>Assassins keep gems/energy as normal; this only governs max-hearts and scoring.</p>
 */
public final class AssassinState {
    private static final String KEY_IS_ASSASSIN = "assassinIsAssassin";
    private static final String KEY_ELIMINATED = "assassinEliminated";
    private static final String KEY_ASSASSIN_HEARTS = "assassinHearts";

    private static final String KEY_TOTAL_NORMAL_KILLS = "assassinTotalNormalKills";
    private static final String KEY_TOTAL_FINAL_KILLS = "assassinTotalFinalKills";

    private static final String KEY_A_NORMAL_KILLS = "assassinNormalKills";
    private static final String KEY_A_FINAL_KILLS = "assassinFinalKills";

    private static final String KEY_A_NORMAL_KILLS_VS_NON = "assassinNormalKillsVsNon";
    private static final String KEY_A_FINAL_KILLS_VS_NON = "assassinFinalKillsVsNon";

    public static final int ASSASSIN_MAX_HEARTS = 10;

    private AssassinState() {
    }

    public static void initIfNeeded(ServerPlayerEntity player) {
        NbtCompound nbt = root(player);
        if (!nbt.contains(KEY_IS_ASSASSIN, NbtElement.BYTE_TYPE)) {
            nbt.putBoolean(KEY_IS_ASSASSIN, false);
        }
        if (!nbt.contains(KEY_ELIMINATED, NbtElement.BYTE_TYPE)) {
            nbt.putBoolean(KEY_ELIMINATED, false);
        }
        if (!nbt.contains(KEY_ASSASSIN_HEARTS, NbtElement.INT_TYPE)) {
            nbt.putInt(KEY_ASSASSIN_HEARTS, ASSASSIN_MAX_HEARTS);
        }
        if (!nbt.contains(KEY_TOTAL_NORMAL_KILLS, NbtElement.INT_TYPE)) {
            nbt.putInt(KEY_TOTAL_NORMAL_KILLS, 0);
        }
        if (!nbt.contains(KEY_TOTAL_FINAL_KILLS, NbtElement.INT_TYPE)) {
            nbt.putInt(KEY_TOTAL_FINAL_KILLS, 0);
        }
        if (!nbt.contains(KEY_A_NORMAL_KILLS, NbtElement.INT_TYPE)) {
            nbt.putInt(KEY_A_NORMAL_KILLS, 0);
        }
        if (!nbt.contains(KEY_A_FINAL_KILLS, NbtElement.INT_TYPE)) {
            nbt.putInt(KEY_A_FINAL_KILLS, 0);
        }
        if (!nbt.contains(KEY_A_NORMAL_KILLS_VS_NON, NbtElement.INT_TYPE)) {
            nbt.putInt(KEY_A_NORMAL_KILLS_VS_NON, 0);
        }
        if (!nbt.contains(KEY_A_FINAL_KILLS_VS_NON, NbtElement.INT_TYPE)) {
            nbt.putInt(KEY_A_FINAL_KILLS_VS_NON, 0);
        }
    }

    public static boolean isAssassin(ServerPlayerEntity player) {
        return root(player).getBoolean(KEY_IS_ASSASSIN);
    }

    public static boolean isEliminated(ServerPlayerEntity player) {
        return root(player).getBoolean(KEY_ELIMINATED);
    }

    public static int getAssassinHearts(ServerPlayerEntity player) {
        if (!isAssassin(player)) {
            return ASSASSIN_MAX_HEARTS;
        }
        int raw = root(player).getInt(KEY_ASSASSIN_HEARTS);
        return clamp(raw, 0, ASSASSIN_MAX_HEARTS);
    }

    public static int getAssassinHeartsForAttribute(ServerPlayerEntity player) {
        // Avoid 0 max-health edge-cases; eliminated players are handled by game mode, but we keep attribute valid.
        return Math.max(1, getAssassinHearts(player));
    }

    public static void becomeAssassin(ServerPlayerEntity player) {
        initIfNeeded(player);
        NbtCompound nbt = root(player);
        if (nbt.getBoolean(KEY_IS_ASSASSIN)) {
            return;
        }
        nbt.putBoolean(KEY_IS_ASSASSIN, true);
        nbt.putBoolean(KEY_ELIMINATED, false);
        nbt.putInt(KEY_ASSASSIN_HEARTS, ASSASSIN_MAX_HEARTS);

        // Reset the "after turning" counters.
        nbt.putInt(KEY_A_NORMAL_KILLS, 0);
        nbt.putInt(KEY_A_FINAL_KILLS, 0);
        nbt.putInt(KEY_A_NORMAL_KILLS_VS_NON, 0);
        nbt.putInt(KEY_A_FINAL_KILLS_VS_NON, 0);
    }

    public static void setEliminated(ServerPlayerEntity player, boolean eliminated) {
        initIfNeeded(player);
        root(player).putBoolean(KEY_ELIMINATED, eliminated);
    }

    public static int addAssassinHearts(ServerPlayerEntity player, int delta) {
        initIfNeeded(player);
        int next = clamp(getAssassinHearts(player) + delta, 0, ASSASSIN_MAX_HEARTS);
        root(player).putInt(KEY_ASSASSIN_HEARTS, next);
        return next;
    }

    public static void recordKill(ServerPlayerEntity killer, boolean finalKill, boolean victimWasAssassin) {
        initIfNeeded(killer);
        NbtCompound nbt = root(killer);

        if (finalKill) {
            nbt.putInt(KEY_TOTAL_FINAL_KILLS, nbt.getInt(KEY_TOTAL_FINAL_KILLS) + 1);
        } else {
            nbt.putInt(KEY_TOTAL_NORMAL_KILLS, nbt.getInt(KEY_TOTAL_NORMAL_KILLS) + 1);
        }

        if (isAssassin(killer)) {
            if (finalKill) {
                nbt.putInt(KEY_A_FINAL_KILLS, nbt.getInt(KEY_A_FINAL_KILLS) + 1);
                if (!victimWasAssassin) {
                    nbt.putInt(KEY_A_FINAL_KILLS_VS_NON, nbt.getInt(KEY_A_FINAL_KILLS_VS_NON) + 1);
                }
            } else {
                nbt.putInt(KEY_A_NORMAL_KILLS, nbt.getInt(KEY_A_NORMAL_KILLS) + 1);
                if (!victimWasAssassin) {
                    nbt.putInt(KEY_A_NORMAL_KILLS_VS_NON, nbt.getInt(KEY_A_NORMAL_KILLS_VS_NON) + 1);
                }
            }
        }
    }

    public static int totalNormalKills(ServerPlayerEntity player) {
        return root(player).getInt(KEY_TOTAL_NORMAL_KILLS);
    }

    public static int totalFinalKills(ServerPlayerEntity player) {
        return root(player).getInt(KEY_TOTAL_FINAL_KILLS);
    }

    public static int totalPoints(ServerPlayerEntity player) {
        return points(totalNormalKills(player), totalFinalKills(player));
    }

    public static int assassinNormalKills(ServerPlayerEntity player) {
        return root(player).getInt(KEY_A_NORMAL_KILLS);
    }

    public static int assassinFinalKills(ServerPlayerEntity player) {
        return root(player).getInt(KEY_A_FINAL_KILLS);
    }

    public static int assassinPoints(ServerPlayerEntity player) {
        return points(assassinNormalKills(player), assassinFinalKills(player));
    }

    public static int assassinNormalKillsVsNonAssassins(ServerPlayerEntity player) {
        return root(player).getInt(KEY_A_NORMAL_KILLS_VS_NON);
    }

    public static int assassinFinalKillsVsNonAssassins(ServerPlayerEntity player) {
        return root(player).getInt(KEY_A_FINAL_KILLS_VS_NON);
    }

    public static int assassinPointsVsNonAssassins(ServerPlayerEntity player) {
        return points(assassinNormalKillsVsNonAssassins(player), assassinFinalKillsVsNonAssassins(player));
    }

    public static int points(int normalKills, int finalKills) {
        return Math.max(0, normalKills) + Math.max(0, finalKills) * 3;
    }

    private static NbtCompound root(ServerPlayerEntity player) {
        return ((GemsPersistentDataHolder) player).gems$getPersistentData();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

