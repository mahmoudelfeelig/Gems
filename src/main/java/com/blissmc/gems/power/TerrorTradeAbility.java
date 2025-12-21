package com.feel.gems.power;

import com.feel.gems.assassin.AssassinState;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.state.GemPlayerState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class TerrorTradeAbility implements GemAbility {
    private static final String KEY_USES = "terrorTradeUses";

    @Override
    public Identifier id() {
        return PowerIds.TERROR_TRADE;
    }

    @Override
    public String name() {
        return "Terror Trade";
    }

    @Override
    public String description() {
        return "Sacrifice yourself to attempt to kill a targeted player. Target totems will still save them.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().terror().terrorTradeCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        int maxUses = GemsBalance.v().terror().terrorTradeMaxUses();
        int used = com.feel.gems.state.PlayerNbt.getInt(player, KEY_USES, 0);
        if (used >= maxUses) {
            player.sendMessage(Text.literal("Terror Trade has no uses remaining (" + maxUses + ")."), true);
            return false;
        }

        LivingEntity target = Targeting.raycastLiving(player, GemsBalance.v().terror().terrorTradeRangeBlocks());
        if (!(target instanceof ServerPlayerEntity victim)) {
            player.sendMessage(Text.literal("No player target."), true);
            return false;
        }
        if (victim == player) {
            return false;
        }

        // Apply the permanent costs immediately so they persist through the death.
        applyCosts(player);
        com.feel.gems.state.PlayerNbt.putInt(player, KEY_USES, used + 1);

        // Attempt to kill the target (totems may save them).
        victim.damage(player.getDamageSources().magic(), 10_000.0F);
        AbilityFeedback.beam(player.getServerWorld(), player.getEyePos(), victim.getPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.SOUL_FIRE_FLAME, 24);
        AbilityFeedback.burstAt(player.getServerWorld(), victim.getPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.FLASH, 1, 0.0D);
        AbilityFeedback.sound(player, SoundEvents.ITEM_TOTEM_USE, 0.7F, 0.9F);

        // Kill the caster even if they have a totem.
        player.damage(player.getDamageSources().outOfWorld(), 10_000.0F);
        return true;
    }

    private static void applyCosts(ServerPlayerEntity player) {
        GemPlayerState.initIfNeeded(player);
        AssassinState.initIfNeeded(player);

        int heartsCost = GemsBalance.v().terror().terrorTradeHeartsCost();
        int energyPenalty = GemsBalance.v().terror().terrorTradePermanentEnergyPenalty();

        if (AssassinState.isAssassin(player)) {
            int after = AssassinState.addAssassinHearts(player, -heartsCost);
            if (after <= 0) {
                AssassinState.setEliminated(player, true);
            }
        } else {
            int before = GemPlayerState.getMaxHearts(player);
            GemPlayerState.setMaxHearts(player, Math.max(GemPlayerState.MIN_MAX_HEARTS, before - heartsCost));
        }
        GemPlayerState.applyMaxHearts(player);

        GemPlayerState.addEnergyCapPenalty(player, energyPenalty);
        GemPowers.sync(player);
        GemStateSync.send(player);
    }
}

