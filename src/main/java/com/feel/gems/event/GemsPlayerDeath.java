package com.feel.gems.event;

import com.feel.gems.assassin.AssassinState;
import com.feel.gems.bounty.BountyBoard;
import com.feel.gems.assassin.AssassinTeams;
import com.feel.gems.core.GemId;
import com.feel.gems.item.GemItemGlint;
import com.feel.gems.item.GemKeepOnDeath;
import com.feel.gems.item.GemOwnership;
import com.feel.gems.item.ModItems;
import com.feel.gems.item.legendary.ExperienceBladeItem;
import com.feel.gems.item.legendary.HuntersTrophyNecklaceItem;
import com.feel.gems.legendary.LegendaryDuels;
import com.feel.gems.mastery.LeaderboardTracker;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.power.gem.hunter.HunterTrophyHunterRuntime;
import com.feel.gems.power.gem.spy.SpySystem;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.power.gem.terror.TerrorBloodPrice;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.ability.sentinel.SentinelInterventionRuntime;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
import com.feel.gems.stats.GemsStats;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public final class GemsPlayerDeath {
    private GemsPlayerDeath() {
    }

    public static void onDeathHead(ServerPlayerEntity victim, @Nullable DamageSource source) {
        if (victim.getEntityWorld().isClient()) {
            return;
        }
        ExperienceBladeItem.clearOnDeath(victim);
        SummonerSummons.discardAll(victim);
        GemKeepOnDeath.stash(victim);
    }

    public static void onDeathTail(ServerPlayerEntity victim, DamageSource source) {
        GemPlayerState.initIfNeeded(victim);
        AssassinState.initIfNeeded(victim);
        SpySystem.incrementDeaths(victim);
        com.feel.gems.power.bonus.BonusPassiveRuntime.resetSecondWind(victim);
        boolean skipHeartDrop = GemOwnership.consumeSkipHeartDrop(victim);
        GemId victimActiveGem = GemPlayerState.getActiveGem(victim);

        boolean victimWasAssassin = AssassinState.isAssassin(victim);
        int victimEnergyBefore = GemPlayerState.getEnergy(victim);
        GemPlayerState.addEnergy(victim, -1);

        int victimHeartsBefore = GemPlayerState.getMaxHearts(victim);
        int assassinTriggerHearts = Math.max(GemPlayerState.minMaxHearts(), com.feel.gems.config.GemsBalance.v().systems().assassinTriggerHearts());
        boolean victimAtAssassinTrigger = !victimWasAssassin && victimHeartsBefore <= assassinTriggerHearts;

        ServerPlayerEntity killer = resolveKiller(victim, source);
        if (killer != null && killer != victim) {
            GemPlayerState.initIfNeeded(killer);
            AssassinState.initIfNeeded(killer);
            LeaderboardTracker.incrementKills(killer);
            boolean killerWasAssassin = AssassinState.isAssassin(killer);
            int killerEnergyBefore = GemPlayerState.getEnergy(killer);
            if (victimEnergyBefore > 0) {
                GemPlayerState.addEnergy(killer, 1);
            }
            GemPowers.sync(killer);
            GemItemGlint.sync(killer);
            GemStateSync.send(killer);

            boolean finalKill = victimAtAssassinTrigger;
            AssassinState.recordKill(killer, finalKill, victimWasAssassin);

            GemsStats.recordPlayerKill(killer, victim, killerWasAssassin, victimWasAssassin, finalKill);
            BountyBoard.handleKill(victim, killer, 0, 0);

            if (GemPowers.isPassiveActive(killer, PowerIds.TERROR_BLOOD_PRICE)) {
                TerrorBloodPrice.onPlayerKill(killer);
            }

            // Hunter Trophy Hunter passive - gain a random passive from the victim
            if (GemPowers.isPassiveActive(killer, PowerIds.HUNTER_TROPHY_HUNTER)) {
                HunterTrophyHunterRuntime.onPlayerKill(killer, victim);
            }

            // Hunter's Trophy Necklace legendary - permanently gain a random passive
            HuntersTrophyNecklaceItem.onKillPlayer(killer, victim);

            // Using another player's active gem should never let you keep re-killing them.
            GemOwnership.removeOwnedGemFromInventory(killer, victim.getUuid(), victimActiveGem);

            if (victimWasAssassin && killerWasAssassin) {
                var cfg = com.feel.gems.config.GemsBalance.v().systems();
                int loss = cfg.assassinVsAssassinVictimHeartsLoss();
                int gain = cfg.assassinVsAssassinKillerHeartsGain();

                // Assassin-vs-Assassin: killer takes the victim's accumulated assassin points.
                AssassinState.transferAssassinPoints(victim, killer);
                if (AssassinState.maybeUnlockChoice(killer)) {
                    AssassinState.sendChoicePrompt(killer);
                }

                int after = AssassinState.addAssassinHearts(victim, -loss);
                int minHearts = Math.min(5, AssassinState.maxHearts());
                if (after < minHearts) {
                    after = AssassinState.setAssassinHearts(victim, minHearts);
                }
                if (gain > 0) {
                    AssassinState.addAssassinHearts(killer, gain);
                }
                if (AssassinState.isEliminatedByHearts(after)) {
                    AssassinState.setEliminated(victim, true);
                }
            }

            AssassinTeams.sync(victim.getEntityWorld().getServer(), killer);

            if (killerEnergyBefore >= GemPlayerState.MAX_ENERGY && victimEnergyBefore > 0) {
                victim.dropStack(victim.getEntityWorld(), new ItemStack(ModItems.ENERGY_UPGRADE));
            }
        }

        GemsStats.recordPlayerDeath(victim, killer, victimWasAssassin, victimAtAssassinTrigger);

        if (!victimWasAssassin) {
            if (victimAtAssassinTrigger) {
                AssassinState.becomeAssassin(victim);
            } else if (!skipHeartDrop && victimHeartsBefore > GemPlayerState.minMaxHearts()) {
                GemPlayerState.setMaxHearts(victim, victimHeartsBefore - 1);
                ItemStack heart = new ItemStack(ModItems.HEART);
                AbilityRuntime.setOwnerWithName(heart, victim.getUuid(), victim.getName().getString());
                victim.dropStack(victim.getEntityWorld(), heart);
            }
        }

        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponentTypes.PROFILE, ProfileComponent.ofStatic(victim.getGameProfile()));
        victim.dropStack(victim.getEntityWorld(), head);

        GemPlayerState.applyMaxHearts(victim);
        GemPowers.sync(victim);
        GemItemGlint.sync(victim);
        GemStateSync.send(victim);
        AssassinTeams.sync(victim.getEntityWorld().getServer(), victim);

        // Challenger's Gauntlet duels: return participants, transfer drops, and clean up arenas.
        LegendaryDuels.onDuelParticipantDeathTail(victim, source);

        // Mirror Match: clear duel state without teleporting the loser back
        com.feel.gems.power.ability.duelist.DuelistMirrorMatchRuntime.onDeath(victim);

        // Sentinel intervention should not persist through death.
        SentinelInterventionRuntime.cleanup(victim.getEntityWorld().getServer(), victim);
    }

    @Nullable
    private static ServerPlayerEntity resolveKiller(ServerPlayerEntity victim, @Nullable DamageSource source) {
        if (source == null) {
            return null;
        }
        Entity attacker = source.getAttacker();
        if (attacker instanceof ServerPlayerEntity player) {
            return player;
        }
        Entity direct = source.getSource();
        if (direct instanceof ServerPlayerEntity player) {
            return player;
        }
        if (direct instanceof ProjectileEntity projectile && projectile.getOwner() instanceof ServerPlayerEntity player) {
            return player;
        }
        Entity last = victim.getAttacker();
        if (last instanceof ServerPlayerEntity player) {
            return player;
        }
        return null;
    }
}
