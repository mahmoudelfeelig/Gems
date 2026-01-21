package com.feel.gems.item.legendary;

import com.feel.gems.GemsMod;
import com.feel.gems.admin.GemsAdmin;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.legendary.LegendaryItem;
import com.feel.gems.legendary.LegendaryDuels;
import com.feel.gems.power.util.Targeting;
import com.feel.gems.util.GemsTooltipFormat;
import java.util.function.Consumer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.particle.ParticleTypes;
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
 * Challenger's Gauntlet - right-click a player to challenge them to a duel.
 * Both players are teleported to a small arena, winner gets energy; loser loses energy.
 * 
 * Note: This item creates a temporary 1v1 duel scenario - the actual arena implementation
 * is handled server-side by {@link LegendaryDuels}.
 */
public final class ChallengersGauntletItem extends Item implements LegendaryItem {
    public ChallengersGauntletItem(Settings settings) {
        super(settings);
    }

    @Override
    public String legendaryId() {
        return Identifier.of(GemsMod.MOD_ID, "challengers_gauntlet").toString();
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
        int rangeBlocks = GemsBalance.v().legendary().challengersGauntletRangeBlocks();
        ServerPlayerEntity target = Targeting.raycastPlayer(player, rangeBlocks);
        if (target == null) {
            player.sendMessage(Text.translatable("gems.message.no_player_target").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        if (target == player) {
            return ActionResult.FAIL;
        }

        if (!LegendaryDuels.startGauntletDuel(player, target)) {
            player.sendMessage(Text.translatable("gems.item.challengers_gauntlet.unavailable").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        int cooldownTicks = GemsBalance.v().legendary().challengersGauntletCooldownTicks();
        if (cooldownTicks > 0 && !GemsAdmin.noLegendaryCooldowns(player)) {
            player.getItemCooldownManager().set(stack, cooldownTicks);
        }

        // Visual effects
        ServerWorld serverWorld = player.getEntityWorld();
        serverWorld.spawnParticles(ParticleTypes.FLAME, 
                player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
        serverWorld.spawnParticles(ParticleTypes.FLAME, 
                target.getX(), target.getY() + 1, target.getZ(), 20, 0.5, 0.5, 0.5, 0.1);

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_GOAT_HORN_BREAK, SoundCategory.PLAYERS, 1.0F, 1.0F);

        player.sendMessage(Text.translatable("gems.item.challengers_gauntlet.challenged", target.getName().getString()).formatted(Formatting.GOLD), false);
        target.sendMessage(Text.translatable("gems.item.challengers_gauntlet.challenged_victim", player.getName().getString()).formatted(Formatting.GOLD), false);
        target.sendMessage(Text.translatable("gems.item.challengers_gauntlet.duel_begin").formatted(Formatting.YELLOW), false);

        // Note: Actual arena teleportation would be implemented in a separate system
        return ActionResult.SUCCESS;
    }

    public static void clearChallenge(ServerPlayerEntity player) {
        // No-op: duel state is stored server-wide, not on the player.
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> tooltip, TooltipType type) {
        GemsTooltipFormat.appendDescription(
                tooltip,
                Text.translatable("gems.item.challengers_gauntlet.tooltip.1"),
                Text.translatable("gems.item.challengers_gauntlet.tooltip.2"),
                Text.translatable("gems.item.challengers_gauntlet.tooltip.3")
        );
    }
}
