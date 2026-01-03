package com.feel.gems.event;

import com.feel.gems.assassin.AssassinState;
import com.feel.gems.assassin.AssassinTeams;
import com.feel.gems.item.GemItemGlint;
import com.feel.gems.item.GemKeepOnDeath;
import com.feel.gems.item.GemOwnership;
import com.feel.gems.item.ModItems;
import com.feel.gems.net.GemStateSync;
import com.feel.gems.power.gem.spy.SpyMimicSystem;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import com.feel.gems.power.gem.terror.TerrorBloodPrice;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.AbilityRuntime;
import com.feel.gems.power.runtime.GemPowers;
import com.feel.gems.state.GemPlayerState;
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
        SummonerSummons.discardAll(victim);
        GemKeepOnDeath.stash(victim);
    }

    public static void onDeathTail(ServerPlayerEntity victim, DamageSource source) {
        GemPlayerState.initIfNeeded(victim);
        AssassinState.initIfNeeded(victim);
        SpyMimicSystem.incrementDeaths(victim);
        boolean skipHeartDrop = GemOwnership.consumeSkipHeartDrop(victim);

        boolean victimWasAssassin = AssassinState.isAssassin(victim);
        int victimEnergyBefore = GemPlayerState.getEnergy(victim);
        GemPlayerState.addEnergy(victim, -1);

        int victimHeartsBefore = GemPlayerState.getMaxHearts(victim);
        int assassinTriggerHearts = Math.max(GemPlayerState.minMaxHearts(), com.feel.gems.config.GemsBalance.v().systems().assassinTriggerHearts());
        boolean victimAtAssassinTrigger = !victimWasAssassin && victimHeartsBefore <= assassinTriggerHearts;

        Entity attacker = source.getAttacker();
        if (attacker instanceof ServerPlayerEntity killer && killer != victim) {
            GemPlayerState.initIfNeeded(killer);
            AssassinState.initIfNeeded(killer);
            boolean killerWasAssassin = AssassinState.isAssassin(killer);
            int killerEnergyBefore = GemPlayerState.getEnergy(killer);
            GemPlayerState.addEnergy(killer, 1);
            GemPowers.sync(killer);
            GemItemGlint.sync(killer);
            GemStateSync.send(killer);

            boolean finalKill = victimAtAssassinTrigger;
            AssassinState.recordKill(killer, finalKill, victimWasAssassin);

            if (GemPowers.isPassiveActive(killer, PowerIds.TERROR_BLOOD_PRICE)) {
                TerrorBloodPrice.onPlayerKill(killer);
            }

            if (victimWasAssassin && killerWasAssassin) {
                var cfg = com.feel.gems.config.GemsBalance.v().systems();
                int loss = cfg.assassinVsAssassinVictimHeartsLoss();
                int gain = cfg.assassinVsAssassinKillerHeartsGain();
                int after = AssassinState.addAssassinHearts(victim, -loss);
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

        if (!victimWasAssassin) {
            if (victimAtAssassinTrigger) {
                AssassinState.becomeAssassin(victim);
            } else if (!skipHeartDrop && victimHeartsBefore > GemPlayerState.minMaxHearts()) {
                GemPlayerState.setMaxHearts(victim, victimHeartsBefore - 1);
                ItemStack heart = new ItemStack(ModItems.HEART);
                AbilityRuntime.setOwnerIfMissing(heart, victim.getUuid());
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
    }
}

