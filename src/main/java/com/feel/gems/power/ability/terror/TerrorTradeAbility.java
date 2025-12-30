package com.feel.gems.power.ability.terror;

import com.feel.gems.assassin.AssassinState;
import com.feel.gems.config.GemsBalance;
import com.feel.gems.item.GemOwnership;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.power.api.GemAbility;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityFeedback;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.power.util.Targeting;
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
        return "Sacrifice yourself to attempt to kill a targeted enemy. Target totems will still save players.";
    }

    @Override
    public int cooldownTicks() {
        return GemsBalance.v().terror().terrorTradeCooldownTicks();
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        LivingEntity target = Targeting.raycastLiving(player, GemsBalance.v().terror().terrorTradeRangeBlocks());
        if (target == null) {
            player.sendMessage(Text.literal("No target."), true);
            return false;
        }
        if (target == player) {
            return false;
        }

        AssassinState.initIfNeeded(player);

        if (!(target instanceof ServerPlayerEntity victim)) {
            return activateMobTrade(player, target);
        }
        boolean casterIsAssassin = AssassinState.isAssassin(player);
        if (casterIsAssassin) {
            return activateAssassinTrade(player, victim);
        }
        return activateNormalTrade(player, victim);
    }

    private static boolean activateMobTrade(ServerPlayerEntity player, LivingEntity target) {
        var world = player.getServerWorld();
        AbilityFeedback.beam(world, player.getEyePos(), target.getPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.SOUL_FIRE_FLAME, 24);
        AbilityFeedback.burstAt(world, target.getPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.FLASH, 1, 0.0D);
        AbilityFeedback.sound(player, SoundEvents.ITEM_TOTEM_USE, 0.7F, 0.9F);

        target.damage(target.getDamageSources().playerAttack(player), 10_000.0F);
        player.damage(player.getDamageSources().outOfWorld(), 10_000.0F);
        return true;
    }

    private static boolean activateNormalTrade(ServerPlayerEntity player, ServerPlayerEntity victim) {
        // Normal players: no uses/costs/energy penalties; target totems still save them.
        // The target pays the "heart cost" by losing 2 max-hearts (instead of the normal 1-heart drop on death).
        boolean skipDropApplied = applyTargetPenaltyForNormalTrade(victim);
        GemPowers.sync(victim);
        GemStateSync.send(victim);

        var world = player.getServerWorld();
        AbilityFeedback.beam(world, player.getEyePos(), victim.getPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.SOUL_FIRE_FLAME, 24);
        AbilityFeedback.burstAt(world, victim.getPos().add(0.0D, 1.0D, 0.0D), ParticleTypes.FLASH, 1, 0.0D);
        AbilityFeedback.sound(player, SoundEvents.ITEM_TOTEM_USE, 0.7F, 0.9F);

        // Attempt to kill the target (totems may save them).
        // Use a source attributed to the caster so kills count normally (energy, scoring, etc.);
        // Totems can still save the target.
        victim.damage(victim.getDamageSources().playerAttack(player), 10_000.0F);
        if (victim.isAlive() && skipDropApplied) {
            // The target survived (most likely via totem); don't suppress their next heart drop.
            GemOwnership.consumeSkipHeartDrop(victim);
        }

        // Kill the caster even if they have a totem.
        player.damage(player.getDamageSources().outOfWorld(), 10_000.0F);
        return true;
    }

    private static boolean applyTargetPenaltyForNormalTrade(ServerPlayerEntity victim) {
        GemPlayerState.initIfNeeded(victim);
        AssassinState.initIfNeeded(victim);

        int penalty = GemsBalance.v().terror().terrorTradeNormalTargetHeartsPenalty();
        if (AssassinState.isAssassin(victim)) {
            int after = AssassinState.addAssassinHearts(victim, -penalty);
            if (AssassinState.isEliminatedByHearts(after)) {
                AssassinState.setEliminated(victim, true);
            }
            GemPlayerState.applyMaxHearts(victim);
            return false;
        }

        int before = GemPlayerState.getMaxHearts(victim);
        GemPlayerState.setMaxHearts(victim, Math.max(GemPlayerState.minMaxHearts(), before - penalty));
        GemPlayerState.applyMaxHearts(victim);

        // Replace the usual death heart drop (1 heart) with our 2-heart penalty.
        GemOwnership.markSkipHeartDropOnce(victim);
        return true;
    }

    private static boolean activateAssassinTrade(ServerPlayerEntity player, ServerPlayerEntity victim) {
        int maxUses = GemsBalance.v().terror().terrorTradeMaxUses();
        int used = com.feel.gems.state.PlayerNbt.getInt(player, KEY_USES, 0);
        if (used >= maxUses) {
            player.sendMessage(Text.literal("Terror Trade has no uses remaining (" + maxUses + ")."), true);
            return false;
        }

        // Apply the permanent costs immediately so they persist through the death.
        applyCosts(player);
        com.feel.gems.state.PlayerNbt.putInt(player, KEY_USES, used + 1);

        // Attempt to kill the target (totems may save them).
        // Use a source attributed to the caster so kills count normally (energy, scoring, etc.);
        // Totems can still save the target.
        victim.damage(victim.getDamageSources().playerAttack(player), 10_000.0F);
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
            if (AssassinState.isEliminatedByHearts(after)) {
                AssassinState.setEliminated(player, true);
            }
        } else {
            int before = GemPlayerState.getMaxHearts(player);
            GemPlayerState.setMaxHearts(player, Math.max(GemPlayerState.minMaxHearts(), before - heartsCost));
        }
        GemPlayerState.applyMaxHearts(player);

        GemPlayerState.addEnergyCapPenalty(player, energyPenalty);
        GemPowers.sync(player);
        GemStateSync.send(player);
    }
}

