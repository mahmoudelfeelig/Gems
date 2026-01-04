package com.feel.gems.power.ability.hunter;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.state.PlayerStateManager;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.UUID;

public final class HunterPackTacticsRuntime {
    private static final String PACK_TARGET_KEY = "hunter_pack_tactics_target";
    private static final String PACK_END_KEY = "hunter_pack_tactics_end";

    private HunterPackTacticsRuntime() {}

    public static void grantBuff(ServerPlayerEntity player, UUID targetId, int durationTicks) {
        long endTime = player.getEntityWorld().getTime() + durationTicks;
        PlayerStateManager.setPersistent(player, PACK_TARGET_KEY, targetId.toString());
        PlayerStateManager.setPersistent(player, PACK_END_KEY, String.valueOf(endTime));
    }

    public static boolean hasBuffAgainst(ServerPlayerEntity attacker, UUID targetId) {
        String storedTarget = PlayerStateManager.getPersistent(attacker, PACK_TARGET_KEY);
        if (storedTarget == null || !storedTarget.equals(targetId.toString())) {
            return false;
        }

        String endStr = PlayerStateManager.getPersistent(attacker, PACK_END_KEY);
        if (endStr == null) return false;

        long endTime = Long.parseLong(endStr);
        if (attacker.getEntityWorld().getTime() > endTime) {
            clearBuff(attacker);
            return false;
        }

        return true;
    }

    public static float getDamageMultiplier() {
        return GemsBalance.v().hunter().packTacticsBonusDamageMultiplier();
    }

    public static void clearBuff(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, PACK_TARGET_KEY);
        PlayerStateManager.clearPersistent(player, PACK_END_KEY);
    }
}
