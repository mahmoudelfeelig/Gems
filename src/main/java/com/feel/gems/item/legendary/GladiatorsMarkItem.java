package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.power.util.Targeting;
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
import net.minecraft.world.World;

/**
 * Gladiator's Mark - brand a player; both you and the marked player deal 50% more damage 
 * to each other for 60s.
 */
public final class GladiatorsMarkItem extends Item implements LegendaryItem {
    private static final String MARK_TARGET_KEY = "gladiators_mark_target";
    private static final String MARK_END_KEY = "gladiators_mark_end";

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
        int rangeBlocks = GemsBalance.v().legendary().gladiatorsMarkRangeBlocks();
        ServerPlayerEntity target = Targeting.raycastPlayer(player, rangeBlocks);
        if (target == null) {
            player.sendMessage(Text.translatable("gems.message.no_player_target").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        if (target == player) {
            return ActionResult.FAIL;
        }

        int durationTicks = GemsBalance.v().legendary().gladiatorsMarkDurationTicks();
        long endTime = world.getTime() + Math.max(0, durationTicks);

        // Mark both players as linked
        PlayerStateManager.setPersistent(player, MARK_TARGET_KEY, target.getUuidAsString());
        PlayerStateManager.setPersistent(player, MARK_END_KEY, String.valueOf(endTime));

        PlayerStateManager.setPersistent(target, MARK_TARGET_KEY, player.getUuidAsString());
        PlayerStateManager.setPersistent(target, MARK_END_KEY, String.valueOf(endTime));

        int cooldownTicks = GemsBalance.v().legendary().gladiatorsMarkCooldownTicks();
        if (cooldownTicks > 0) {
            player.getItemCooldownManager().set(stack, cooldownTicks);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.5F, 1.5F);
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 0.5F, 1.5F);

        player.sendMessage(Text.translatable("gems.item.gladiators_mark.marked", target.getName().getString()).formatted(Formatting.RED), true);
        target.sendMessage(Text.translatable("gems.item.gladiators_mark.marked_victim", player.getName().getString()).formatted(Formatting.RED), true);

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
        return GemsBalance.v().legendary().gladiatorsMarkDamageMultiplier();
    }

    public static void clearMark(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, MARK_TARGET_KEY);
        PlayerStateManager.clearPersistent(player, MARK_END_KEY);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        tooltip.accept(Text.translatable("gems.item.gladiators_mark.tooltip.1").formatted(Formatting.GRAY));
        tooltip.accept(Text.translatable("gems.item.gladiators_mark.tooltip.2").formatted(Formatting.DARK_GRAY));
        tooltip.accept(Text.translatable("gems.item.gladiators_mark.tooltip.3").formatted(Formatting.DARK_GRAY));
    }
}
