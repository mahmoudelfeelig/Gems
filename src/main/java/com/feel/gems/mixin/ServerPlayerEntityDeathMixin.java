package com.feel.gems.mixin;

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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityDeathMixin {
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void gems$stashGems(DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity victim = (ServerPlayerEntity) (Object) this;
        if (victim.getWorld().isClient) {
            return;
        }
        // Summoner summons are soulbound: despawn them immediately on owner death.
        SummonerSummons.discardAll(victim);
        GemKeepOnDeath.stash(victim);
    }

    @Inject(method = "onDeath", at = @At("TAIL"))
    private void gems$onDeath(DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity victim = (ServerPlayerEntity) (Object) this;

        GemPlayerState.initIfNeeded(victim);
        AssassinState.initIfNeeded(victim);
        SpyMimicSystem.incrementDeaths(victim);
        boolean skipHeartDrop = GemOwnership.consumeSkipHeartDrop(victim);

        boolean victimWasAssassin = AssassinState.isAssassin(victim);
        int victimEnergyBefore = GemPlayerState.getEnergy(victim);
        GemPlayerState.addEnergy(victim, -1);

        int victimHeartsBefore = GemPlayerState.getMaxHearts(victim);
        boolean victimAtFiveHearts = !victimWasAssassin && victimHeartsBefore <= GemPlayerState.MIN_MAX_HEARTS;

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

            boolean finalKill = victimAtFiveHearts;
            AssassinState.recordKill(killer, finalKill, victimWasAssassin);

            if (GemPowers.isPassiveActive(killer, PowerIds.TERROR_BLOOD_PRICE)) {
                TerrorBloodPrice.onPlayerKill(killer);
            }

            if (victimWasAssassin && killerWasAssassin) {
                int after = AssassinState.addAssassinHearts(victim, -2);
                AssassinState.addAssassinHearts(killer, 2);
                if (after <= 0) {
                    AssassinState.setEliminated(victim, true);
                }
            }

            AssassinTeams.sync(victim.getServer(), killer);

            if (killerEnergyBefore >= GemPlayerState.MAX_ENERGY && victimEnergyBefore > 0) {
                victim.dropStack(new ItemStack(ModItems.ENERGY_UPGRADE));
            }
        }

        // Heart drop / conversion rules:
        if (!victimWasAssassin) {
            if (victimAtFiveHearts) {
                AssassinState.becomeAssassin(victim);
            } else if (!skipHeartDrop && victimHeartsBefore > GemPlayerState.MIN_MAX_HEARTS) {
                GemPlayerState.setMaxHearts(victim, victimHeartsBefore - 1);
                ItemStack heart = new ItemStack(ModItems.HEART);
                AbilityRuntime.setOwnerIfMissing(heart, victim.getUuid());
                victim.dropStack(heart);
            }
        }

        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponentTypes.PROFILE, new ProfileComponent(victim.getGameProfile()));
        victim.dropStack(head);

        GemPlayerState.applyMaxHearts(victim);
        GemPowers.sync(victim);
        GemItemGlint.sync(victim);
        GemStateSync.send(victim);
        AssassinTeams.sync(victim.getServer(), victim);
    }

}
