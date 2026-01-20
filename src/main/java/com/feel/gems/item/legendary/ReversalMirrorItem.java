package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.admin.GemsAdmin;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.state.PlayerStateManager;
import java.util.function.Consumer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

/**
 * Reversal Mirror - right-click to activate for 5s; all incoming damage is reflected back to the attacker.
 * Works on all types of attacks (not just projectiles).
 */
public final class ReversalMirrorItem extends Item implements LegendaryItem {
    private static final String ACTIVE_END_KEY = "reversal_mirror_end";
    private static final ThreadLocal<Boolean> REFLECTING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public ReversalMirrorItem(Settings settings) {
        super(settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "reversal_mirror").toString();
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

        // Activate mirror
        int durationTicks = GemsBalance.v().legendary().reversalMirrorDurationTicks();
        long endTime = world.getTime() + Math.max(0, durationTicks);
        PlayerStateManager.setPersistent(player, ACTIVE_END_KEY, String.valueOf(endTime));

        int cooldownTicks = GemsBalance.v().legendary().reversalMirrorCooldownTicks();
        if (cooldownTicks > 0 && !GemsAdmin.noLegendaryCooldowns(player)) {
            player.getItemCooldownManager().set(stack, cooldownTicks);
        }
        world.playSound(null, player.getX(), player.getY(), player.getZ(), 
                SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0F, 1.5F);

        player.sendMessage(Text.translatable("gems.item.reversal_mirror.activated").formatted(Formatting.GOLD), true);
        return ActionResult.SUCCESS;
    }

    public static boolean isActive(ServerPlayerEntity player) {
        String endStr = PlayerStateManager.getPersistent(player, ACTIVE_END_KEY);
        if (endStr == null) return false;

        long endTime = Long.parseLong(endStr);
        if (player.getEntityWorld().getTime() > endTime) {
            clearActive(player);
            return false;
        }

        return true;
    }

    public static void clearActive(ServerPlayerEntity player) {
        PlayerStateManager.clearPersistent(player, ACTIVE_END_KEY);
    }

    /**
     * Called when the player takes damage while mirror is active.
     * Reflects the damage back to the attacker.
     */
    public static void reflectDamage(ServerPlayerEntity victim, Entity attacker, float amount, ServerWorld world) {
        tryReflectDamage(victim, attacker, amount, world);
    }

    /**
     * Attempts to reflect damage back to the attacker.
     *
     * @return true when damage was reflected
     */
    public static boolean tryReflectDamage(ServerPlayerEntity victim, Entity attacker, float amount, ServerWorld world) {
        if (!isActive(victim)) return false;
        if (amount <= 0.0F) return false;
        if (attacker == null || !(attacker instanceof LivingEntity living)) return false;
        if (living == victim) return false;

        // Prevent infinite reflection loops (e.g. two players both have mirrors active).
        if (REFLECTING.get()) {
            return false;
        }

        REFLECTING.set(Boolean.TRUE);
        try {
            living.damage(world, victim.getDamageSources().thorns(victim), amount);
        } finally {
            REFLECTING.set(Boolean.FALSE);
        }

        world.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0F, 1.2F);
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        tooltip.accept(Text.translatable("gems.item.reversal_mirror.tooltip.1").formatted(Formatting.GRAY));
        tooltip.accept(Text.translatable("gems.item.reversal_mirror.tooltip.2").formatted(Formatting.DARK_GRAY));
        tooltip.accept(Text.translatable("gems.item.reversal_mirror.tooltip.3").formatted(Formatting.DARK_GRAY));
    }
}
