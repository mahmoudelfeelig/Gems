package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.HypnoControl;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.state.GemsPersistentDataHolder;
import com.feel.gems.util.GemsNbt;
import com.feel.gems.util.GemsTooltipFormat;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;




public final class HypnoStaffItem extends Item implements LegendaryItem {
    private static final String KEY_TARGET = "legendaryHypnoTarget";
    private static final String KEY_PROGRESS = "legendaryHypnoProgress";

    public HypnoStaffItem(Settings settings) {
        super(settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "hypno_staff").toString();
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ActionResult use(World world, net.minecraft.entity.player.PlayerEntity user, Hand hand) {
        user.setCurrentHand(hand);
        return ActionResult.CONSUME;
    }

    @Override
    public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (world.isClient()) {
            return;
        }
        if (!(user instanceof ServerPlayerEntity player)) {
            return;
        }
        var cfg = GemsBalance.v().legendary();
        int range = cfg.hypnoRangeBlocks();
        double viewRange = Math.max(4.0D, cfg.hypnoViewRangeBlocks());
        LivingEntity target = Targeting.raycastLiving(player, range, viewRange);
        if (!(target instanceof MobEntity mob) || !HypnoControl.isAllowed(mob)) {
            resetProgress(player);
            return;
        }

        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        UUID current = GemsNbt.getUuid(data, KEY_TARGET);
        if (current == null || !current.equals(mob.getUuid())) {
            GemsNbt.putUuid(data, KEY_TARGET, mob.getUuid());
            data.putInt(KEY_PROGRESS, 1);
            return;
        }

        int progress = data.getInt(KEY_PROGRESS, 0) + 1;
        data.putInt(KEY_PROGRESS, progress);

        int holdTicks = cfg.hypnoHoldTicks();
        sendProgress(player, progress, holdTicks);
        if (progress < holdTicks) {
            return;
        }
        if (HypnoControl.isHypno(mob) && player.getUuid().equals(HypnoControl.ownerUuid(mob))) {
            float heal = cfg.hypnoHealHearts() * 2.0F;
            if (heal > 0.0F) {
                mob.heal(heal);
            }
            player.sendMessage(Text.translatable("gems.item.hypno_staff.refreshed"), true);
            player.stopUsingItem();
            resetProgress(player);
            return;
        }
        boolean controlled = HypnoControl.tryControl(player, mob);
        if (controlled) {
            player.sendMessage(Text.translatable("gems.item.hypno_staff.successful"), true);
            player.stopUsingItem();
        } else {
            player.sendMessage(Text.translatable("gems.item.hypno_staff.failed"), true);
        }
        resetProgress(player);
    }

    @Override
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (world.isClient()) {
            return false;
        }
        if (user instanceof ServerPlayerEntity player) {
            resetProgress(player);
        }
        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        GemsTooltipFormat.appendDescription(tooltip, Text.translatable("item.gems.hypno_staff.desc"));
    }

    private static void resetProgress(ServerPlayerEntity player) {
        NbtCompound data = ((GemsPersistentDataHolder) player).gems$getPersistentData();
        data.remove(KEY_TARGET);
        data.remove(KEY_PROGRESS);
    }

    private static void sendProgress(ServerPlayerEntity player, int progress, int holdTicks) {
        if (holdTicks <= 0) {
            return;
        }
        int clamped = Math.min(progress, holdTicks);
        int percent = (int) Math.floor((clamped / (double) holdTicks) * 100.0D);
        int bars = 10;
        int filled = (int) Math.round((clamped / (double) holdTicks) * bars);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < bars; i++) {
            bar.append(i < filled ? "=" : " ");
        }
        bar.append("]");
        player.sendMessage(Text.translatable("gems.item.hypno_staff.progress", bar.toString(), percent), true);
    }
}
