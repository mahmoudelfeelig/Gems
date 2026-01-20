package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.registry.PowerIds;
import com.feel.gems.power.runtime.GemPowers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityHungerResistMixin {
    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true)
    private float gems$hungerResist(float exhaustion) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) {
            return exhaustion;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_HUNGER_RESIST)) {
            return exhaustion;
        }
        float reduction = GemsBalance.v().bonusPool().hungerResistReductionPercent / 100.0f;
        return exhaustion * (1.0f - reduction);
    }
}
