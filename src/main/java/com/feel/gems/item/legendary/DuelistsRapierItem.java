package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.state.PlayerStateManager;
import java.util.function.Consumer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

/**
 * Duelist's Rapier - parry window on right-click; successful parry grants guaranteed crit on next hit.
 */
public final class DuelistsRapierItem extends Item implements LegendaryItem {
    private static final String PARRY_WINDOW_END_KEY = "duelists_rapier_parry_end";
    private static final String CRIT_READY_KEY = "duelists_rapier_crit_ready";

    public DuelistsRapierItem(ToolMaterial material, Settings settings) {
        super(settings.sword(material, 2.0F, -2.0F).enchantable(15)); // Faster attack speed, slightly less damage
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "duelists_rapier").toString();
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient() || !(user instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }

        ItemStack stack = player.getStackInHand(hand);

        // Check cooldown
        if (player.getItemCooldownManager().isCoolingDown(stack)) {
            return ActionResult.FAIL;
        }

        // Activate parry window
        int windowTicks = GemsBalance.v().legendary().duelistsRapierParryWindowTicks();
        long parryEnd = world.getTime() + Math.max(0, windowTicks);
        PlayerStateManager.setPersistent(player, PARRY_WINDOW_END_KEY, String.valueOf(parryEnd));

        int cooldownTicks = GemsBalance.v().legendary().duelistsRapierCooldownTicks();
        if (cooldownTicks > 0) {
            player.getItemCooldownManager().set(stack, cooldownTicks);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, SoundCategory.PLAYERS, 1.0F, 1.5F);

        player.sendMessage(Text.translatable("gems.item.duelists_rapier.parry_ready").formatted(Formatting.YELLOW), true);
        return ActionResult.SUCCESS;
    }

    /**
     * Check if player is in parry window.
     */
    public static boolean isInParryWindow(ServerPlayerEntity player) {
        String endStr = PlayerStateManager.getPersistent(player, PARRY_WINDOW_END_KEY);
        if (endStr == null) return false;

        long endTime = Long.parseLong(endStr);
        if (player.getEntityWorld().getTime() > endTime) {
            clearParryWindow(player);
            return false;
        }

        return true;
    }

    /**
     * Called when a successful parry occurs (player blocked during parry window).
     */
    public static void onSuccessfulParry(ServerPlayerEntity player) {
        clearParryWindow(player);
        PlayerStateManager.setPersistent(player, CRIT_READY_KEY, "true");

        player.sendMessage(Text.translatable("gems.item.duelists_rapier.perfect_parry").formatted(Formatting.GOLD), true);
        player.getEntityWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0F, 1.2F);
    }

    /**
     * Check and consume guaranteed crit.
     */
    public static boolean hasAndConsumeGuaranteedCrit(ServerPlayerEntity player) {
        String ready = PlayerStateManager.getPersistent(player, CRIT_READY_KEY);
        if (!"true".equals(ready)) return false;

        PlayerStateManager.clearPersistent(player, CRIT_READY_KEY);
        return true;
    }

    public static void clearParryWindow(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, PARRY_WINDOW_END_KEY);
    }

    public static void clearAll(ServerPlayerEntity player) {
        clearParryWindow(player);
        PlayerStateManager.clearPersistent(player, CRIT_READY_KEY);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        tooltip.accept(Text.translatable("gems.item.duelists_rapier.tooltip.1").formatted(Formatting.GRAY));
        tooltip.accept(Text.translatable("gems.item.duelists_rapier.tooltip.2").formatted(Formatting.DARK_GRAY));
        tooltip.accept(Text.translatable("gems.item.duelists_rapier.tooltip.3").formatted(Formatting.DARK_GRAY));
    }
}
