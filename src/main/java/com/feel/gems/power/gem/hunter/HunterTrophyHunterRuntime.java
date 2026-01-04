package com.feel.gems.power.gem.hunter;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.api.GemPassive;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Trophy Hunter passive - on player kill, gain one of their random passives temporarily.
 * The gained passive persists through death.
 */
public final class HunterTrophyHunterRuntime {
    private static final String TROPHY_PASSIVE_KEY = "hunter_trophy_passive";
    private static final String TROPHY_END_KEY = "hunter_trophy_end";
    private static final Random RANDOM = new Random();

    private HunterTrophyHunterRuntime() {}

    public static void onPlayerKill(ServerPlayerEntity hunter, ServerPlayerEntity victim) {
        // Get victim's active passives
        List<Identifier> victimPassives = getVictimPassives(victim);
        if (victimPassives.isEmpty()) return;

        // Pick a random passive
        Identifier stolenPassive = victimPassives.get(RANDOM.nextInt(victimPassives.size()));

        int durationTicks = GemsBalance.v().hunter().trophyHunterDurationTicks();
        long endTime = hunter.getEntityWorld().getTime() + durationTicks;

        PlayerStateManager.setPersistent(hunter, TROPHY_PASSIVE_KEY, stolenPassive.toString());
        PlayerStateManager.setPersistent(hunter, TROPHY_END_KEY, String.valueOf(endTime));
    }

    private static List<Identifier> getVictimPassives(ServerPlayerEntity victim) {
        // This would integrate with GemPowers to get the victim's active passives
        // For now, return an empty list - the actual implementation would query the power system
        List<Identifier> passives = new ArrayList<>();
        // TODO: Integrate with GemPowers.getActivePassives(victim)
        return passives;
    }

    public static Identifier getTrophyPassive(ServerPlayerEntity hunter) {
        String passiveStr = PlayerStateManager.getPersistent(hunter, TROPHY_PASSIVE_KEY);
        if (passiveStr == null || passiveStr.isEmpty()) return null;

        String endStr = PlayerStateManager.getPersistent(hunter, TROPHY_END_KEY);
        if (endStr == null) return null;

        long endTime = Long.parseLong(endStr);
        if (hunter.getEntityWorld().getTime() > endTime) {
            clearTrophy(hunter);
            return null;
        }

        return Identifier.tryParse(passiveStr);
    }

    public static boolean hasTrophyPassive(ServerPlayerEntity hunter, Identifier passiveId) {
        Identifier trophy = getTrophyPassive(hunter);
        return trophy != null && trophy.equals(passiveId);
    }

    public static void clearTrophy(ServerPlayerEntity hunter) {
        PlayerStateManager.clearPersistent(hunter, TROPHY_PASSIVE_KEY);
        PlayerStateManager.clearPersistent(hunter, TROPHY_END_KEY);
    }
}
