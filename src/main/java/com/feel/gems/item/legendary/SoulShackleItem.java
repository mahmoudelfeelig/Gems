package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.admin.GemsAdmin;
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
import java.util.UUID;

/**
 * Soul Shackle - link yourself to an enemy; damage you take is split between you both for 10s.
 */
public final class SoulShackleItem extends Item implements LegendaryItem {
    private static final String SHACKLE_TARGET_KEY = "soul_shackle_target";
    private static final String SHACKLE_END_KEY = "soul_shackle_end";

    public SoulShackleItem(Settings settings) {
        super(settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "soul_shackle").toString();
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient() || !(user instanceof ServerPlayerEntity player)) {
            return ActionResult.PASS;
        }

        ItemStack stack = player.getStackInHand(hand);

        // Check cooldown
        if (player.getItemCooldownManager().isCoolingDown(stack) && !GemsAdmin.noLegendaryCooldowns(player)) {
            return ActionResult.FAIL;
        }

        // Raycast to find target
        int rangeBlocks = GemsBalance.v().legendary().soulShackleRangeBlocks();
        ServerPlayerEntity target = Targeting.raycastPlayer(player, rangeBlocks);
        if (target == null) {
            player.sendMessage(Text.translatable("gems.message.no_player_target").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        if (target == player) {
            return ActionResult.FAIL;
        }

        int durationTicks = GemsBalance.v().legendary().soulShackleDurationTicks();
        long endTime = world.getTime() + Math.max(0, durationTicks);

        // Link player to target
        PlayerStateManager.setPersistent(player, SHACKLE_TARGET_KEY, target.getUuidAsString());
        PlayerStateManager.setPersistent(player, SHACKLE_END_KEY, String.valueOf(endTime));

        int cooldownTicks = GemsBalance.v().legendary().soulShackleCooldownTicks();
        if (cooldownTicks > 0 && !GemsAdmin.noLegendaryCooldowns(player)) {
            player.getItemCooldownManager().set(stack, cooldownTicks);
        }

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1.0F, 0.8F);
        world.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 1.0F, 0.8F);

        player.sendMessage(Text.translatable("gems.item.soul_shackle.linked", target.getName().getString()).formatted(Formatting.DARK_PURPLE), true);
        target.sendMessage(Text.translatable("gems.item.soul_shackle.linked_victim", player.getName().getString()).formatted(Formatting.DARK_PURPLE), true);

        return ActionResult.SUCCESS;
    }

    /**
     * Called when the shackle holder takes damage.
     * Returns the amount to transfer to the linked target.
     */
    public static float getDamageToTransfer(ServerPlayerEntity holder, float damage) {
        String endStr = PlayerStateManager.getPersistent(holder, SHACKLE_END_KEY);
        if (endStr == null) return 0;

        long endTime = Long.parseLong(endStr);
        if (holder.getEntityWorld().getTime() > endTime) {
            clearShackle(holder);
            return 0;
        }

        return damage * GemsBalance.v().legendary().soulShackleSplitRatio();
    }

    public static ServerPlayerEntity getShackledTarget(ServerPlayerEntity holder) {
        String targetStr = PlayerStateManager.getPersistent(holder, SHACKLE_TARGET_KEY);
        if (targetStr == null || targetStr.isEmpty()) return null;

        String endStr = PlayerStateManager.getPersistent(holder, SHACKLE_END_KEY);
        if (endStr == null) return null;

        long endTime = Long.parseLong(endStr);
        if (holder.getEntityWorld().getTime() > endTime) {
            clearShackle(holder);
            return null;
        }

        try {
            UUID targetId = UUID.fromString(targetStr);
            return holder.getEntityWorld().getServer().getPlayerManager().getPlayer(targetId);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean hasShackle(ServerPlayerEntity player) {
        return getShackledTarget(player) != null;
    }

    public static void clearShackle(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, SHACKLE_TARGET_KEY);
        PlayerStateManager.clearPersistent(player, SHACKLE_END_KEY);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        tooltip.accept(Text.translatable("gems.item.soul_shackle.tooltip.1").formatted(Formatting.GRAY));
        tooltip.accept(Text.translatable("gems.item.soul_shackle.tooltip.2").formatted(Formatting.DARK_GRAY));
        tooltip.accept(Text.translatable("gems.item.soul_shackle.tooltip.3").formatted(Formatting.DARK_GRAY));
    }
}
