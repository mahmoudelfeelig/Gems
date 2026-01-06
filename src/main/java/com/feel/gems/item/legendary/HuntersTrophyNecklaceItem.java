package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.PlayerStateManager;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Hunter's Trophy Necklace - on player kill, permanently gain one of the victim's random gem passives.
 * The passive persists through death/logout.
 */
public final class HuntersTrophyNecklaceItem extends Item implements LegendaryItem {
    private static final String TROPHY_PASSIVE_KEY = "trophy_necklace_passive";
    private static final Random RANDOM = new Random();

    public HuntersTrophyNecklaceItem(Settings settings) {
        super(settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "hunters_trophy_necklace").toString();
    }

    /**
     * Called when the holder kills another player.
     * Captures one of their random passives permanently.
     */
    public static void onKillPlayer(ServerPlayerEntity killer, ServerPlayerEntity victim) {
        // Check if killer has the necklace in inventory
        if (!hasNecklace(killer)) return;

        // Get victim's passives and pick a random one
        // This would integrate with GemPowers to get victim's passives
        Identifier stolenPassive = pickRandomPassive(victim);
        if (stolenPassive == null) return;

        // Store permanently (persists through death/logout)
        PlayerStateManager.setPersistent(killer, TROPHY_PASSIVE_KEY, stolenPassive.toString());

        killer.sendMessage(Text.translatable("gems.item.trophy_necklace.captured", stolenPassive.getPath()).formatted(Formatting.GOLD), false);
    }

    private static boolean hasNecklace(ServerPlayerEntity player) {
        for (ItemStack stack : player.getInventory().getMainStacks()) {
            if (stack.getItem() instanceof HuntersTrophyNecklaceItem) {
                return true;
            }
        }
        return player.getOffHandStack().getItem() instanceof HuntersTrophyNecklaceItem;
    }

    private static Identifier pickRandomPassive(ServerPlayerEntity victim) {
        List<Identifier> victimPassives = GemPowers.getActivePassives(victim);
        if (victimPassives.isEmpty()) return null;
        return victimPassives.get(RANDOM.nextInt(victimPassives.size()));
    }

    public static Identifier getTrophyPassive(ServerPlayerEntity player) {
        String passiveStr = PlayerStateManager.getPersistent(player, TROPHY_PASSIVE_KEY);
        if (passiveStr == null || passiveStr.isEmpty()) return null;
        return Identifier.tryParse(passiveStr);
    }

    public static boolean hasTrophyPassive(ServerPlayerEntity player, Identifier passiveId) {
        Identifier trophy = getTrophyPassive(player);
        return trophy != null && trophy.equals(passiveId);
    }

    /**
     * Note: Unlike the Hunter gem's Trophy Hunter passive, this one is PERSISTENT.
     * It does NOT clear on death or logout.
     */
    public static void clearTrophy(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, TROPHY_PASSIVE_KEY);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        tooltip.accept(Text.translatable("gems.item.trophy_necklace.tooltip.1").formatted(Formatting.GRAY));
        tooltip.accept(Text.translatable("gems.item.trophy_necklace.tooltip.2").formatted(Formatting.GRAY));
        tooltip.accept(Text.translatable("gems.item.trophy_necklace.tooltip.3").formatted(Formatting.GOLD));
    }
}
