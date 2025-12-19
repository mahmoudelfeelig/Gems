package com.blissmc.gems.mixin;

import com.blissmc.gems.item.ModItems;
import com.blissmc.gems.net.GemStateSync;
import com.blissmc.gems.power.GemPowers;
import com.blissmc.gems.state.GemPlayerState;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityDeathMixin {
    @Inject(method = "onDeath", at = @At("TAIL"))
    private void gems$onDeath(DamageSource source, CallbackInfo ci) {
        ServerPlayerEntity victim = (ServerPlayerEntity) (Object) this;

        GemPlayerState.initIfNeeded(victim);

        int victimEnergyBefore = GemPlayerState.getEnergy(victim);
        GemPlayerState.addEnergy(victim, -1);

        int victimHeartsBefore = GemPlayerState.getMaxHearts(victim);
        if (victimHeartsBefore > GemPlayerState.MIN_MAX_HEARTS) {
            GemPlayerState.setMaxHearts(victim, victimHeartsBefore - 1);
            victim.dropStack(new ItemStack(ModItems.HEART));
        }

        Entity attacker = source.getAttacker();
        if (attacker instanceof ServerPlayerEntity killer && killer != victim) {
            GemPlayerState.initIfNeeded(killer);
            int killerEnergyBefore = GemPlayerState.getEnergy(killer);
            GemPlayerState.addEnergy(killer, 1);
            GemPowers.sync(killer);
            GemStateSync.send(killer);

            if (killerEnergyBefore >= GemPlayerState.MAX_ENERGY && victimEnergyBefore > 0) {
                victim.dropStack(new ItemStack(ModItems.ENERGY_UPGRADE));
            }
        }
    }
}
