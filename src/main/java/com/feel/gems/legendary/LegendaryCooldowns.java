package com.feel.gems.legendary;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.item.ModItems;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.server.network.ServerPlayerEntity;

public final class LegendaryCooldowns {
    private LegendaryCooldowns() {
    }

    /**
     * Gets the cooldown multiplier for abilities. Chrono Charms stack multiplicatively.
     * 1 charm = 0.5x (50% cooldown), 2 charms = 0.25x (25% cooldown), etc.
     */
    public static float getCooldownMultiplier(ServerPlayerEntity player) {
        if (player == null) {
            return 1.0F;
        }
        int charmCount = countChronoCharms(player);
        if (charmCount <= 0) {
            return 1.0F;
        }
        float baseMultiplier = GemsBalance.v().legendary().chronoCharmCooldownMultiplier();
        // Stack multiplicatively: 2 charms = mult^2, 3 charms = mult^3, etc.
        return (float) Math.pow(baseMultiplier, charmCount);
    }

    /**
     * Counts how many Chrono Charms the player has in their inventory.
     */
    public static int countChronoCharms(ServerPlayerEntity player) {
        if (player == null) {
            return 0;
        }
        int count = 0;
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(ModItems.CHRONO_CHARM)) {
                count += inv.getStack(i).getCount();
            }
        }
        return count;
    }

    public static boolean hasChronoCharm(ServerPlayerEntity player) {
        return countChronoCharms(player) > 0;
    }
}
