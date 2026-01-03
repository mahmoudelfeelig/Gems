package com.feel.gems.mixin;

import com.feel.gems.legendary.HypnoControl;
import com.feel.gems.power.gem.summoner.SummonerSummons;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;




@Mixin(LivingEntity.class)
public abstract class LivingEntitySummonFriendlyFireMixin {
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void gems$preventSummonFriendlyFire(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof MobEntity target)) {
            return;
        }
        Entity attackerEntity = source.getAttacker();
        if (!(attackerEntity instanceof MobEntity attacker)) {
            return;
        }

        UUID targetOwner = ownerFor(target);
        if (targetOwner == null) {
            return;
        }
        UUID attackerOwner = ownerFor(attacker);
        if (attackerOwner == null) {
            return;
        }
        if (!targetOwner.equals(attackerOwner)) {
            return;
        }
        cir.setReturnValue(false);
    }

    private static UUID ownerFor(MobEntity mob) {
        if (SummonerSummons.isSummon(mob)) {
            return SummonerSummons.ownerUuid(mob);
        }
        if (HypnoControl.isHypno(mob)) {
            return HypnoControl.ownerUuid(mob);
        }
        return null;
    }
}
