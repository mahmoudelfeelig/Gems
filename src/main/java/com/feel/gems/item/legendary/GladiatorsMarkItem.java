package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.state.PlayerStateManager;
import java.util.function.Consumer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

/**
 * Gladiator's Mark - brand a player; both you and the marked player deal 50% more damage 
 * to each other for 60s.
 */
public final class GladiatorsMarkItem extends Item implements LegendaryItem {
    private static final String MARK_TARGET_KEY = "gladiators_mark_target";
    private static final String MARK_END_KEY = "gladiators_mark_end";
    private static final int DURATION_TICKS = 60 * 20;
    private static final int COOLDOWN_TICKS = 120 * 20;
    private static final float DAMAGE_MULTIPLIER = 1.5F;

    public GladiatorsMarkItem(Settings settings) {
        super(settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "gladiators_mark").toString();
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

        // Raycast to find target
        HitResult hit = player.raycast(20, 0.0F, false);
        if (!(hit instanceof EntityHitResult entityHit) || !(entityHit.getEntity() instanceof ServerPlayerEntity target)) {
            player.sendMessage(Text.literal("No player target found").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        if (target == player) {
            return ActionResult.FAIL;
        }

        long endTime = world.getTime() + DURATION_TICKS;

        // Mark both players as linked
        PlayerStateManager.setPersistent(player, MARK_TARGET_KEY, target.getUuidAsString());
        PlayerStateManager.setPersistent(player, MARK_END_KEY, String.valueOf(endTime));

        PlayerStateManager.setPersistent(target, MARK_TARGET_KEY, player.getUuidAsString());
        PlayerStateManager.setPersistent(target, MARK_END_KEY, String.valueOf(endTime));

        player.getItemCooldownManager().set(stack, COOLDOWN_TICKS);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.5F, 1.5F);
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.5F, 1.5F);

        player.sendMessage(Text.literal("Gladiator's Mark: You and " + target.getName().getString() + " deal 50% more damage to each other!").formatted(Formatting.RED), true);
        target.sendMessage(Text.literal("Gladiator's Mark: " + player.getName().getString() + " has marked you! 50% more damage between you!").formatted(Formatting.RED), true);

        return ActionResult.SUCCESS;
    }

    public static boolean isMarkedAgainst(ServerPlayerEntity attacker, ServerPlayerEntity target) {
        String markedTarget = PlayerStateManager.getPersistent(attacker, MARK_TARGET_KEY);
        if (markedTarget == null || !markedTarget.equals(target.getUuidAsString())) {
            return false;
        }

        String endStr = PlayerStateManager.getPersistent(attacker, MARK_END_KEY);
        if (endStr == null) return false;

        long endTime = Long.parseLong(endStr);
        if (attacker.getEntityWorld().getTime() > endTime) {
            clearMark(attacker);
            return false;
        }

        return true;
    }

    public static float getDamageMultiplier() {
        return DAMAGE_MULTIPLIER;
    }

    public static void clearMark(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, MARK_TARGET_KEY);
        PlayerStateManager.clearPersistent(player, MARK_END_KEY);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        tooltip.accept(Text.literal("Right-click a player to brand them").formatted(Formatting.GRAY));
        tooltip.accept(Text.literal("Both of you deal 50% more damage to each other").formatted(Formatting.DARK_GRAY));
        tooltip.accept(Text.literal("Lasts 60 seconds, 2 minute cooldown").formatted(Formatting.DARK_GRAY));
    }
}
