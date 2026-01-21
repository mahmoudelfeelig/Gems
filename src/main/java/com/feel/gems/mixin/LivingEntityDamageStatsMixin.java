package com.feel.gems.mixin;

import com.feel.gems.stats.GemsStats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageStatsMixin {
    @Unique
    private float gems$damageBefore;
    @Unique
    private ServerPlayerEntity gems$damageAttacker;

    @Inject(method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("HEAD"))
    private void gems$captureDamageSource(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        Entity attacker = source.getAttacker();
        if (attacker == null) {
            attacker = source.getSource();
        }
        if (attacker instanceof ServerPlayerEntity player && player != self && amount > 0.0F) {
            gems$damageAttacker = player;
            gems$damageBefore = self.getHealth();
        } else {
            gems$damageAttacker = null;
            gems$damageBefore = 0.0F;
        }
    }

    @Inject(method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("RETURN"))
    private void gems$recordDamage(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && gems$damageAttacker != null) {
            LivingEntity self = (LivingEntity) (Object) this;
            float delta = gems$damageBefore - self.getHealth();
            if (delta > 0.0F) {
                GemsStats.recordDamageDealt(gems$damageAttacker, delta);
            }
        }
        gems$damageAttacker = null;
        gems$damageBefore = 0.0F;
    }
}
