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
public abstract class PlayerEntityXpBoostMixin {
    @ModifyVariable(method = "addExperience", at = @At("HEAD"), argsOnly = true)
    private int gems$xpBoost(int amount) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity player)) {
            return amount;
        }
        if (!GemPowers.isPassiveActive(player, PowerIds.BONUS_XP_BOOST)) {
            return amount;
        }
        float boost = GemsBalance.v().bonusPool().xpBoostPercent / 100.0f;
        return Math.max(0, Math.round(amount * (1.0f + boost)));
    }
}
