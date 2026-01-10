package com.feel.gems.legendary;

import com.feel.gems.item.ModItems;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;

public final class LegendaryCooldowns {
    private LegendaryCooldowns() {
    }

    public static float getCooldownMultiplier(ServerPlayerEntity player) {
        if (player == null) {
            return 1.0F;
        }
        return hasChronoCharm(player) ? 0.5F : 1.0F;
    }

    public static boolean hasChronoCharm(ServerPlayerEntity player) {
        if (player == null) {
            return false;
        }
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(ModItems.CHRONO_CHARM)) {
                return true;
            }
        }
        return false;
    }
}
