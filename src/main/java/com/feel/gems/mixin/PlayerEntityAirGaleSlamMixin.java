package com.feel.gems.mixin;

import com.feel.gems.power.gem.air.AirGaleSlamRuntime;
import com.feel.gems.power.gem.air.AirMacePassive;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(PlayerEntity.class)
public abstract class PlayerEntityAirGaleSlamMixin {
    @Inject(method = "attack", at = @At("TAIL"))
    private void gems$airGaleSlam(Entity target, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) {
            return;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.AIR_WINDBURST_MACE)) {
            return;
        }
        if (!AirMacePassive.isHoldingMace(player)) {
            return;
        }
        if (!AirGaleSlamRuntime.consumeIfActive(player)) {
            return;
        }
        AirGaleSlamRuntime.trigger(player, target);
    }
}

