package com.feel.gems.mixin;

import com.feel.gems.config.GemsBalance;
import com.feel.gems.power.runtime.AbilityRuntime;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;




@Mixin(PlayerEntity.class)
public abstract class PlayerEntityChadStrengthMixin {
    @Inject(method = "attack", at = @At("TAIL"))
    private void gems$chadStrength(Entity target, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (!(self instanceof ServerPlayerEntity attacker)) {
            return;
        }
        if (!AbilityRuntime.isChadStrengthActive(attacker)) {
            return;
        }
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        int hit = AbilityRuntime.incrementChadHit(attacker);
        int every = Math.max(1, GemsBalance.v().strength().chadEveryHits());
        if (hit % every != 0) {
            return;
        }
        living.damage(attacker.getEntityWorld(), attacker.getDamageSources().playerAttack(attacker), GemsBalance.v().strength().chadBonusDamage());
    }
}
